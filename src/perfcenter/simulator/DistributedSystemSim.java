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

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;

import org.apache.log4j.Logger;

import perfcenter.baseclass.enums.MigrationTechnique;
import perfcenter.baseclass.Device;
import perfcenter.baseclass.DistributedSystem;
import perfcenter.baseclass.Helper;
import perfcenter.baseclass.Machine;
import perfcenter.baseclass.PhysicalMachine;
import perfcenter.baseclass.LanLink;
import perfcenter.baseclass.Metric;
import perfcenter.baseclass.MigrationPolicyInfo;
import perfcenter.baseclass.ModelParameters;
import perfcenter.baseclass.TaskNode;
import perfcenter.baseclass.Scenario;
import perfcenter.baseclass.SoftServer;
import perfcenter.baseclass.Task;
import perfcenter.simulator.metric.ManuallyComputedMetric;
import perfcenter.simulator.metric.MetricSim;
import perfcenter.simulator.metric.TimeAverageMetric;
import perfcenter.simulator.queue.QueueSim;
import perfcenter.simulator.virtualization.MigrationPolicy;

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
	
	public MigrationPolicy migrationPolicy;
	
	public HashMap<String, Boolean> migratedServers = new HashMap<String, Boolean>();
		
	public boolean serverMigrated(String servername){
		if(migratedServers == null){
			return false;
		}
		return migratedServers.containsKey(servername);
	}

	
	public ManuallyComputedMetric overallGoodputSim = new ManuallyComputedMetric(0.95); //FIXME remove hardcoding
	public ManuallyComputedMetric overallBadputSim = new ManuallyComputedMetric(0.95); //FIXME remove hardcoding
	public ManuallyComputedMetric overallThroughputSim = new ManuallyComputedMetric(0.95); //FIXME remove hardcoding
	public ManuallyComputedMetric overallDroprateSim = new ManuallyComputedMetric(0.95); //FIXME remove hardcoding
	public ManuallyComputedMetric overallBuffTimeoutSim = new ManuallyComputedMetric(0.95); //FIXME remove hardcoding
	public ManuallyComputedMetric overallRespTimeSim = new ManuallyComputedMetric(0.95); //FIXME remove hardcoding
	public ManuallyComputedMetric overallBlockingProbSim = new ManuallyComputedMetric(0.95); //FIXME remove hardcoding

	public HashMap<String, Double> vmDownTimeMap = new HashMap<String, Double>();
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
		
		for(PhysicalMachine pm : ds.pms.values()){
			for(String vmname : pm.vservers.keySet()){
				vmDownTimeMap.put(vmname, new Double(0.0));
			}
		}
		// this need not be done if its use in perfanalytic is fixed
		for (SoftServer s : softServers.values()) {
			for (Task t : s.tasks) {
				t.initialize();
			}
		}
		for (Scenario sce : ds.scenarios.values()) {
			ScenarioSim s = new ScenarioSim(sce);
			scenarios.put(s.getName(), s);
			scenarioMap.put(s.getName(), s);
		}
		
		if(ds.migrationPolicyInfo != null){
			migrationPolicy = loadMigrationPolicyClass(ds.migrationPolicyInfo);
		}

	}
	
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public MigrationPolicy loadMigrationPolicyClass(MigrationPolicyInfo policyinfo) {
		MigrationPolicy policy = null;
		policyinfo.print();
		try {
			Class c = Class.forName("perfcenter.simulator.virtualization." + policyinfo.type.toString());

			Class[] proto = new Class[4];
			proto[0] = Double.class;
			proto[1] = String.class;
			proto[2] = String.class;
			proto[3] = MigrationTechnique.class;

			Object[] params = new Object[4];
			params[0] = policyinfo.policyarg;
			params[1] = policyinfo.vmname;
			params[2] = policyinfo.destpmname;
			params[3] = policyinfo.technique;

			Constructor cons = c.getConstructor(proto);
			policy = (MigrationPolicy) cons.newInstance(params);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return policy;
	}


	// clear all the variables but keep the confidence interval calculations
	public void clearValuesButKeepConfIvals() {
		for (Machine m : pms.values()) {
			((PhysicalMachineSim)m).clearValuesButKeepConfIvals();
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
				((SoftServerSim)softServer).computeConfIvalsAtEndOfRepl();
			}
			for (Device device : m.devices.values()) {
				/* RAM device won't have actual contention, no point of computing usual performance measures */
				if (device.name.equals("ram")) {
					continue;
				}
				((DeviceSim) device).avgFreqSim.computeConfIvalsAtEndOfRepl();
				DistributedSystemSim.computeConfIvalForMetric(device.averageFrequency, ((DeviceSim) device).avgFreqSim);
				
				((QueueSim) ((DeviceSim) device).resourceQueue).computeConfIvalsAtEndOfRepl();
			}
			((PhysicalMachineSim)m).computeConfIvalsAtEndOfRepl();
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
			((PhysicalMachineSim)m).recordCISampleAtTheEndOfSimulation();
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
			//System.out.println(slot + " : " + metric.getConfidenceInterval(slot)  + " : " + metricSim.getCI(slot));
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
	public double migrate1(String vmname, String destpmname){
		return 0.0;
	}
	/* This method should check feasibility of migration through following checks
	 * 1) Whether physical machine of vm and destination physical machine are different
	 * 2) Device categories of virtual machines should be supported by destination physical machine
	 * 3) It should check whether sufficient memory is available on destination host or not. This is currently not 
	 *    implemented.
	 */
	public boolean checkFeasibilityOfMigration(String vmname, String destpmname){
		String srcpmname = ModelParameters.inputDistSys.getActualHostName(vmname);
		PhysicalMachineSim srcpm = SimulationParameters.distributedSystemSim.machineMap.get(srcpmname);
		PhysicalMachineSim destpm = SimulationParameters.distributedSystemSim.machineMap.get(destpmname);
		if(destpm.vservers.containsKey(vmname)){
			System.err.println("No need to migrate. \"" + vmname + "\" is already deployed on \"" + destpmname + "\"");
			return false;
		}
		HashSet<String> srcdevcats = new HashSet<String>();
		HashSet<String> destdevcats = new HashSet<String>();
		for(Device device : ModelParameters.inputDistSys.vms.get(vmname).devices.values()){
			srcdevcats.add(device.category.name);
		}
		for(Device device : destpm.deviceMap.values()){
			destdevcats.add(device.category.name);
		}
		for(String srcdevcat : srcdevcats){
			if(!destdevcats.contains(srcdevcat)){
				System.err.println("Migration Feasibility failed. VM's devicecagory \"" + srcdevcat + "\" can't be accmmodated on \"" + destpmname);
				return false;
			}
		}
		return true;
	}
	
	/* Migrates a vm from its host to destination host.
	 * This method achieves this by updating data structures related to physical machines, software servers, their tasks and scenarios 
	 */
	public double migrate(String vmname, String destpmname){
		String srcpmname = ModelParameters.inputDistSys.getActualHostName(vmname);
		PhysicalMachineSim srcpm = machineMap.get(srcpmname);
		PhysicalMachineSim destpm = machineMap.get(destpmname);
		double downtime = 0.0;
		ArrayList<String> origTasks = new ArrayList<String>();
		
		/* Move newly created vcpu servers from source physical machine to destination physical machine */
		for(SoftServer srvr : srcpm.vservers.get(vmname)){
			downtime += migrateServer((SoftServerSim)srvr, srcpm, destpm);
			migratedServers.put(srvr.name, true);
			if(isLink(srcpm.lan, destpm.lan)){
				LanLink lnk = getLink(srcpm.lan, destpm.lan);
				double srvrdt = migrationPolicy.computeDownTime((SoftServerSim)srvr, Helper.convertTobps(lnk.trans.getValue(), lnk.transUnit));
				System.out.println("Server name:" + srvr.name + ":downtime:" + srvrdt);
				downtime += srvrdt;
			}
		}
		
		/* Move servers actually deployed on virtual machine from source physical machine to destination physical machine */
		for(SoftServer srvr : srcpm.serversDeployedOnVm.get(vmname)){
			downtime += migrateServer((SoftServerSim)srvr, srcpm, destpm);
			migratedServers.put(srvr.name, true);
			for(Task task : ModelParameters.inputDistSys.softServers.get(srvr.name).tasks){
				origTasks.add(task.name);
			}
			if(isLink(srcpm.lan, destpm.lan)){
				LanLink lnk = getLink(srcpm.lan, destpm.lan);
				double srvrdt = migrationPolicy.computeDownTime((SoftServerSim)srvr, Helper.convertTobps(lnk.trans.getValue(), lnk.transUnit));
				System.out.println("Server name:" + srvr.name + ":downtime:" + srvrdt);
				downtime += srvrdt;
			}
		}
		
		
		/* vservers and serversDeployedOnVm just contain list of references to actual softservers for each vm. 
		 * Removing them from source pm and adding to destination pm 
		 */
		if(destpm.vservers == null){
			destpm.vservers = new HashMap<String, ArrayList<SoftServer> >();
		}
		ArrayList<SoftServer> tmp = srcpm.vservers.get(vmname);
		srcpm.vservers.remove(vmname);
		destpm.vservers.put(vmname, tmp);
		
		
		if(destpm.serversDeployedOnVm == null){
			destpm.serversDeployedOnVm = new HashMap<String, ArrayList<SoftServer> >();
		}
		tmp = srcpm.serversDeployedOnVm.get(vmname);
		srcpm.serversDeployedOnVm.remove(vmname);
		destpm.serversDeployedOnVm.put(vmname, tmp);
		
		/* Updating appropriate Data structures related to Ram utilization in source pm and destination pm */
		if(destpm.ramUtilSim == null){
			destpm.ramUtilSim = new TimeAverageMetric(0.95);
		}
		
		
		if(destpm.vmRamUtilSim == null){
			destpm.vmRamUtilSim = new HashMap<String, TimeAverageMetric>();
		}
		destpm.vmRamUtilSim.put(vmname, srcpm.vmRamUtilSim.get(vmname));
		srcpm.vmRamUtilSim.remove(vmname);
		
		if(destpm.currVmRamUtil == null){
			destpm.currVmRamUtil = new HashMap<String, Double>();
		}
		
		if(destpm.avgRamUtil == null){
			destpm.avgRamUtil = new Metric();
		}
		if(destpm.avgVmRamUtils == null){
			destpm.avgVmRamUtils = new HashMap<String, Metric>();
		}
		destpm.avgVmRamUtils.put(vmname, srcpm.avgVmRamUtils.get(vmname));
		srcpm.avgVmRamUtils.remove(vmname);
		
		/* Hypervisor server will not move. 
		 * But tasks attached to them are actually related to moving servers, so they need to be changed
		 */
		HashMap<String, Task> hvTasksToBeModified = new HashMap<String, Task>();
		SoftServer fromHypervisor = softServerMap.get(srcpm.getHypervisorName());
		SoftServer toHypervisor = softServerMap.get(destpm.getHypervisorName());
		
		ArrayList<Integer> toBeDeletedTasksFromOrigHv = new ArrayList<Integer>();
		for(int i=0;i<fromHypervisor.tasks.size();i++){
			Task hvTask = fromHypervisor.tasks.get(i);
			for(String origtaskname : origTasks){
				if(hvTask.name.contains(origtaskname)){
					hvTasksToBeModified.put(hvTask.name, hvTask);
					toBeDeletedTasksFromOrigHv.add(i);
					break;
				}
			}
		}
		
		for(Integer i : toBeDeletedTasksFromOrigHv){
			fromHypervisor.tasks.remove(i);
		}
		
		for(Task task : hvTasksToBeModified.values()){
			task.softServerName = toHypervisor.name;
			toHypervisor.tasks.add(task);
		}
		
		/* Tasks and servers are migrated. Scenarios remain same except one change. 
		 * (Hypervisor)Server name of network overhead tasknode in scenario needs to be computed
		 */
		for(Scenario scenario : SimulationParameters.distributedSystemSim.scenarioMap.values()){
			
			/* Level traversal */
			ArrayList<TaskNode> level = new ArrayList<TaskNode>();
			level.add(scenario.rootNode);
			while(!level.isEmpty()){
				ArrayList<TaskNode> nxtlevel = new ArrayList<TaskNode>();
				for(int i=0;i<level.size();i++){
					TaskNode node = level.get(i);
					if(hvTasksToBeModified.containsKey(node.name)){
						node.servername = toHypervisor.name;
					}
					for(TaskNode child : node.children){
						nxtlevel.add(child);
					}
				}
				level = nxtlevel;
			}
		}
		
		vmDownTimeMap.put(vmname, downtime);
		return downtime;
	}
	
	/* This method moves a softserver from source pm to destiantion pm
	 */
	public double migrateServer(SoftServerSim srvr, PhysicalMachineSim srcpm, PhysicalMachineSim destpm){
		double downtime = 0.0;
		
		srcpm.softServerMap.remove(srvr.name);
		destpm.softServerMap.put(srvr.name, srvr);
		
		srcpm.softServers.remove(srvr.name);
		destpm.softServers.put(srvr.name, (SoftServer)srvr);
		
		for(int i=0;i<srvr.machines.size();i++){
			if(srvr.machines.get(i).compareTo(srcpm.name) == 0){
				srvr.machines.remove(i);
				break;
			}
		}
		srvr.machines.add(destpm.name);
		
		for(int i=0;i<srvr.hostObjects.size();i++){
			if(srvr.hostObjects.get(i).name.compareTo(srcpm.name) == 0){
				srvr.hostObjects.remove(i);
				break;
			}
		}
		srvr.hostObjects.add(destpm);
		
		return downtime;
	}
}
