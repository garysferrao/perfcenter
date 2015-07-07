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
/**
 * Inherited from Scenario. Has functions and variables required by simulation part
 * of PerfCenter
 * @author  akhila
 */
package perfcenter.simulator;

import perfcenter.baseclass.ModelParameters;
import perfcenter.baseclass.Scenario;
import perfcenter.simulator.metric.DiscreteSampleAverageMetric;
import perfcenter.simulator.metric.ManuallyComputedMetric;
import perfcenter.simulator.metric.SummationMetric;
import perfcenter.simulator.request.Request;

/** Contains simulation book-keeping for the Scenario object. */
public class ScenarioSim extends Scenario {

	// used by simulation part
	public SummationMetric numOfRequestsCompletedSuccessfully = new SummationMetric();
	public SummationMetric numOfRequestsProcessed = new SummationMetric();
	public SummationMetric numOfRequestsTimedoutDuringService = new SummationMetric();
	public SummationMetric numOfRequestsTimedoutInBuffer = new SummationMetric();
	public SummationMetric numOfRequestsDropped = new SummationMetric();
	public SummationMetric numOfRequestsArrived = new SummationMetric();

	public DiscreteSampleAverageMetric averageResponseTimeSim = new DiscreteSampleAverageMetric(ModelParameters.getResptCILevel());
	public ManuallyComputedMetric averageThroughputSim = new ManuallyComputedMetric(ModelParameters.getTputCILevel());
	public ManuallyComputedMetric averageBadputSim = new ManuallyComputedMetric(0.95); //FIXME remove hardcoding
	public ManuallyComputedMetric averageGoodputSim = new ManuallyComputedMetric(0.95); //FIXME remove hardcoding
	public ManuallyComputedMetric buffTimeoutSim = new ManuallyComputedMetric(0.95); //FIXME remove hardcoding
	public ManuallyComputedMetric dropRateSim = new ManuallyComputedMetric(0.95); //FIXME remove hardcoding
	public ManuallyComputedMetric blockingProbabilitySim = new ManuallyComputedMetric(0.95); //FIXME remove hardcoding
	public ManuallyComputedMetric arateToScenarioDuringSimulationSim = new ManuallyComputedMetric(0.95); //FIXME remove hardcoding

	public ScenarioSim(Scenario s) {
		scenarioName = s.getName();
		scenarioProbability = s.scenarioProbability;
		rootNodeOfScenario = s.rootNodeOfScenario; // root node of scenario tree
		arateToScenario.copy(s.arateToScenario); // scenario arrival rate
	}

	public void updateMeasuresAtTheEndOfRequestCompletion(Request rq) {
		numOfRequestsProcessed.recordValue(1);
		averageResponseTimeSim.recordValue(SimulationParameters.currentTime - rq.scenarioArrivalTime);
		
		if (ModelParameters.timeoutEnabled == true) {
			if ((SimulationParameters.currentTime < rq.scenarioTimeout)) {
				numOfRequestsCompletedSuccessfully.recordValue(1);
			} else {
				numOfRequestsTimedoutDuringService.recordValue(1);
				rq.timeoutFlagAfterService = true;
			}
		} else {
			numOfRequestsCompletedSuccessfully.recordValue(1);
		}
	}

	public int getNumOfRequestsCompletedSuccessfully() {
		return (int) numOfRequestsCompletedSuccessfully.getTotalValue();
	}

	public int getNumOfRequestsProcessed() {
		return (int) numOfRequestsProcessed.getTotalValue();
	}

	public int getNumOfRequestsTimedoutDuringService() {
		return (int) numOfRequestsTimedoutDuringService.getTotalValue();
	}

	public int getNumOfRequestsTimedoutInBuffer() {
		return (int) numOfRequestsTimedoutInBuffer.getTotalValue();
	}

	public int getNumOfRequestsDropped() {
		return (int) numOfRequestsDropped.getTotalValue();
	}

	public int getNumOfRequestsArrived() {
		return (int)numOfRequestsArrived.getTotalValue();
	}

	public double getAverageResponseTimeSim() {
		return averageResponseTimeSim.getMean();
	}
	
	public void calculateConfidenceIntervalsAtTheEndOfReplications() {
		DistributedSystemSim.calculateConfidenceIntervalForMetric(averageResponseTime, averageResponseTimeSim);
		DistributedSystemSim.calculateConfidenceIntervalForMetric(averageThroughput, averageThroughputSim);
		DistributedSystemSim.calculateConfidenceIntervalForMetric(averageBadput, averageBadputSim);
		DistributedSystemSim.calculateConfidenceIntervalForMetric(averageGoodput, averageGoodputSim);
		DistributedSystemSim.calculateConfidenceIntervalForMetric(buffTimeout, buffTimeoutSim);
		DistributedSystemSim.calculateConfidenceIntervalForMetric(dropRate, dropRateSim);
		DistributedSystemSim.calculateConfidenceIntervalForMetric(blockingProb, blockingProbabilitySim);
		DistributedSystemSim.calculateConfidenceIntervalForMetric(arateToScenarioDuringSimulation, arateToScenarioDuringSimulationSim);
	}
	
	public void clearValuesButKeepConfInts() {
		numOfRequestsCompletedSuccessfully.clearValuesButKeepConfInts();
		numOfRequestsProcessed.clearValuesButKeepConfInts();
		numOfRequestsTimedoutDuringService.clearValuesButKeepConfInts();
		numOfRequestsTimedoutInBuffer.clearValuesButKeepConfInts();
		numOfRequestsDropped.clearValuesButKeepConfInts();
		numOfRequestsArrived.clearValuesButKeepConfInts();
		
		averageResponseTimeSim.clearValuesButKeepConfInts();
		averageThroughputSim.clearValuesButKeepConfInts();
		averageBadputSim.clearValuesButKeepConfInts();
		averageGoodputSim.clearValuesButKeepConfInts();
		buffTimeoutSim.clearValuesButKeepConfInts();
		dropRateSim.clearValuesButKeepConfInts();
		blockingProbabilitySim.clearValuesButKeepConfInts();
		arateToScenarioDuringSimulationSim.clearValuesButKeepConfInts();
	}

	public void recordCISampleAtTheEndOfSimulation() {
		for (int slot = 0; slot < ModelParameters.intervalSlotCount; slot++) {
			averageResponseTimeSim.recordCISample(slot);
			averageThroughputSim.recordCISample(slot, (numOfRequestsTimedoutDuringService.getTotalValue(slot) + numOfRequestsCompletedSuccessfully.getTotalValue(slot)) / SimulationParameters.getIntervalSlotRunTime(slot));
			averageBadputSim.recordCISample(slot, numOfRequestsTimedoutDuringService.getTotalValue(slot) / SimulationParameters.getIntervalSlotRunTime(slot));
			averageGoodputSim.recordCISample(slot, numOfRequestsCompletedSuccessfully.getTotalValue(slot) / SimulationParameters.getIntervalSlotRunTime(slot));
			buffTimeoutSim.recordCISample(slot, numOfRequestsTimedoutInBuffer.getTotalValue(slot) / SimulationParameters.getIntervalSlotRunTime(slot));
			dropRateSim.recordCISample(slot, numOfRequestsDropped.getTotalValue(slot) / SimulationParameters.getIntervalSlotRunTime(slot));
			blockingProbabilitySim.recordCISample(slot, (numOfRequestsDropped.getTotalValue(slot) + numOfRequestsTimedoutInBuffer.getTotalValue(slot)) / numOfRequestsArrived.getTotalValue(slot));
			arateToScenarioDuringSimulationSim.recordCISample(slot, numOfRequestsArrived.getTotalValue(slot) / SimulationParameters.getIntervalSlotRunTime(slot));
		}
	}
}
