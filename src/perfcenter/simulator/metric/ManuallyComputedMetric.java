/*
 * Copyright (C) 2011-12  by Varsha Apte - <varsha@cse.iitb.ac.in>, et al.
 * This file is distributed as part of PerfCenter
 *
 *  This program is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License as published by
 *   the Free Software Foundation, either version 3 of the License, or
 *   (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License
 *   along with this program.  If not, see <http://www.gnu.org/licenses/>. 
 */
package perfcenter.simulator.metric;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import perfcenter.baseclass.ModelParameters;
import perfcenter.simulator.SimulationParameters;

/**
 * This class is used for those metrics, which are computed from other performance counters.
 * <p>
 * E.g. Throughput is computed from the totaltime and total number of requests served, which are tracked independently throughout the simulation.
 * Hence there is no need to track progressive throughput. We just feed the computed value of throughput to this class, for ConfInt purposes.
 * <p>
 * There can be no recordValue() for this set of classes, just the recordCISample() type of methods are possible.
 * <p>
 * Refer DiscreteSampleAverageMetric class's method comments for more information.
 * 
 * @author bhavin
 * 
 */

public class ManuallyComputedMetric extends MetricSim {
	_ManuallyComputedMetricSingleSlot[] metricSlots = new _ManuallyComputedMetricSingleSlot[ModelParameters.intervalSlotCount];

	public ManuallyComputedMetric(double probability) {
		for (int i = 0; i < metricSlots.length; i++) {
			metricSlots[i] = new _ManuallyComputedMetricSingleSlot(probability);
		}

	}

	public void recordCISample(int slot, double sampleValue) {
		metricSlots[slot].recordCISample(null, sampleValue);
	}

	public void recordCISample(int slot, String serverName, double sampleValue) {
		metricSlots[slot].recordCISample(serverName, sampleValue);
	}

	public double getMean(int slot) {
		return metricSlots[slot].getMean();
	}

	public double getMean(int slot, String serverName) {
		return metricSlots[slot].getMean(serverName);
	}

	public double getValue(int slot, String serverName, int replicationNumber) {
		return metricSlots[slot].getValue(replicationNumber);
	}

	public double getValue(int slot, int replicationNumber) {
		return metricSlots[slot].getValue(replicationNumber);
	}

	public double getProbability(int slot) {
		return metricSlots[slot].getProbability();
	}

	public double getProbability(int slot, String serverName) {
		return metricSlots[slot].getProbability(serverName);
	}

	public double getCI(int slot) {
		return metricSlots[slot].getCI();
	}

	public double getCI(int slot, String serverName) {
		return metricSlots[slot].getCI(serverName);
	}

	public Set<String> getServerList(int slot) {
		return metricSlots[slot].getServerList();
	}

	public void clearValuesButKeepConfIvals() {
		for (int i = 0; i < metricSlots.length; i++) {
			metricSlots[i].clearValuesButKeepConfInts();

		}
	}

	public double getMean() {
		return metricSlots[SimulationParameters.getIntervalSlotCounter()].getMean();
	}

	public final void clearEverything() {
		for (int i = 0; i < metricSlots.length; i++) {
			metricSlots[i].clearEverything();

		}
	}

	public final void computeConfIvalsAtEndOfRepl() {
		for (int i = 0; i < metricSlots.length; i++) {
			metricSlots[i].calculateConfidenceIntervalsAtTheEndOfReplications();

		}
	}

}

/**
 * This class contains the server level bifurcation of values.
 * <p>
 * _total is the dummy server used to verify the totals across the servers. _passthrough is the dummy server used to store values for samples not
 * coming from any request context.
 * @author nadeesh
 * 
 */
class _ManuallyComputedMetricSingleSlot extends _MetricSimSlotLevel {
	Map<String, _ManuallyComputedMetricLowestLevel> perServerMetric = new HashMap<String, _ManuallyComputedMetricLowestLevel>();
	double probability;

	public _ManuallyComputedMetricSingleSlot(double probability) {
		this.probability = probability;
		_ManuallyComputedMetricLowestLevel manuallyComputedMetricSlot = new _ManuallyComputedMetricLowestLevel(probability);
		manuallyComputedMetricSlot.recordCISample(0); // _total used to keep totalValue
		perServerMetric.put("_total", manuallyComputedMetricSlot);
	}

	public void recordCISample(String serverName, double sampleValue) {
		if (serverName == null) {
			serverName = "_passthrough";
		}

		if (perServerMetric.containsKey(serverName)) {
			perServerMetric.get(serverName).recordCISample(sampleValue);
		} else {
			_ManuallyComputedMetricLowestLevel manualMetrSlot = new _ManuallyComputedMetricLowestLevel(probability);
			manualMetrSlot.recordCISample(sampleValue);
			perServerMetric.put(serverName, manualMetrSlot);
		}
		// store the total value by getting old and add it with that
		perServerMetric.get("_total").recordCISample(perServerMetric.get("_total").getValue(SimulationParameters.replicationNo) + sampleValue);
	}

	public double getMean(String serverName) {
		return perServerMetric.get(serverName) != null ? perServerMetric.get(serverName).getMean() : 0;
	}

	public double getMean() {
		return perServerMetric.get("_total") != null ? perServerMetric.get("_total").getMean() : 0;
	}

	public double getValue(String serverName, int replicationNumber) {
		return perServerMetric.get(serverName) != null ? perServerMetric.get(serverName).getValue(replicationNumber) : 0;
	}

	public double getValue(int replicationNumber) {
		return perServerMetric.get("_total") != null ? perServerMetric.get("_total").getValue(replicationNumber) : 0;
	}

	public double getProbability(String serverName) {
		return perServerMetric.get(serverName) != null ? perServerMetric.get(serverName).getProbability() : 0;
	}

	public double getProbability() {
		return perServerMetric.get("_total") != null ? perServerMetric.get("_total").getProbability() : 0;
	}

	public double getCI(String serverName) {
		return perServerMetric.get(serverName) != null ? perServerMetric.get(serverName).getCI() : 0;
	}

	public double getCI() {
		return perServerMetric.get("_total") != null ? perServerMetric.get("_total").getCI() : 0;
	}

	public Set<String> getServerList() {
		Set<String> serverList = perServerMetric.keySet();
		return serverList;
	}

	public final void clearEverything() {
		for (_ManuallyComputedMetricLowestLevel manCompMetric : perServerMetric.values()) {
			manCompMetric.clearEverything();
		}
	}

	public void clearValuesButKeepConfInts() {
		for (_ManuallyComputedMetricLowestLevel manCompMetric : perServerMetric.values()) {
			manCompMetric.clearValuesButKeepConfInts();
		}
	}

	public final void calculateConfidenceIntervalsAtTheEndOfReplications() {
		for (_ManuallyComputedMetricLowestLevel manCompMetric : perServerMetric.values()) {
			manCompMetric.calculateConfidenceIntervalsAtTheEndOfReplications();
		}
	}

}

/**
 * The low level class that manages samples for a given slot, and given server.
 * @author nadeesh
 * 
 */

class _ManuallyComputedMetricLowestLevel extends _MetricSimLowestLevel {

	protected _ManuallyComputedMetricLowestLevel(double probability) {
		super(probability);
	}

	public double getValue(int replicationNumber) {
		return confInterval.getDataPoint(replicationNumber);
	}

	public void recordCISample(double sampleValue) {
		if (SimulationParameters.warmupEnabled) {
			return;
		}
		confInterval.recordCISample(sampleValue);
	}

	public double getMean() {
		return confInterval.getMean();
	}

	public double getCI(String serverName) {
		return confInterval.getCI();
	}

	public void clearValuesButKeepConfInts() {
		confInterval.clearValuesButKeepConfInts();

	}

	@Override
	public void recordCISample() {
		throw new Error(
				"ManuallyComputedMetric::recordCISample() should never be called (the method with single argument should be called, not the no-arg version.");
	}

}
