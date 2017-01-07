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
import perfcenter.simulator.request.Request;

/**
 * This class of hierarchy is used to manage values of a metric which is averaged in a discrete way.
 * <p>
 * For example, response time values are recorded once per request completion, and thats discrete, not continuous. On the other hand, utilization is
 * recorded continuously and then averaged.
 * <p>
 * The top level class DiscreteSampleAverageMetric has the information at the level of the cyclic workload slots. The next level
 * _DiscreteSampleAverageMetricSingleSlot manages the server level bifurcation of the data for the given slot. The lowest level
 * _DiscreteSampleAverageMetricLowestLevel manages the discrete data for the given slot and given server.
 * 
 * @author bhavin
 * 
 */
public class DiscreteSampleAverageMetric extends MetricSim {
	_DiscreteSampleAverageMetricSingleSlot metricSlots[] = new _DiscreteSampleAverageMetricSingleSlot[ModelParameters.intervalSlotCount];

	public DiscreteSampleAverageMetric(double probability) {
		for (int i = 0; i < metricSlots.length; i++) {
			metricSlots[i] = new _DiscreteSampleAverageMetricSingleSlot(probability);
		}

	}

	/**
	 * Records a single sample of the metric. This is used during the simulation, when samples are being produced.
	 * <p>
	 * This flavour of method is used when there is no Request context in the simulation.
	 * 
	 * @param sampleValue
	 *            The sample produced by the simulation
	 */
	public void recordValue(double sampleValue) {
		metricSlots[SimulationParameters.getIntervalSlotCounter()].recordValue(null, sampleValue);
	}

	/**
	 * Records a single sample of the metric. This is used during the simulation, when samples are being produced.
	 * <p>
	 * This flavour of method is used when there is a Request context available. Request would decide the classification of the data sample being
	 * recorded inside the lower levels of the objects.
	 * 
	 * @param req
	 *            Request object that defines the context of the value
	 * @param sampleValue
	 *            The sample produced by the simulation
	 */
	public void recordValue(Request req, double sampleValue) {
		// assumption is that request's fromServer is getting set properly
		metricSlots[SimulationParameters.getIntervalSlotCounter()].recordValue(req, sampleValue);
	}

	/**
	 * This method records a confidence interval sample of the collected values till now.
	 * <p>
	 * This confidence interval sample is calculated by appropriately calculating the average of all the collected values till now.
	 */
	public void recordCISample() {
		metricSlots[SimulationParameters.getIntervalSlotCounter()].recordCISample();
	}

	/**
	 * This method records a confidence interval sample of the collected values till now only for the given slot.
	 * <p>
	 * This confidence interval sample is calculated by appropriately calculating the average of all the collected values till now.
	 */
	public void recordCISample(int slot) {
		metricSlots[slot].recordCISample();
	}

	public double getTotalValue() {
		return metricSlots[SimulationParameters.getIntervalSlotCounter()].getTotalValue();
	}

	public double getTotalValue(int slot) {
		return metricSlots[slot].getTotalValue();
	}

	public double getTotalValue(int slot, String serverName) {
		return metricSlots[slot].getTotalValue(serverName);
	}

	public long getTotalSamples() {
		return metricSlots[SimulationParameters.getIntervalSlotCounter()].getTotalSamples();
	}

	public long getTotalSamples(int slot) {
		return metricSlots[slot].getTotalSamples();
	}

	public long getTotalSamples(int slot, String serverName) {
		return metricSlots[slot].getTotalSamples(serverName);
	}

	public double getMean() {
		return metricSlots[SimulationParameters.getIntervalSlotCounter()].getMean();
	}

	@Override
	public double getMean(int slot) {
		return metricSlots[slot].getMean();
	}

	@Override
	public double getMean(int slot, String serverName) {
		return metricSlots[slot].getMean(serverName);
	}

	@Override
	public double getProbability(int slot) {
		return metricSlots[slot].getProbability();
	}

	public double getProbability(int slot, String serverName) {
		return metricSlots[slot].getProbability(serverName);
	}

	@Override
	public double getCI(int slot) {
		return metricSlots[slot].getCI();
	}

	@Override
	public double getCI(int slot, String serverName) {
		return metricSlots[slot].getCI(serverName);
	}

	/**
	 * this method retrieve all the serverNames of a Host Used in DitributedSystemSim.java to get server level value of each metric
	 */
	@Override
	public Set<String> getServerList(int slot) {
		return metricSlots[slot].getServerList();
	}

	/**
	 * Clears all the values, all the book keeping variables, and just retains the CI samples.
	 * <p>
	 * This is done once at the end of simulation run. This will be done equal to the
	 * {@linkplain perfcenter.baseclass.ModelParameters#getNumberOfReplications() number of replications}.
	 */
	public void clearValuesButKeepConfInts() {
		for (int i = 0; i < metricSlots.length; i++) {
			metricSlots[i].clearValuesButKeepConfInts();
		}
	}

	/**
	 * Calculates final value of the metric, along with confidence interval, at the end of all the replication runs.
	 * <p>
	 * This method is called just once.
	 */
	@Override
	public final void computeConfIvalsAtEndOfRepl() {
		for (int i = 0; i < metricSlots.length; i++) {
			metricSlots[i].calculateConfidenceIntervalsAtTheEndOfReplications();
		}
	}

	/**
	 * Clears everything, including the confidence interval values.
	 */
	public final void clearEverything() {
		for (int i = 0; i < metricSlots.length; i++) {
			metricSlots[i].clearEverything();

		}
	}

}

/**
 * This class contains the server level bifurcation of values.
 * <p>
 * _total is the dummy server used to verify the totals across the servers. _passthrough is the dummy server used to store values for samples not
 * coming from any request context.
 * 
 * @author nadeesh
 */
class _DiscreteSampleAverageMetricSingleSlot extends _MetricSimSlotLevel {
	Map<String, _DiscreteSampleAverageMetricLowestLevel> perServerMetric = new HashMap<String, _DiscreteSampleAverageMetricLowestLevel>();
	double probability;

	public _DiscreteSampleAverageMetricSingleSlot(double probability) {
		this.probability = probability;
		_DiscreteSampleAverageMetricLowestLevel discAvgMetrSlot = new _DiscreteSampleAverageMetricLowestLevel(probability);
		discAvgMetrSlot.recordValue(0);
		perServerMetric.put("_total", discAvgMetrSlot);
	}

	public void recordValue(Request req, double sampleValue) {
		String serverName = "_passthrough";
		if (req != null) {
			serverName = req.fromServer;
		}

		if (perServerMetric.containsKey(serverName)) {
			perServerMetric.get(serverName).recordValue(sampleValue);
		} else {
			_DiscreteSampleAverageMetricLowestLevel discAvgMetrSlot = new _DiscreteSampleAverageMetricLowestLevel(probability);
			discAvgMetrSlot.recordValue(sampleValue);
			perServerMetric.put(serverName, discAvgMetrSlot);
		}
		perServerMetric.get("_total").recordValue(sampleValue); // used to keepTotalValue
	}

	public void recordCISample() {
		for (_DiscreteSampleAverageMetricLowestLevel discAvMetr : perServerMetric.values()) {
			discAvMetr.recordCISample();
		}
	}

	public double getTotalValue(String serverName) {
		return perServerMetric.get(serverName) != null ? perServerMetric.get(serverName).getTotalValue() : 0;
	}

	public double getTotalValue() {
		return perServerMetric.get("_total") != null ? perServerMetric.get("_total").getTotalValue() : 0;
	}

	public long getTotalSamples(String serverName) {
		return perServerMetric.get(serverName) != null ? perServerMetric.get(serverName).getTotalSamples() : 0;
	}

	public long getTotalSamples() {
		return perServerMetric.get("_total") != null ? perServerMetric.get("_total").getTotalSamples() : 0;
	}

	@Override
	public double getMean(String serverName) {
		return perServerMetric.get(serverName) != null ? perServerMetric.get(serverName).getMean() : 0;
	}

	public double getMean() {
		return perServerMetric.get("_total") != null ? perServerMetric.get("_total").getMean() : 0;
	}

	public double getProbability(String serverName) {
		return perServerMetric.get(serverName) != null ? perServerMetric.get(serverName).getProbability() : 0;
	}

	public double getProbability() {
		return perServerMetric.get("_total") != null ? perServerMetric.get("_total").getProbability() : 0;
	}

	@Override
	public double getCI(String serverName) {
		return perServerMetric.get(serverName) != null ? perServerMetric.get(serverName).getCI() : 0;
	}

	public double getCI() {
		return perServerMetric.get("_total") != null ? perServerMetric.get("_total").getCI() : 0;

	}

	@Override
	public Set<String> getServerList() {
		Set<String> serverList = perServerMetric.keySet();
		return serverList;
	}

	public void clearValuesButKeepConfInts() {
		for (_DiscreteSampleAverageMetricLowestLevel discAvmetr : perServerMetric.values()) {
			discAvmetr.clearValuesButKeepConfInts();
		}
	}

	public final void calculateConfidenceIntervalsAtTheEndOfReplications() {
		for (_DiscreteSampleAverageMetricLowestLevel discAvmetr : perServerMetric.values()) {
			discAvmetr.calculateConfidenceIntervalsAtTheEndOfReplications();
		}
	}

	public final void clearEverything() {
		for (_DiscreteSampleAverageMetricLowestLevel discAvmetr : perServerMetric.values()) {
			discAvmetr.clearEverything();
		}
	}

}

/**
 * The low level class that manages samples for a given slot, and given server.
 * 
 * @author bhavin
 *
 */
class _DiscreteSampleAverageMetricLowestLevel extends _MetricSimLowestLevel {

	double totalValue = 0;
	long totalSamples = 0;

	protected _DiscreteSampleAverageMetricLowestLevel(double probability) {
		super(probability);
	}

	public void recordValue(double sampleValue) {
		if (SimulationParameters.warmupEnabled) {
			return;
		}

		totalValue += sampleValue;
		totalSamples++;
	}

	@Override
	public void recordCISample() {
		if (SimulationParameters.warmupEnabled) {
			return;
		}

		double sample = 0;
		if ((totalSamples - confInterval.totalSamplesPrev) == 0) {
			sample = 0;
		} else {
			sample = (totalValue - confInterval.totalValuePrev) / (totalSamples - confInterval.totalSamplesPrev);
		}
		assert sample >= 0 : totalValue + " " + confInterval.totalValuePrev + " " + totalSamples + " " + confInterval.totalSamplesPrev;
		confInterval.recordCISample(sample);
		confInterval.totalSamplesPrev = totalSamples;
		confInterval.totalValuePrev = totalValue;
	}

	@Override
	public void clearValuesButKeepConfInts() {
		totalValue = 0;
		totalSamples = 0;
		confInterval.clearValuesButKeepConfInts();
	}

	public double getTotalValue() {
		return totalValue;
	}

	public long getTotalSamples() {
		return totalSamples;
	}

	@Override
	public double getMean() {
		try {
			return confInterval.getMean();
		} catch (IndexOutOfBoundsException e) {
			return totalValue / totalSamples;
		}
	}
}
