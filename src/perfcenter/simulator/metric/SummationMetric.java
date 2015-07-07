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

import org.apache.log4j.Logger;

import perfcenter.baseclass.ModelParameters;
import perfcenter.simulator.SimulationParameters;
import perfcenter.simulator.request.Request;


/**
 * This class is just a wrapper around a summation variable, like total number of requests served.
 * <p>
 * These set of classes also manage the server and slot level break up of this summation.
 * <p>
 * The top level class SummationMetric has the information at the level of the cyclic workload slots. The next level
 * _SummationMetricSingleSlot manages the server level bifurcation of the data for the given slot. The lowest level
 * _SummationMetricLowestLevel manages the discrete data for the given slot and given server.
 * 
 * @author bhavin
 * 
 */
public class SummationMetric {
	_SummationMetricSingleSlot[] metricSlots = new _SummationMetricSingleSlot[ModelParameters.intervalSlotCount];

	public SummationMetric() {
		for (int i = 0; i < metricSlots.length; i++) {
			metricSlots[i] = new _SummationMetricSingleSlot();
		}
	}

	public void clearEverything() {
		for (_SummationMetricSingleSlot slot : metricSlots) {
			slot.clearEverything();
		}
	}

	public void clearValuesButKeepConfInts() {
		for (_SummationMetricSingleSlot slot : metricSlots) {
			slot.clearValuesButKeepConfInts();
		}
	}

	public void recordValue(double sampleValue) {
		metricSlots[SimulationParameters.getIntervalSlotCounter()].recordValue(null, sampleValue);
	}

	public void recordValue(Request request, double sampleValue) {
		metricSlots[SimulationParameters.getIntervalSlotCounter()].recordValue(request, sampleValue);
	}

	public double getTotalValue(int slot) {
		return metricSlots[slot].getTotalValue();
	}

	public double getTotalValue() { // XXX purge this eventually, just keep the getTotalValue(int)
		return metricSlots[SimulationParameters.getIntervalSlotCounter()].getTotalValue();
	}

	public double getTotalValue(String serverName) {
		return metricSlots[SimulationParameters.getIntervalSlotCounter()].getTotalValue(serverName);
	}

	public double getTotalValue(int slot, String serverName) {
		return metricSlots[slot].getTotalValue(serverName);
	}

}
/**
 * This class contains the server level bifurcation of values.
 * @author nadeesh
 */
class _SummationMetricSingleSlot {
	Logger logger = Logger.getLogger("SummationMetric");
	Map<String, _SummationMetricLowestLevel> perServerMetric = new HashMap<String, _SummationMetricLowestLevel>();

	public _SummationMetricSingleSlot() {
		_SummationMetricLowestLevel summationMetrSlot = new _SummationMetricLowestLevel();
		summationMetrSlot.recordValue(0);
		perServerMetric.put("_total", summationMetrSlot);
	}

	public void recordValue(Request req, double sampleValue) {
		String serverName = "_passthrough";
		if (req != null) {
			serverName = req.fromServer;
		}
		if (perServerMetric.containsKey(serverName)) {
			perServerMetric.get(serverName).recordValue(sampleValue);
		} else {
			_SummationMetricLowestLevel sumMetr = new _SummationMetricLowestLevel();
			sumMetr.recordValue(sampleValue);
			perServerMetric.put(serverName, sumMetr);

		}
		perServerMetric.get("_total").recordValue(sampleValue);
	}

	public double getTotalValue(String serverName) {
		if(serverName == null) {
			serverName = "_total";
		}
		return perServerMetric.get(serverName) != null ? perServerMetric.get(serverName).getTotalValue() : 0;
	}

	public double getTotalValue() {
		return perServerMetric.get("_total") != null ? perServerMetric.get("_total").getTotalValue() : 0;
	}

	public void clearEverything() {
		for (_SummationMetricLowestLevel sumMetr : perServerMetric.values())
			sumMetr.clearEverything();
	}

	public void clearValuesButKeepConfInts() {
		clearEverything();
	}

}
/**
 * This class contains the low level implementation of the summation mechanism for the given server and given slot.
 * @author bhavin
 */

class _SummationMetricLowestLevel {

	private double totalValue = 0;

	public void clearEverything() {
		totalValue = 0;
	}

	public void clearValuesButKeepConfInts() {
		clearEverything();
	}

	public void recordValue(double sampleValue) {
		if(SimulationParameters.warmupEnabled) {
			return;
		}
		
		totalValue += sampleValue;
	}

	public double getTotalValue() {
		return totalValue;
	}

}
