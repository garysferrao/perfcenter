/*
ver * Copyright (C) 2011-12  by Varsha Apte - <varsha@cse.iitb.ac.in>, et al.
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
package perfcenter.simulator;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Random;
import java.util.Set;

import org.apache.log4j.Logger;

import perfcenter.baseclass.Device;
import perfcenter.baseclass.DistributedSystem;
import perfcenter.baseclass.Machine;
import perfcenter.baseclass.PhysicalMachine;
import perfcenter.baseclass.VirtualMachine;
import perfcenter.baseclass.LanLink;
import perfcenter.baseclass.Metric;
import perfcenter.baseclass.ModelParameters;
import perfcenter.baseclass.TaskNode;
import perfcenter.baseclass.Scenario;
import perfcenter.baseclass.SoftServer;
import perfcenter.baseclass.Task;
import perfcenter.simulator.metric.ManuallyComputedMetric;
import perfcenter.simulator.metric.MetricSim;
import perfcenter.simulator.queue.QueueSim;

/**
 * This has all the distributed system parameters as given in input file. It uses inherited class HostAna for Host and ServerAna for server.
 * 
 * @author akhila
 */
public class DistributedSystemSim extends DistributedSystem {

	Logger logger = Logger.getLogger("DistSysSim");
	public int cycleSamples = 1;

	//bhavin: added for scalability
	public HashMap<String, ScenarioSim> scenarioMap = new HashMap<String, ScenarioSim>();
	public HashMap<String, PhysicalMachineSim> machineMap = new HashMap<String, PhysicalMachineSim>();
	public HashMap<String, SoftServerSim> softServerMap = new HashMap<String, SoftServerSim>();
	
	public ManuallyComputedMetric overallGoodputSim = new ManuallyComputedMetric(0.95); //FIXME remove hardcoding
	public ManuallyComputedMetric overallBadputSim = new ManuallyComputedMetric(0.95); //FIXME remove hardcoding
	public ManuallyComputedMetric overallThroughputSim = new ManuallyComputedMetric(0.95); //FIXME remove hardcoding
	public ManuallyComputedMetric overallDroprateSim = new ManuallyComputedMetric(0.95); //FIXME remove hardcoding
	public ManuallyComputedMetric overallBuffTimeoutSim = new ManuallyComputedMetric(0.95); //FIXME remove hardcoding
	public ManuallyComputedMetric overallRespTimeSim = new ManuallyComputedMetric(0.95); //FIXME remove hardcoding
	public ManuallyComputedMetric overallBlockingProbSim = new ManuallyComputedMetric(0.95); //FIXME remove hardcoding

	public DistributedSystemSim() {
		logger.debug("breakpoint");
	}
	
	public DistributedSystemSim(DistributedSystem ds) {
		softRes = ds.softRes;
		
		devcats = ds.devcats;
		pdevices = ds.pdevices;
		
		variables = ds.variables;
		tasks = ds.tasks;
		// softServers = ds.softServers;
		// scenarios = ds.scenarios;
		lans = ds.lans;
		// links = ds.links;

		ArrayList<SoftServerSim> softServersTemp = new ArrayList<SoftServerSim>();
		for(SoftServer softServer : softServers.values()) { //to take care of the "user" server created by parent constructor of DistributedSystem class.
			SoftServerSim softServerSim = new SoftServerSim(softServer);
			softServersTemp.add(softServerSim);
		}
		for(SoftServer softServer : ds.softServers.values()) {
			SoftServerSim softServerSim = new SoftServerSim(softServer);
			softServersTemp.add(softServerSim);
		}
		softServers.clear();
		for(SoftServerSim softServerSim : softServersTemp) {
			softServers.put(softServerSim.getName(), softServerSim);
			softServerMap.put(softServerSim.getName(), softServerSim);
		}
		
		for (LanLink lk : ds.links.values()) {
			links.put(lk.name, new LanLinkSim(lk));
		}

		for (PhysicalMachine pm : ds.pms.values()) {
			PhysicalMachineSim pmsim = new PhysicalMachineSim(pm, softServerMap);
			pms.put(pmsim.getName(), pmsim);
			machineMap.put(pmsim.getName(), pmsim);
		}
		// this need not be done if its use in perfanalytic is fixed
		for (SoftServer s : softServers.values()) {
			for (Task t : s.simpleTasks) {
				t.initialize();
			}
		}
		for (Scenario sce : ds.scenarios.values()) {
			ScenarioSim s = new ScenarioSim(sce);
			scenarios.put(s.getName(), s);
			scenarioMap.put(s.getName(), s);
		}

	}

	// clear all the variables but keep the confidence interval calculations
	public void clearValuesButKeepConfIvals() {
		for (Machine m : pms.values()) {
			for (SoftServer softServer : m.softServers.values()) {
				((SoftServerSim) softServer).clearValuesButKeepConfIvals();
//				softServerSim.resourceQueue = QueueSim.loadSchedulingPolicyClass(softServerSim.schedp.toString(), buffSizeTemp, (int) softServerSim.thrdCount.getValue(), softServerSim);
			}
			for (Object dev : m.devices.values()) {
				((DeviceSim) dev).clearValuesButKeepConfIvals();
//				deviceSim.resourceQueue = QueueSim.loadSchedulingPolicyClass(deviceSim.schedulingPolicy.toString(), buffSizeTemp, ((QueueSim) deviceSim.resourceQueue).numberOfInstances, /* "hwRes", */deviceSim);
			}
		}
		for (Object lk : links.values()) {
			LanLinkSim lnk = (LanLinkSim) lk;
			((QueueSim) lnk.linkQforward).clearValuesButKeepConfIvals();
			((QueueSim) lnk.linkQreverse).clearValuesButKeepConfIvals();
		}
		for (Object sce : scenarios.values()) {
			((ScenarioSim) sce).clearValuesButKeepConfIvals();
		}
		
		overallGoodputSim.clearValuesButKeepConfIvals();
		overallBadputSim.clearValuesButKeepConfIvals();
		overallBlockingProbSim.clearValuesButKeepConfIvals();
		overallThroughputSim.clearValuesButKeepConfIvals();
		overallDroprateSim.clearValuesButKeepConfIvals();
		overallBuffTimeoutSim.clearValuesButKeepConfIvals();
		overallRespTimeSim.clearValuesButKeepConfIvals();
	}
	
	/**
	 * calls confInt.calConfidenceIntervals() for each of the s/w servers in softServerList confInt.calConfidenceIntervals() in turn calculates the
	 * confidence intervals for the performance measures modified by akhila
	 * 
	 * There are two different devices, queuing and non-queueing. Currently(Date:12-05-11) RAM is the only non-queuing device in the tool. But modeled
	 * as queuing device, and buffSize is used as RAM size. So all other measures except utilization is of no use. Thus seperate procedure is
	 * implemented for evaluating RAM utiliztion. This procedure will calculate the mean, and confidence interval for RAM utilization modified by
	 * nikhil, for the device RAM.
	 */
	// calculates confidence interval for each queue and
	// addes the results to the queue variables
	public void computeConfIvalsAtEndOfRepl() {
		for (Machine m : pms.values()) {
			for (SoftServer softServer : m.softServers.values()) {
				((QueueSim) ((SoftServerSim) softServer).resourceQueue).computeConfIvalsAtEndOfRepl();
				// so.setThroughput(((QueueSim) so.softQ).giveMeThroughtput());
			}
			for (Device device : m.devices.values()) {
				// calculate RAM utilization for current host if device is RAM.
				if (device.name.equals("ram")) {
					//XXX argument passing redundant in next line
					((QueueSim) ((DeviceSim) device).resourceQueue).computeConfIvalsForRAM(ModelParameters.inputDistSys.getPM(m.name));
				} else {
					((DeviceSim) device).avgFreqSim.computeConfIvalsAtEndOfRepl();
					DistributedSystemSim.computeConfIvalForMetric(device.averageFrequency, ((DeviceSim) device).avgFreqSim);
					
					((QueueSim) ((DeviceSim) device).resourceQueue).computeConfIvalsAtEndOfRepl();
				}
			}
		}
		for (LanLink lanLink : links.values()) {
			((QueueSim) ((LanLinkSim) lanLink).linkQforward).computeConfIvalsAtEndOfRepl();
			((QueueSim) ((LanLinkSim) lanLink).linkQreverse).computeConfIvalsAtEndOfRepl();
		}
		
		for(Scenario scenario : scenarios.values()) {
			((ScenarioSim) scenario).computeConfIvalsAtEndOfRepl();
		}
		
		computeScenarioEndToEndValuesAtTheEndOfRepl();
	}
	
	/**
	 * Calculates avg response time, tput and arrival rate of all scenarios function added by akhila modified by nikhil: Gput, Bput, Tput,
	 * BuffTimeout.
	 * 
	 */
	void computeScenarioEndToEndValuesAtTheEndOfRepl() {
		DistributedSystemSim dss = SimulationParameters.distributedSystemSim;

		DistributedSystemSim.computeConfIvalForMetric(dss.overallGoodput, dss.overallGoodputSim);
		DistributedSystemSim.computeConfIvalForMetric(dss.overallBadput, dss.overallBadputSim);
		DistributedSystemSim.computeConfIvalForMetric(dss.overallThroughput, dss.overallThroughputSim);
		DistributedSystemSim.computeConfIvalForMetric(dss.overallDroprate, dss.overallDroprateSim);
		DistributedSystemSim.computeConfIvalForMetric(dss.overallBuffTimeout, dss.overallBuffTimeoutSim);
		DistributedSystemSim.computeConfIvalForMetric(dss.overallRespTime, dss.overallRespTimeSim);
		DistributedSystemSim.computeConfIvalForMetric(dss.overallBlockProb, dss.overallBlockingProbSim);
	}

	private void recordScenarioEndToEndCISamplesAtTheEndOfSimulation() {
		DistributedSystemSim dss = SimulationParameters.distributedSystemSim;

		for (int slot = 0; slot < ModelParameters.intervalSlotCount; slot++) {
			int totNumOfReq = 0;
			double totResponseTime, overallGoodput, overallBadput, overallThroughput;
			double overallBuffTimeout, overallDroprate, overallArrivalRate;
			totResponseTime = overallGoodput = overallBadput = overallThroughput = 0;
			overallBuffTimeout = overallDroprate = overallArrivalRate = 0;

			for (Object scenario : SimulationParameters.distributedSystemSim.scenarios.values()) {
				ScenarioSim scenarioSim = (ScenarioSim) scenario;

				totResponseTime += scenarioSim.avgRespTimeSim.getTotalValue(slot);
				totNumOfReq += scenarioSim.noOfReqCompletedSuccessfully.getTotalValue(slot)
						+ scenarioSim.noOfReqTimedoutDuringService.getTotalValue(slot);
				overallGoodput += scenarioSim.avgGoodputSim.getValue(slot, SimulationParameters.replicationNo);
				overallBadput += scenarioSim.avgBadputSim.getValue(slot, SimulationParameters.replicationNo);
				overallThroughput += scenarioSim.avgThroughputSim.getValue(slot, SimulationParameters.replicationNo);
				overallBuffTimeout += scenarioSim.buffTimeoutSim.getValue(slot, SimulationParameters.replicationNo);
				overallDroprate += scenarioSim.dropRateSim.getValue(slot, SimulationParameters.replicationNo);
				overallArrivalRate += scenarioSim.arateToScenarioDuringSimulation.getValue(slot);
			}

			dss.overallGoodputSim.recordCISample(slot, overallGoodput);
			dss.overallBadputSim.recordCISample(slot, overallBadput);
			dss.overallThroughputSim.recordCISample(slot, overallThroughput);
			dss.overallBuffTimeoutSim.recordCISample(slot, overallBuffTimeout);
			dss.overallDroprateSim.recordCISample(slot, overallDroprate);
			dss.overallRespTimeSim.recordCISample(slot, totResponseTime / totNumOfReq);
			dss.overallBlockingProbSim.recordCISample(slot, (overallDroprate + overallBuffTimeout)/totNumOfReq);

			dss.overallArrivalRate.setValue(slot, overallArrivalRate);
		}
	}

	/** 
	 * This procedure is called at the end of every simulation run.
	 * and updates the performance measures of the queues.
	 * 
	 * RAM measures should be caclulated once confidence interval(CI) of average queue length is calculated, which is done when all simulation runs
	 * are over. CI procedure will evaluate the mean, and will generate the lower and upper bound of CI for all the performance measures including
	 * average queue length, then we can easily put those value in memory releated formula and measures can be evaluated easily. So hold on and do
	 * nothing if the device is RAM. Procedure modified by nikhil, for the device RAM.
	 */
	public void recordCISampleAtTheEndOfSimulation() {
		for (Machine m : pms.values()) {
			for (SoftServer softServer : m.softServers.values()) {
				((SoftServerSim) softServer).recordCISampleAtTheEndOfSimulation();
			}

			for (Device device : m.devices.values()) {
				DeviceSim deviceSim = (DeviceSim) device;
				if (!device.name.equals("ram")) {
					deviceSim.recordCISampleAtTheEndOfSimulation();
				}
			}
		}

		for (LanLink lanLink : links.values()) {
			((LanLinkSim) lanLink).recordCISampleAtTheEndOfSimulation();
		}
		
		for(Scenario scenario : scenarios.values()) {
			((ScenarioSim) scenario).recordCISampleAtTheEndOfSimulation();
		}
		
		recordScenarioEndToEndCISamplesAtTheEndOfSimulation();
	}
	
	public TaskNode findNextTaskNode(TaskNode cf) {
		if (cf.children.isEmpty()) {
			return null;
		}
		if (cf.children.size() == 1) {
			return cf.children.get(0);
		}

		// this is the case when we need to choose from more than one
		// children
		Random r = new Random();
		double d = r.nextDouble();
		double dbl = 0.0;
		for (TaskNode c : cf.children) {
			dbl += c.prob.getValue();
			if (dbl >= d) {
				return c;
			}
		}
		return null;
	}

	public static void computeConfIvalForMetric(Metric metric, MetricSim metricSim) {
		metricSim.computeConfIvalsAtEndOfRepl();
		for (int slot = 0; slot < ModelParameters.intervalSlotCount; slot++) {
			//nadeesh Store the Slot level Metric value
			metric.setValue(slot, metricSim.getMean(slot));
			metric.setConfidenceInterval(slot, metricSim.getCI(slot));
			
		if (metricSim.getServerList(slot) != null) {
				Set<String> srvList=metricSim.getServerList(slot);
				for(String srvName:srvList){
					//nadeesh Store the server level Metric value
					metric.setValue(slot,srvName,metricSim.getMean(slot,srvName));
					metric.setConfIval(slot,srvName, metricSim.getCI(slot,srvName));
				}
			}
		}
		metric.setConfidenceProbability(metricSim.getProbability(SimulationParameters.getIntervalSlotCounter()));
	}
	
	public ScenarioSim getScenario(String name) {
		ScenarioSim scenario = scenarioMap.get(name);
		if(scenario != null) {
			return scenario;
		}
		throw new Error(name + " is not Scenario");
	}
	
	public boolean isScenario(String name) {
		return scenarioMap.containsKey(name);
	}
	
	public PhysicalMachineSim getPM(String name) {
		PhysicalMachineSim host = machineMap.get(name);
		if(host != null) {
			return host;
		}
		throw new Error(name + " is not Host");
	}

	public boolean isPM(String name) {
		return machineMap.containsKey(name);
	}
	
	public SoftServerSim getServer(String name) {
		SoftServerSim softServerSim = softServerMap.get(name);
		if(softServerSim != null) {
			return softServerSim;
		}
		throw new Error(name + " is not Server");
	}

	public boolean isServer(String name) {
		return softServerMap.containsKey(name);
	}
}
