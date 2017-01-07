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
 * This class of hierarchy is used to manage values of a metric which is averaged in a continuous way.
 * <p>
 * For example, response time values are recorded once per request completion, and thats discrete, not continuous.
 * On the other hand, utilization is recorded continuously and then averaged.
 * <p>
 * The top level class TimeAverageMetric has the information at the level of the cyclic workload slots. The next level
 * _TimeAverageMetricSingleSlot manages the server level bifurcation of the data for the given slot. The lowest level
 * _TimeAverageMetricLowestLevel manages the discrete data for the given slot and given server.
 * 
 * @author bhavin
 * 
 */

public class TimeAverageMetric extends MetricSim {
	_TimeAverageMetricSlotLevel[] metricSlots = new _TimeAverageMetricSlotLevel[ModelParameters.intervalSlotCount];

	public TimeAverageMetric(double probability) {
		for (int i = 0; i < metricSlots.length; i++) {
			metricSlots[i] = new _TimeAverageMetricSlotLevel(probability);
		}
	}

	public void recordValue(double sampleValue) {
		// assumption is that request's fromServer is getting set properly
		metricSlots[SimulationParameters.getIntervalSlotCounter()].recordValue(null, sampleValue);
	}

	public void recordValue(Request req, double sampleValue) {
		// assumption is that request's fromServer is getting set properly
		metricSlots[SimulationParameters.getIntervalSlotCounter()].recordValue(req, sampleValue);
	}

	public void recordValue(Request req, double sampleValue, double timeElapsedForCurrentSample) {
		// assumption is that request's fromServer is getting set properly
		metricSlots[SimulationParameters.getIntervalSlotCounter()].recordValue(req, sampleValue, timeElapsedForCurrentSample);
	}

	public void recordCISample(int slot) {
		metricSlots[slot].recordCISample();
	}

	public double getTotalValue(int slot) {
		return metricSlots[slot].getTotalValue();
	}

	public double getTotalValue(int slot, String serverName) {
		return metricSlots[slot].getTotalValue(serverName);
	}

	public double getTotalDuration(int slot) {
		return metricSlots[slot].getTotalDuration();
	}

	public double getTotalDuration(int slot, String serverName) {
		return metricSlots[slot].getTotalDuration(serverName);
	}

	public double getCI(int slot) {
		return metricSlots[slot].getCI();
	}

	public double getCI(int slot, String serverName) {
		return metricSlots[slot].getCI(serverName);
	}

	public boolean isValuesCapturedSinceLastCI(int slot) {
		return metricSlots[slot].isValuesCapturedSinceLastCI();
	}

	public boolean isValuesCapturedSinceLastCI(int slot, String serverName) {
		return metricSlots[slot].isValuesCapturedSinceLastCI(serverName);
	}

	public double getMean(int slot) {
		return metricSlots[slot].getMean();
	}

	public double getMean(int slot, String serverName) {
		return metricSlots[slot].getMean(serverName);
	}

	public double getProbability(int slot) {
		return metricSlots[slot].getProbability();
	}

	public double getProbability(int slot, String serverName) {
		return metricSlots[slot].getProbability(serverName);
	}

	public Set<String> getServerList(int slot) {
		return metricSlots[slot].getServerList();
	}

	public void clearValuesButKeepConfInts() {
		for (int i = 0; i < metricSlots.length; i++) {
			metricSlots[i].clearValuesButKeepConfInts();
		}
	}

	public final void computeConfIvalsAtEndOfRepl() {
		for (int i = 0; i < metricSlots.length; i++) {
			metricSlots[i].calculateConfidenceIntervalsAtTheEndOfReplications();
		}
	}

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
 * @author nadeesh 
 */
class _TimeAverageMetricSlotLevel extends _MetricSimSlotLevel {
	Map<String, _TimeAverageMetricLowestLevel> perServerMetric = new HashMap<String, _TimeAverageMetricLowestLevel>();
	double probability;

	public _TimeAverageMetricSlotLevel(double probability) {
		this.probability = probability;
		_TimeAverageMetricLowestLevel timeAvgMetrSlot = new _TimeAverageMetricLowestLevel(probability);
		timeAvgMetrSlot.recordValue(0);
		perServerMetric.put("_total", timeAvgMetrSlot);
	}

	public void recordValue(Request req, double sampleValue) {
		String serverName = "_passthrough";
		if (req != null) {
			serverName = req.fromServer;
		}

		if (perServerMetric.containsKey(serverName)) {
			perServerMetric.get(serverName).recordValue(sampleValue);
		} else {
			_TimeAverageMetricLowestLevel timeAvgMetrSlot = new _TimeAverageMetricLowestLevel(probability);
			timeAvgMetrSlot.recordValue(sampleValue);
			perServerMetric.put(serverName, timeAvgMetrSlot);
		}
		perServerMetric.get("_total").recordValue(sampleValue);
	}

	public void recordValue(Request req, double sampleValue, double timeElapsedForCurrentSample) {
		String serverName = "_passthrough";
		if (req != null) {
			serverName = req.fromServer;
		}
		if (perServerMetric.containsKey(serverName)) {
			perServerMetric.get(serverName).recordValue(sampleValue, timeElapsedForCurrentSample);
		} else {
			_TimeAverageMetricLowestLevel timeAvgMetrSlot = new _TimeAverageMetricLowestLevel(probability);
			timeAvgMetrSlot.recordValue(sampleValue);
			perServerMetric.put(serverName, timeAvgMetrSlot);
		}
		perServerMetric.get("_total").recordValue(sampleValue);

	}

	public void recordCISample() {
		for (_TimeAverageMetricLowestLevel timeAvMetr : perServerMetric.values()) {
			timeAvMetr.recordCISample();
		}
	}

	public double getTotalValue(String serverName) {
		return perServerMetric.get(serverName) != null ? perServerMetric.get(serverName).getTotalValue() : 0;
	}

	public double getTotalValue() {
		return perServerMetric.get("_total") != null ? perServerMetric.get("_total").getTotalValue() : 0;
	}

	public double getTotalDuration(String serverName) {
		return perServerMetric.get(serverName) != null ? perServerMetric.get(serverName).getTotalDuration() : 0;
	}

	public double getTotalDuration() {
		return perServerMetric.get("_total") != null ? perServerMetric.get("_total").getTotalDuration() : 0;
	}

	public double getMean(String serverName) {
		return perServerMetric.get(serverName) != null ? perServerMetric.get(serverName).getMean() : 0;
	}

	public double getMean() {
		return perServerMetric.get("_total") != null ? perServerMetric.get("_total").getMean() : 0;
	}
	
	public boolean isValuesCapturedSinceLastCI(String serverName) {
		return perServerMetric.get(serverName).isValuesCapturedSinceLastCI();
	}

	public boolean isValuesCapturedSinceLastCI() {
		boolean isValCapt = true;
		for (_TimeAverageMetricLowestLevel timeAvMetr : perServerMetric.values())
			isValCapt = isValCapt && timeAvMetr.isValuesCapturedSinceLastCI();
		return (perServerMetric.size() == 0 ? false : isValCapt);
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

	public void clearValuesButKeepConfInts() {
		for (_TimeAverageMetricLowestLevel tam : perServerMetric.values()) {
			tam.clearValuesButKeepConfInts();
		}
	}

	public final void calculateConfidenceIntervalsAtTheEndOfReplications() {
		for (_TimeAverageMetricLowestLevel tam : perServerMetric.values()) {
			tam.calculateConfidenceIntervalsAtTheEndOfReplications();
		}
	}

	public final void clearEverything() {
		for (_TimeAverageMetricLowestLevel tam : perServerMetric.values()) {
			tam.clearEverything();
		}
	}

}

/**
 * The low level class that manages samples for a given slot, and given server.
 * @author nadeesh 
 */

class _TimeAverageMetricLowestLevel extends _MetricSimLowestLevel {
	private double totalValue = 0;
	private double totalDuration = 0;
	private boolean valuesCapturedSinceLastCI = false;
	
	private double timeOfLastSampleRecording = 0;

	public _TimeAverageMetricLowestLevel(double probability) {
		super(probability);
	}

	public void recordValue(double sampleValue) {
		if(SimulationParameters.warmupEnabled) {
			return;
		}
		
		recordValue(sampleValue, SimulationParameters.currTime - timeOfLastSampleRecording);
		timeOfLastSampleRecording = SimulationParameters.currTime;
	}

	public void recordValue(double sampleValue, double timeElapsedForCurrentSample) {
		if(SimulationParameters.warmupEnabled) {
			return;
		}
		if (!Double.isNaN(sampleValue) && !Double.isNaN(timeElapsedForCurrentSample)) {
			totalValue += sampleValue * timeElapsedForCurrentSample;
		}
		if (!Double.isNaN(timeElapsedForCurrentSample)) {
			totalDuration += timeElapsedForCurrentSample;
		}
		valuesCapturedSinceLastCI = true;
	}

	public void recordCISample() {
		if(SimulationParameters.warmupEnabled) {
			return;
		}
		
		double sample = 0;
		if (totalDuration - confInterval.totalDurationPrev == 0) {
			assert false;
			sample = 0;
		} else {
			sample = (totalValue - confInterval.totalValuePrev) / (totalDuration - confInterval.totalDurationPrev);
		}
		assert sample >= 0 : sample + " " + totalValue + " " + confInterval.totalValuePrev + " " + totalDuration + " " + confInterval.totalDurationPrev;
		confInterval.recordCISample(sample);
		confInterval.totalDurationPrev = totalDuration;
		confInterval.totalValuePrev = totalValue;
		valuesCapturedSinceLastCI = false;
	}

	public void clearValuesButKeepConfInts() {
		totalValue = 0;
		totalDuration = 0;
		confInterval.clearValuesButKeepConfInts();
		timeOfLastSampleRecording = 0;
	}

	public double getTotalValue() {
		return totalValue;
	}

	public double getTotalDuration() {
		return totalDuration;
	}

	public double getMean() {
		try {
			return confInterval.getMean();
		} catch (IndexOutOfBoundsException e) {
			System.out.println("\noops\n");
			throw new RuntimeException(e);
			// return totalValue / totalDuration;
		}
	}

	public boolean isValuesCapturedSinceLastCI() {
		return valuesCapturedSinceLastCI;
	}
}
