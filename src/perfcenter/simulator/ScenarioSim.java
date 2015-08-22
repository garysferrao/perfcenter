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
	public SummationMetric noOfReqCompletedSuccessfully = new SummationMetric();
	public SummationMetric noOfReqProcessed = new SummationMetric();
	public SummationMetric noOfReqTimedoutDuringService = new SummationMetric();
	public SummationMetric noOfReqTimedoutInBuffer = new SummationMetric();
	public SummationMetric noOfReqDropped = new SummationMetric();
	public SummationMetric noOfReqArrived = new SummationMetric();

	public DiscreteSampleAverageMetric avgRespTimeSim = new DiscreteSampleAverageMetric(ModelParameters.getResptCILevel());
	public ManuallyComputedMetric avgThroughputSim = new ManuallyComputedMetric(ModelParameters.getTputCILevel());
	public ManuallyComputedMetric avgBadputSim = new ManuallyComputedMetric(0.95); //FIXME remove hardcoding
	public ManuallyComputedMetric avgGoodputSim = new ManuallyComputedMetric(0.95); //FIXME remove hardcoding
	public ManuallyComputedMetric buffTimeoutSim = new ManuallyComputedMetric(0.95); //FIXME remove hardcoding
	public ManuallyComputedMetric dropRateSim = new ManuallyComputedMetric(0.95); //FIXME remove hardcoding
	public ManuallyComputedMetric blockProbSim = new ManuallyComputedMetric(0.95); //FIXME remove hardcoding
	public ManuallyComputedMetric arateToScenarioDuringSimulationSim = new ManuallyComputedMetric(0.95); //FIXME remove hardcoding

	public ScenarioSim(Scenario s) {
		name = s.getName();
		scenarioProb = s.scenarioProb;
		rootNode = s.rootNode; // root node of scenario tree
		arateToScenario.copy(s.arateToScenario); // scenario arrival rate
	}

	public void updateMeasuresAtTheEndOfRequestCompletion(Request rq) {
		noOfReqProcessed.recordValue(1);
		avgRespTimeSim.recordValue(SimulationParameters.currTime - rq.scenarioArrivalTime);
		
		if (ModelParameters.timeoutEnabled == true) {
			if ((SimulationParameters.currTime < rq.scenarioTimeout)) {
				noOfReqCompletedSuccessfully.recordValue(1);
			} else {
				noOfReqTimedoutDuringService.recordValue(1);
				rq.timeoutFlagAfterService = true;
			}
		} else {
			noOfReqCompletedSuccessfully.recordValue(1);
		}
	}

	public int getNumOfRequestsCompletedSuccessfully() {
		return (int) noOfReqCompletedSuccessfully.getTotalValue();
	}

	public int getNumOfRequestsProcessed() {
		return (int) noOfReqProcessed.getTotalValue();
	}

	public int getNumOfRequestsTimedoutDuringService() {
		return (int) noOfReqTimedoutDuringService.getTotalValue();
	}

	public int getNumOfRequestsTimedoutInBuffer() {
		return (int) noOfReqTimedoutInBuffer.getTotalValue();
	}

	public int getNumOfRequestsDropped() {
		return (int) noOfReqDropped.getTotalValue();
	}

	public int getNumOfRequestsArrived() {
		return (int)noOfReqArrived.getTotalValue();
	}

	public double getAverageResponseTimeSim() {
		return avgRespTimeSim.getMean();
	}
	
	public void computeConfIvalsAtEndOfRepl() {
		DistributedSystemSim.computeConfIvalForMetric(avgRespTime, avgRespTimeSim);
		DistributedSystemSim.computeConfIvalForMetric(avgThroughput, avgThroughputSim);
		DistributedSystemSim.computeConfIvalForMetric(avgBadput, avgBadputSim);
		DistributedSystemSim.computeConfIvalForMetric(avgGoodput, avgGoodputSim);
		DistributedSystemSim.computeConfIvalForMetric(buffTimeout, buffTimeoutSim);
		DistributedSystemSim.computeConfIvalForMetric(dropRate, dropRateSim);
		DistributedSystemSim.computeConfIvalForMetric(blockingProb, blockProbSim);
		DistributedSystemSim.computeConfIvalForMetric(arateToScenarioDuringSimulation, arateToScenarioDuringSimulationSim);
	}
	
	public void clearValuesButKeepConfIvals() {
		noOfReqCompletedSuccessfully.clearValuesButKeepConfInts();
		noOfReqProcessed.clearValuesButKeepConfInts();
		noOfReqTimedoutDuringService.clearValuesButKeepConfInts();
		noOfReqTimedoutInBuffer.clearValuesButKeepConfInts();
		noOfReqDropped.clearValuesButKeepConfInts();
		noOfReqArrived.clearValuesButKeepConfInts();
		
		avgRespTimeSim.clearValuesButKeepConfInts();
		avgThroughputSim.clearValuesButKeepConfIvals();
		avgBadputSim.clearValuesButKeepConfIvals();
		avgGoodputSim.clearValuesButKeepConfIvals();
		buffTimeoutSim.clearValuesButKeepConfIvals();
		dropRateSim.clearValuesButKeepConfIvals();
		blockProbSim.clearValuesButKeepConfIvals();
		arateToScenarioDuringSimulationSim.clearValuesButKeepConfIvals();
	}

	public void recordCISampleAtTheEndOfSimulation() {
		for (int slot = 0; slot < ModelParameters.intervalSlotCount; slot++) {
			avgRespTimeSim.recordCISample(slot);
			avgThroughputSim.recordCISample(slot, (noOfReqTimedoutDuringService.getTotalValue(slot) + noOfReqCompletedSuccessfully.getTotalValue(slot)) / SimulationParameters.getIntervalSlotRunTime(slot));
			avgBadputSim.recordCISample(slot, noOfReqTimedoutDuringService.getTotalValue(slot) / SimulationParameters.getIntervalSlotRunTime(slot));
			avgGoodputSim.recordCISample(slot, noOfReqCompletedSuccessfully.getTotalValue(slot) / SimulationParameters.getIntervalSlotRunTime(slot));
			buffTimeoutSim.recordCISample(slot, noOfReqTimedoutInBuffer.getTotalValue(slot) / SimulationParameters.getIntervalSlotRunTime(slot));
			dropRateSim.recordCISample(slot, noOfReqDropped.getTotalValue(slot) / SimulationParameters.getIntervalSlotRunTime(slot));
			blockProbSim.recordCISample(slot, (noOfReqDropped.getTotalValue(slot) + noOfReqTimedoutInBuffer.getTotalValue(slot)) / noOfReqArrived.getTotalValue(slot));
			arateToScenarioDuringSimulationSim.recordCISample(slot, noOfReqArrived.getTotalValue(slot) / SimulationParameters.getIntervalSlotRunTime(slot));
		}
	}
}
