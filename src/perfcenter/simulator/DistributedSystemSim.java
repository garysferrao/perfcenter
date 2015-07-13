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
package perfcenter.simulator;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Random;
import java.util.Set;

import org.apache.log4j.Logger;

import perfcenter.baseclass.Device;
import perfcenter.baseclass.DistributedSystem;
import perfcenter.baseclass.Host;
import perfcenter.baseclass.LanLink;
import perfcenter.baseclass.Metric;
import perfcenter.baseclass.ModelParameters;
import perfcenter.baseclass.Node;
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
	public HashMap<String, HostSim> hostMap = new HashMap<String, HostSim>();
	public HashMap<String, SoftServerSim> softServerMap = new HashMap<String, SoftServerSim>();
	
	public ManuallyComputedMetric overallGoodputSim = new ManuallyComputedMetric(0.95); //FIXME remove hardcoding
	public ManuallyComputedMetric overallBadputSim = new ManuallyComputedMetric(0.95); //FIXME remove hardcoding
	public ManuallyComputedMetric overallThroughputSim = new ManuallyComputedMetric(0.95); //FIXME remove hardcoding
	public ManuallyComputedMetric overallDroprateSim = new ManuallyComputedMetric(0.95); //FIXME remove hardcoding
	public ManuallyComputedMetric overallBuffTimeoutSim = new ManuallyComputedMetric(0.95); //FIXME remove hardcoding
	public ManuallyComputedMetric overallResponseTimeSim = new ManuallyComputedMetric(0.95); //FIXME remove hardcoding
	public ManuallyComputedMetric overallBlockingProbabilitySim = new ManuallyComputedMetric(0.95); //FIXME remove hardcoding

	public DistributedSystemSim() {
		logger.debug("breakpoint");
	}
	
	public DistributedSystemSim(DistributedSystem ds) {
		virRes = ds.virRes;
		devices = ds.devices;
		variables = ds.variables;
		tasks = ds.tasks;
		// softServers = ds.softServers;
		// scenarios = ds.scenarios;
		lans = ds.lans;
		// links = ds.links;

		ArrayList<SoftServerSim> softServersTemp = new ArrayList<SoftServerSim>();
		for(SoftServer softServer : softServers) { //to take care of the "user" server created by parent constructor of DistributedSystem class.
			SoftServerSim softServerSim = new SoftServerSim(softServer);
			softServersTemp.add(softServerSim);
		}
		for(SoftServer softServer : ds.softServers) {
			SoftServerSim softServerSim = new SoftServerSim(softServer);
			softServersTemp.add(softServerSim);
		}
		softServers.clear();
		for(SoftServerSim softServerSim : softServersTemp) {
			softServers.add(softServerSim);
			softServerMap.put(softServerSim.getName(), softServerSim);
		}
		
		for (LanLink lk : ds.links) {
			links.add(new LanLinkSim(lk));
		}

		for (Host h : ds.hosts) {
			HostSim hs = new HostSim(h, softServerMap);
			hosts.add(hs);
			hostMap.put(hs.getName(), hs);
		}
		// this need not be done if its use in perfanalytic is fixed
		for (SoftServer s : softServers) {
			for (Task t : s.simpleTasks) {
				t.initialize();
			}
		}
		for (Scenario sce : ds.scenarios) {
			ScenarioSim s = new ScenarioSim(sce);
			scenarios.add(s);
			scenarioMap.put(s.getName(), s);
		}

	}

	// clear all the variables but keep the confidence interval calculations
	public void clearValuesButKeepConfInts() {
		for (Host h : hosts) {
			for (SoftServer softServer : h.softServers) {
				((SoftServerSim) softServer).clearValuesButKeepConfInts();
//				softServerSim.resourceQueue = QueueSim.loadSchedulingPolicyClass(softServerSim.schedp.toString(), buffSizeTemp, (int) softServerSim.thrdCount.getValue(), softServerSim);
			}
			for (Object dev : h.devices) {
				((DeviceSim) dev).clearValuesButKeepConfInts();
//				deviceSim.resourceQueue = QueueSim.loadSchedulingPolicyClass(deviceSim.schedulingPolicy.toString(), buffSizeTemp, ((QueueSim) deviceSim.resourceQueue).numberOfInstances, /* "hwRes", */deviceSim);
			}
		}
		for (Object lk : links) {
			LanLinkSim lnk = (LanLinkSim) lk;
			((QueueSim) lnk.linkQforward).clearValuesButKeepConfInts();
			((QueueSim) lnk.linkQreverse).clearValuesButKeepConfInts();
		}
		for (Object sce : scenarios) {
			((ScenarioSim) sce).clearValuesButKeepConfInts();
		}
		
		overallGoodputSim.clearValuesButKeepConfInts();
		overallBadputSim.clearValuesButKeepConfInts();
		overallBlockingProbabilitySim.clearValuesButKeepConfInts();
		overallThroughputSim.clearValuesButKeepConfInts();
		overallDroprateSim.clearValuesButKeepConfInts();
		overallBuffTimeoutSim.clearValuesButKeepConfInts();
		overallResponseTimeSim.clearValuesButKeepConfInts();
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
	public void calculateConfidenceIntervalsAtTheEndOfReplications() {
		for (Host host : hosts) {
			for (SoftServer softServer : host.softServers) {
				((QueueSim) ((SoftServerSim) softServer).resourceQueue).calculateConfidenceIntervalsAtTheEndOfReplications();
				// so.setThroughput(((QueueSim) so.softQ).giveMeThroughtput());
			}
			for (Device device : host.devices) {
				// calculate RAM utilization for current host if device is RAM.
				if (device.name.equals("ram")) {
					//XXX argument passing redundant in next line
					((QueueSim) ((DeviceSim) device).resourceQueue).calculateConfidenceIntervalsForRAM(ModelParameters.inputDistributedSystem.getHost(host.name));
				} else {
					((DeviceSim) device).averageFrequencySim.calculateConfidenceIntervalsAtTheEndOfReplications();
					DistributedSystemSim.calculateConfidenceIntervalForMetric(device.averageFrequency, ((DeviceSim) device).averageFrequencySim);
					
					((QueueSim) ((DeviceSim) device).resourceQueue).calculateConfidenceIntervalsAtTheEndOfReplications();
				}
			}
		}
		for (LanLink lanLink : links) {
			((QueueSim) ((LanLinkSim) lanLink).linkQforward).calculateConfidenceIntervalsAtTheEndOfReplications();
			((QueueSim) ((LanLinkSim) lanLink).linkQreverse).calculateConfidenceIntervalsAtTheEndOfReplications();
		}
		
		for(Scenario scenario : scenarios) {
			((ScenarioSim) scenario).calculateConfidenceIntervalsAtTheEndOfReplications();
		}
		
		calculateScenarioEndToEndValuesAtTheEndOfReplications();
	}
	
	/**
	 * Calculates avg response time, tput and arrival rate of all scenarios function added by akhila modified by nikhil: Gput, Bput, Tput,
	 * BuffTimeout.
	 * 
	 */
	void calculateScenarioEndToEndValuesAtTheEndOfReplications() {
		DistributedSystemSim dss = SimulationParameters.distributedSystemSim;

		DistributedSystemSim.calculateConfidenceIntervalForMetric(dss.overallGoodput, dss.overallGoodputSim);
		DistributedSystemSim.calculateConfidenceIntervalForMetric(dss.overallBadput, dss.overallBadputSim);
		DistributedSystemSim.calculateConfidenceIntervalForMetric(dss.overallThroughput, dss.overallThroughputSim);
		DistributedSystemSim.calculateConfidenceIntervalForMetric(dss.overallDroprate, dss.overallDroprateSim);
		DistributedSystemSim.calculateConfidenceIntervalForMetric(dss.overallBuffTimeout, dss.overallBuffTimeoutSim);
		DistributedSystemSim.calculateConfidenceIntervalForMetric(dss.overallResponseTime, dss.overallResponseTimeSim);
		DistributedSystemSim.calculateConfidenceIntervalForMetric(dss.overallBlockingProbability, dss.overallBlockingProbabilitySim);
	}

	private void recordScenarioEndToEndCISamplesAtTheEndOfSimulation() {
		DistributedSystemSim dss = SimulationParameters.distributedSystemSim;

		for (int slot = 0; slot < ModelParameters.intervalSlotCount; slot++) {
			int totNumOfReq = 0;
			double totResponseTime, overallGoodput, overallBadput, overallThroughput;
			double overallBuffTimeout, overallDroprate, overallArrivalRate;
			totResponseTime = overallGoodput = overallBadput = overallThroughput = 0;
			overallBuffTimeout = overallDroprate = overallArrivalRate = 0;

			for (Object scenario : SimulationParameters.distributedSystemSim.scenarios) {
				ScenarioSim scenarioSim = (ScenarioSim) scenario;

				totResponseTime += scenarioSim.averageResponseTimeSim.getTotalValue(slot);
				totNumOfReq += scenarioSim.numOfRequestsCompletedSuccessfully.getTotalValue(slot)
						+ scenarioSim.numOfRequestsTimedoutDuringService.getTotalValue(slot);
				overallGoodput += scenarioSim.averageGoodputSim.getValue(slot, SimulationParameters.replicationNumber);
				overallBadput += scenarioSim.averageBadputSim.getValue(slot, SimulationParameters.replicationNumber);
				overallThroughput += scenarioSim.averageThroughputSim.getValue(slot, SimulationParameters.replicationNumber);
				overallBuffTimeout += scenarioSim.buffTimeoutSim.getValue(slot, SimulationParameters.replicationNumber);
				overallDroprate += scenarioSim.dropRateSim.getValue(slot, SimulationParameters.replicationNumber);
				overallArrivalRate += scenarioSim.arateToScenarioDuringSimulation.getValue(slot);
			}

			dss.overallGoodputSim.recordCISample(slot, overallGoodput);
			dss.overallBadputSim.recordCISample(slot, overallBadput);
			dss.overallThroughputSim.recordCISample(slot, overallThroughput);
			dss.overallBuffTimeoutSim.recordCISample(slot, overallBuffTimeout);
			dss.overallDroprateSim.recordCISample(slot, overallDroprate);
			dss.overallResponseTimeSim.recordCISample(slot, totResponseTime / totNumOfReq);
			dss.overallBlockingProbabilitySim.recordCISample(slot, (overallDroprate + overallBuffTimeout)/totNumOfReq);

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
		for (Host host : hosts) {
			for (SoftServer softServer : host.softServers) {
				((SoftServerSim) softServer).recordCISampleAtTheEndOfSimulation();
			}

			for (Device device : host.devices) {
				DeviceSim deviceSim = (DeviceSim) device;
				if (!device.name.equals("ram")) {
					deviceSim.recordCISampleAtTheEndOfSimulation();
				}
			}
		}

		for (LanLink lanLink : links) {
			((LanLinkSim) lanLink).recordCISampleAtTheEndOfSimulation();
		}
		
		for(Scenario scenario : scenarios) {
			((ScenarioSim) scenario).recordCISampleAtTheEndOfSimulation();
		}
		
		recordScenarioEndToEndCISamplesAtTheEndOfSimulation();
	}
	
	public Node findNextNode(Node cf) {
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
		for (Node c : cf.children) {
			dbl += c.prob.getValue();
			if (dbl >= d) {
				return c;
			}
		}
		return null;
	}

	public static void calculateConfidenceIntervalForMetric(Metric metric, MetricSim metricSim) {
		metricSim.calculateConfidenceIntervalsAtTheEndOfReplications();
		for (int slot = 0; slot < ModelParameters.intervalSlotCount; slot++) {
			//nadeesh Store the Slot level Metric value
			metric.setValue(slot, metricSim.getMean(slot));
			metric.setConfidenceInterval(slot, metricSim.getCI(slot));
			
		if (metricSim.getServerList(slot) != null) {
				Set<String> srvList=metricSim.getServerList(slot);
				for(String srvName:srvList){
					//nadeesh Store the server level Metric value
					metric.setValue(slot,srvName,metricSim.getMean(slot,srvName));
					metric.setConfidenceInterval(slot,srvName, metricSim.getCI(slot,srvName));
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
	
	public HostSim getHost(String name) {
		HostSim host = hostMap.get(name);
		if(host != null) {
			return host;
		}
		throw new Error(name + " is not Host");
	}

	public boolean isHost(String name) {
		return hostMap.containsKey(name);
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