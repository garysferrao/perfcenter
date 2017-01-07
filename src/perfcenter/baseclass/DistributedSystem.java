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
/*
 * This is the main class that has complete representation of input file
 * It has functions to print,validate,modify  the resources defined.
 */
package perfcenter.baseclass;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Stack;
import java.util.Random;
import java.util.Set;

import perfcenter.baseclass.VirtModelParameters;
import perfcenter.baseclass.enums.DeviceType;
import perfcenter.baseclass.DeviceCategory;

import org.apache.log4j.FileAppender;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;

/**
 * This is the complete representation of the input file. Parser fills in this
 * structure.
 * 
 * @author akhila
 */
public class DistributedSystem {

	// List of soft resources. used for reference only. they are deployed on
	// machines
	public HashMap<String, SoftResource> softRes; // list of soft resources
	public HashMap<String, DeviceCategory> devcats;
	public HashMap<String, PhysicalDevice> pdevices;
	public HashMap<String, VirtualDevice> vdevices;
	public HashMap<String, Variable> variables;
	public HashMap<String, Task> tasks;
	/**
	 * List of softServers. used for reference only. They are deployed on
	 * Machines
	 */
	public HashMap<String, SoftServer> softServers;
	/**
	 * All the Machines. For simulation, this would contain objects of
	 * MachineSim.
	 */
	public HashMap<String, PhysicalMachine> pms;
	public HashMap<String, VirtualMachine> vms;
	/**
	 * All the Scenarios. For simulation, this would contain objects of
	 * ScenarioSim.
	 */
	public HashMap<String, Scenario> scenarios;
	public HashMap<String, Lan> lans;
	/**
	 * All the LanLinks. For simulation, this would contain objects of
	 * LanLinkSim.
	 */
	public HashMap<String, LanLink> links;
	
	/* Following two variables are used for virtperfcenter */
	private boolean isTransformed; 
	public HashMap<String, ArrayList<SoftServer> > vservers;

	// parameters across all scenarios
	public Metric overallRespTime = new Metric();
	public Metric overallThroughput = new Metric();
	public Metric overallBadput = new Metric();
	public Metric overallGoodput = new Metric();
	public Metric overallBuffTimeout = new Metric();
	public Metric overallDroprate = new Metric();
	public Metric overallArrivalRate = new Metric();
	public Metric overallBlockProb = new Metric();

	// this list contains different device types and used to copy attributes
	// from this device to other devices of same type: rakesh
	public HashMap<String, PhysicalDevice> powerManagedDevicePrototypes;
	// list of power managed devices
	public HashMap<String, PhysicalDevice> powerManagedDevices;

	public Random random;

	public MigrationPolicyInfo migrationPolicyInfo;
	
	public DistributedSystem() {
		pms = new HashMap<String, PhysicalMachine>();
		vms = new HashMap<String, VirtualMachine>();
		softRes = new HashMap<String, SoftResource>();
		
		devcats = new HashMap<String, DeviceCategory>();
		DeviceCategory ramdevcat = new DeviceCategory("ram", DeviceType.RAM);
		devcats.put(ramdevcat.name, ramdevcat);
		
		pdevices = new HashMap<String, PhysicalDevice>();
		PhysicalDevice pdevram = new PhysicalDevice("ram",ramdevcat);
		pdevices.put(pdevram.name, pdevram);
		
		vdevices = new HashMap<String, VirtualDevice>();
		VirtualDevice vdevram = new VirtualDevice("ram",ramdevcat);
		vdevices.put(vdevram.name, vdevram);
		
		isTransformed = false;
		vservers = new HashMap<String, ArrayList<SoftServer> >();

		/***************************************************************************/
		powerManagedDevicePrototypes = new HashMap<String, PhysicalDevice>(); // added
																				// by
																				// Rakesh
		powerManagedDevices = new HashMap<String, PhysicalDevice>(); // added by
																		// Rakesh
		/***************************************************************************/
		variables = new HashMap<String, Variable>();
		tasks = new HashMap<String, Task>();
		softServers = new HashMap<String, SoftServer>();
		scenarios = new HashMap<String, Scenario>();
		lans = new HashMap<String, Lan>();
		links = new HashMap<String, LanLink>();

		random = new Random();
		// This is done to have a dummy task "user" at the start and end of a
		// scenario
		Task t = new Task("user", 0);

		SoftServer sdummy = new SoftServer("user");
		sdummy.addTask(t);
		// sdummy.addHost("dummy");
		softServers.put("user", sdummy);

		t.softServerName = "user";
		tasks.put("user", t);

		// why these things are here? they are local variables which are being
		// used nowhere else
		// Host hdummy = new Host("dummy", 1);
		// hdummy.addServer(sdummy);
		// End dummy task
	}

	public void printConfiguration() {
		System.out.println("---printcfg start----");
		for (Machine machine : pms.values()) {
			System.out.println("---MachineDef----");
			machine.print();
		}
		System.out.println("---printcfg end----");
	}

	public Task getTask(String name) {
		if (tasks.containsKey(name.toLowerCase())) {
			return tasks.get(name.toLowerCase());
		}
		throw new Error(name + " is not Task");
	}

	public LanLink getLink(String name1, String name2) {
		for (LanLink link : links.values()) {
			if (link.srclan.compareToIgnoreCase(name1) == 0) {
				if (link.destlan.compareToIgnoreCase(name2) == 0) {
					return link;
				}
			}
			if (link.srclan.compareToIgnoreCase(name2) == 0) {
				if (link.destlan.compareToIgnoreCase(name1) == 0) {
					return link;
				}
			}
		}
		throw new Error(name1 + " " + name2 + "is not Link");
	}
	
	public void setMigrationPolicyInfo(MigrationPolicyInfo policy){
		migrationPolicyInfo = policy;
	}

	public LanLink getLink(String name1) {
		if (links.containsKey(name1.toLowerCase())) {
			return links.get(name1.toLowerCase());
		}
		throw new Error(name1 + "is not defined as Link");
	}

	public boolean isLink(String name1) {
		return links.containsKey(name1.toLowerCase());
	}

	public boolean isLink(String name1, String name2) {
		for (LanLink link : links.values()) {
			if (link.srclan.compareToIgnoreCase(name1) == 0) {
				if (link.destlan.compareToIgnoreCase(name2) == 0) {
					return true;
				}
			}
			if (link.srclan.compareToIgnoreCase(name2) == 0) {
				if (link.destlan.compareToIgnoreCase(name1) == 0) {
					return true;
				}
			}
		}
		return false;
	}

	public boolean isTask(String name) {
		return tasks.containsKey(name.toLowerCase());
	}

	public SoftServer getServer(String name) {
		if (softServers.containsKey(name.toLowerCase())) {
			return softServers.get(name.toLowerCase());
		}
		throw new Error(name + " is not Server");
	}

	public boolean isServer(String name) {
		return softServers.containsKey(name.toLowerCase());
	}

	public PhysicalMachine getPM(String pmname) {
		if (pms.containsKey(pmname.toLowerCase())) {
			return pms.get(pmname.toLowerCase());
		}
		throw new Error(pmname + " is not Machine");
	}

	public VirtualMachine getVM(String vmname) {
		if (vms.containsKey(vmname.toLowerCase())) {
			return vms.get(vmname.toLowerCase());
		}
		throw new Error(vmname + " is not Virtual Machine");
	}

	public boolean isPM(String pmname) {
		return pms.containsKey(pmname.toLowerCase());
	}

	public boolean isVM(String vmname) {
		if(isTransformed){
			return vservers.containsKey(vmname);
		}else{
			return vms.containsKey(vmname.toLowerCase());
		}
	}

	public DeviceCategory getDeviceCategory(String devcatname) {
		if (devcats.containsKey(devcatname.toLowerCase())) {
			return devcats.get(devcatname.toLowerCase());
		}
		throw new Error(devcatname + " is not Device Category");
	}

	public PhysicalDevice getPDevice(String devname) {
		if (pdevices.containsKey(devname.toLowerCase())) {
			return pdevices.get(devname.toLowerCase());
		}
		throw new Error(devname + " is not Physical Device");
	}

	public VirtualDevice getVDevice(String vdevname) {
		if (vdevices.containsKey(vdevname.toLowerCase())) {
			return vdevices.get(vdevname.toLowerCase());
		}
		throw new Error(vdevname + " is not Virtual Device");
	}

	public DeviceType getDeviceType(String devcatname) {
		if (devcats.containsKey(devcatname.toLowerCase())) {
			return devcats.get(devcatname.toLowerCase()).type;
		}
		throw new Error(devcatname + " is not Device Category");
	}

	public DeviceCategory getVDeviceCategory(String vdevname) {
		if (vdevices.containsKey(vdevname.toLowerCase())) {
			return vdevices.get(vdevname.toLowerCase()).category;
		}
		throw new Error(vdevname + " is not Device");
	}

	public boolean isDeviceCategory(String devcatname) {
		return devcats.containsKey(devcatname);
	}

	public boolean isPDevice(String pdevname) {
		return pdevices.containsKey(pdevname.toLowerCase());
	}

	public boolean isVDevice(String vdevname) {
		return vdevices.containsKey(vdevname.toLowerCase());
	}

	public SoftResource getSoftRes(String name) {
		if (softRes.containsKey(name.toLowerCase())) {
			return softRes.get(name.toLowerCase());
		}
		throw new Error(name + " is not soft resource");
	}

	public boolean isSoftRes(String name) {
		return softRes.containsKey(name.toLowerCase());
	}

	public Scenario getScenario(String name) {
		if (scenarios.containsKey(name.toLowerCase())) {
			return scenarios.get(name.toLowerCase());
		}
		throw new Error(name + " is not Scenario");
	}

	public boolean isScenario(String name) {
		return scenarios.containsKey(name.toLowerCase());
	}

	public Variable getVariable(String name) {
		if (variables.containsKey(name.toLowerCase())) {
			return variables.get(name.toLowerCase());
		}
		throw new Error(name + " is not Variable");
	}

	public boolean isVariable(String name) {
		return variables.containsKey(name.toLowerCase());
	}

	public Lan getLan(String name) {
		if (lans.containsKey(name.toLowerCase())) {
			return lans.get(name.toLowerCase());
		}
		throw new Error(name + " is not Lan");
	}

	public boolean isLan(String name) {
		return lans.containsKey(name.toLowerCase());
	}

	// CHECK : This method is not used anywhere
	// deploying server on machine or machine on lan
	public void deploy(String name1, String name2) {
		SoftServer srv;
		Machine machine;
		// deploying server on machine
		if (isServer(name1)) {
			srv = getServer(name1);
			machine = getPM(name2);
			srv.addMachine(name2);
			machine.addServer(srv);
			return;
		} else {
			// deploying machine on lan
			machine = getPM(name1);
			Lan ln = getLan(name2);
			machine.addLan(name2);
			ln.addMachine(name1);
		}
	}

	public void validate() {
		for (Task task : tasks.values()) {
			task.validate();
		}
		for (SoftServer serv : softServers.values()) {
			serv.validate();
		}
		for (Machine machine : pms.values()) {
			machine.validate();
		}
		for (Lan lan : lans.values()) {
			lan.validate();
		}
		for (Variable var : variables.values()) {
			var.validate();
		}
		
		for(Scenario sce : scenarios.values()){
			sce.validate();
			sce.isValidated = true;
		}
	}

	// check that sum of all scenario prob is one
	public void checkParameters() throws IOException {
		double total_prob = 0;
		for (Scenario sc : ModelParameters.inputDistSys.scenarios.values()) {
			total_prob += sc.getProbability();
		}
		if (total_prob != 1) {
			throw new Error("Sum of scenario probability is not equal to 1");
		}

		if (ModelParameters.outputFileStr.length() > 0) {
			FileAppender fa = new FileAppender(new PatternLayout(), ModelParameters.outputFileStr, false);
			Logger l = Logger.getLogger("DistributedSystem");
			l.addAppender(fa);
		}
	}

	public void print(){
		System.out.println("====Distributed System====");
		System.out.println("===PhysicalDevices===");
		for(PhysicalDevice pd : pdevices.values()){
			System.out.println("\t" + pd.name);
		}
		System.out.println("===VirtualDevices===");
		for(VirtualDevice vd : vdevices.values()){
			System.out.println("\t" + vd.name);
		}
		System.out.println("===PhysicalMachines===");
		for(PhysicalMachine pm : pms.values()){
			System.out.println("\t" + pm.name);
			System.out.print("Servers: ");
			for(SoftServer server : pm.softServers.values()){
				System.out.print(server.name + " ");
			}
			System.out.println();
		}
		
		System.out.println("===VirtualMachines===");
		for(VirtualMachine vm : vms.values()){
			System.out.print("\t" + vm.name);
			if(vm.host == null){
				System.out.println(" not deployed");
			}else{
				System.out.println(" deployed on " + vm.host.name);
			}
		}
		System.out.println("===Tasks===");
		for(Task task : tasks.values()){
			System.out.println("\t" + task.name);
			for(int i=0;i<task.subtaskServiceTimes.size();i++){
				ServiceTime st = task.subtaskServiceTimes.get(i);
				System.out.println("\t\t" + st.devCategory.name + " servt " + st.dist.name_ + "(" + st.dist.value1_.getName() + ":" + st.dist.value1_.getValue() + ") at " + st.basespeed);
			}
		}
		System.out.println("===SoftServers===");
		for(SoftServer server : softServers.values()){
			System.out.print("\t" + server.name);
			if(server.machines.size() > 0)
				System.out.println(" deployed on " + server.machines.get(0) + " size:" + server.machines.size());
			else
				System.out.println(" not deployed");
			System.out.print("\t\tTasks:");
			for(Task task : server.tasks)
			{
				System.out.print(task.name + ", ");
			}
			System.out.println();
		}
		System.out.println("======Scenarios=====");
		for(Scenario scenario : scenarios.values()){
			scenario.print();
		}
	}
	
	private boolean isDeployedOnVm(String sname) {
		SoftServer server = getServer(sname);
		if(server == null || server.name == "user"){
			return false;
		}
		
		if(server.machines.size() == 0){
			throw new Error("ERROR:SoftServer " + server.name + " is not deployed");
		}
		String mname = server.machines.get(0); // TRANSCHECK: What if there softServer is deployed on more than one machines. Right
												// now assumption is that it is deployed on only on machine
		if (vms.containsKey(mname)) {
			return true;
		}
		/* If not depoyed on VM, then it should be deployed on Physical Machine
		 */
		return false;
	}
	
	private boolean isDeployedOnVm(SoftServer origServer, HashMap<String, VirtualMachine> origVms) {
		if(origServer == null || origServer.name == "user"){
			return false;
		}
		
		if(origServer.machines.size() == 0){
			throw new Error("ERROR:SoftServer " + origServer.name + " is not deployed");
		}
		String mname = origServer.machines.get(0); // TRANSCHECK: What if there softServer is deployed on more than one machines. Right
												// now assumption is that it is deployed on only on machine
		if (origVms.containsKey(mname)) {
			return true;
		}
		/* If not depoyed on VM, then it should be deployed on Physical Machine
		 */
		return false;
	}
	
	
	/* In the case of nested virtualization, this method finds actual physical machine on which stack of vm is deployed 
	 */
	public String getActualHostName(String vmname){
		VirtualMachine vm = ModelParameters.inputDistSys.getVM(vmname);
		while(ModelParameters.inputDistSys.vms.containsKey(vm.host.name)){
			vm = ModelParameters.inputDistSys.getVM(vm.host.name);
		}
		if(!ModelParameters.inputDistSys.pms.containsKey(vm.host.name)){
			throw new Error("Virtual Machine " + vm.name + " is deployed on " + vm.host.name + " instead of physical machine\n");
		}
		return vm.host.name;
	}
	
	
	public String getActualHostName(String vmname, HashMap<String, VirtualMachine> origVms, HashMap<String, PhysicalMachine> transformedPms){
		VirtualMachine vm = origVms.get(vmname);
		while(origVms.containsKey(vm.host.name)){
			vm = ModelParameters.inputDistSys.getVM(vm.host.name);
		}
		if(!transformedPms.containsKey(vm.host.name)){
			throw new Error("Virtual Machine " + vm.name + " is deployed on " + vm.host.name + " instead of physical machine\n");
		}
		return vm.host.name;
	}
	
	public String getActualHostName(SoftServer srvr){
		if(!ModelParameters.inputDistSys.vms.containsKey(srvr.machines.get(0))){
			return srvr.machines.get(0);
		}
		return getActualHostName(srvr.machines.get(0));
	}
	
	/* A VM can be deployed on another vm. There can be long nested virtualization
	 * This method finds the last vm in the stack. It is directly deployed on physical machine 
	 */
	private String getLowestVm(String vmname){
		String finalvmname = vmname;
		/* If vm is deployed on another vm, then find out vm which is directly deployed on Physical machine */
		while(true){
			VirtualMachine vm = ModelParameters.inputDistSys.getVM(finalvmname);
			if(vm.host != null && ModelParameters.inputDistSys.vms.containsKey(vm.host.name)){
				finalvmname = vm.host.name;
			}else{
				break;
			}
		}
		return finalvmname;
	}

	private String pickVServer(String vmname) {
		ArrayList<SoftServer> vservlist = vservers.get(vmname);
		return vservlist.get(random.nextInt(vservlist.size())).name;
	}
	
	private String pickVServer(String vmname, HashMap<String, ArrayList<SoftServer>> _vservers) {
		ArrayList<SoftServer> vservlist = _vservers.get(vmname);
		return _vservers.get(vmname).get(random.nextInt(vservlist.size())).name;
	}
	
	/* This function is exclusively used in transform() method. Has no utility outside it.
	 * It copies original TaskNode attributes to its every TaskNode of resultant equivalent sequence 
	 * First and last TaskNode are given of sequence. 
	 * It is assumed that every TaskNode in sequence has only one element in their member variable named children.
	 * To copy original tasknode attributes to single tasknode, keep toStartNode and toEndNode same
	 */
	private void copyNodeInfo(TaskNode from, TaskNode toStartNode, TaskNode toEndNode){
		/*System.out.print("********copyNodeInfo(" + from.name + "-prob:" + from.prob.value +  ",");
		System.out.print(toStartNode.name + ",");
		System.out.print(toEndNode.name + ")*******"); */  
		toStartNode.pktsize = from.pktsize;
		toStartNode.arrate = from.arrate;
		toStartNode.belongsToCT = from.belongsToCT;
		toStartNode.isCT = from.isCT;
		toStartNode.isRoot = from.isRoot;
		toStartNode.prob = new Variable(from.prob.name, from.prob.value);
		TaskNode temp = toStartNode;
		TaskNode prev = null;
		/* This is sort of hack to set calls of all the dummy start tasks to synchronous */ 
		boolean nextsync = false;
		if(temp.name.indexOf("_start") != -1){
			nextsync = true;
		}
		while(temp != toEndNode){
			if(temp.children.size() == 0){
				throw new Error("TRANSFORMATION ERROR: " + temp.name + " has no children in copyNode() ");
			}
			prev = temp;
			temp = temp.children.get(0);
			temp.pktsize = from.pktsize;
			temp.arrate = from.arrate;
			temp.belongsToCT = from.belongsToCT;
			temp.isCT = from.isCT;
			temp.isRoot = false;
			temp.parent = prev;
			temp.prob = new Variable(from.prob.name, from.prob.value);
			temp.issync = nextsync;
			if(temp.name.indexOf("_start") != -1){
				nextsync = true;
			}else{
				nextsync = false;
			}
		}
	}
	
	public String getHypervisorName(String srvrname){
		SoftServer server = ModelParameters.inputDistSys.getServer(srvrname);
		String vmname = server.machines.get(0);											//TRANSCHECK
		String pmname  = getActualHostName(vmname);
		String hvname = pmname.replaceAll("\\[", "").replaceAll("\\]", "") + "_hypervisor";
		return hvname;
	}
	
	/*
	 * 
	 */
	public String getHypervisorName(SoftServer origServer, HashMap<String, VirtualMachine> origVms, HashMap<String, PhysicalMachine> origPms){
		String vmname = origServer.machines.get(0);											//TRANSCHECK
		String pmname  = getActualHostName(vmname, origVms, origPms);
		String hvname = pmname.replaceAll("\\[", "").replaceAll("\\]", "") + "_hypervisor";
		return hvname;
	}
	
	
	/* This method is exclusively used in transformation.
	 * It is used when generating equivalent tasknode corresponding to task in list of new tasks.
	 * It returns pair of first tasknode and last tasknode as ArrayList
	 */
	public ArrayList<TaskNode> buildStartToEndTaskNode(ArrayList<Task> newEqTaskList){
		TaskNode startNode = new TaskNode(newEqTaskList.get(0).name);
		startNode.servername = newEqTaskList.get(0).softServerName;
		startNode.parent = null;
		TaskNode prevNode = startNode;
		for (int i=1; i<newEqTaskList.size(); i++) {
			TaskNode currNode = new TaskNode(newEqTaskList.get(i).name);
			currNode.servername = newEqTaskList.get(i).softServerName;
			currNode.parent = prevNode;
			prevNode.children.add(currNode);
			prevNode = currNode;
		}
		ArrayList<TaskNode> newlist = new ArrayList<TaskNode>();
		newlist.add(startNode);
		newlist.add(prevNode);
		return newlist;
	}
	
	/* Virtualization is supported with some limitations. Following method validates input distributed system for those limiatations */
	public void validateForTransformation(){
		for(Task task : ModelParameters.inputDistSys.tasks.values()){
			if(task.name.indexOf("_start") != -1){
				throw new Error("Please choose modify name of task \"" + task.name + "\". Currently \"_start\" is not allowed as part of task name\n");
			}
		}
		for(SoftServer server : ModelParameters.inputDistSys.softServers.values()){
			if(server.machines.size() > 1){
				throw new Error("Please deploy software server \"" + server.name + "\" on one pm/vm only. Support for multiple deployment of single server is not added yet\n");
			}
		}
	}

	public DistributedSystem transform() throws IOException{
		validateForTransformation();
		DistributedSystem transformedDistSys = new DistributedSystem();

		/*
		 * Device Category specification will remain same in new transformed
		 * system
		 */
		for (DeviceCategory devcat : this.devcats.values()) {
			transformedDistSys.devcats.put(devcat.name, devcat.getCopy());
		}
		/*
		 * Physical Device specification will remain same in new transformed
		 * system
		 */
		for (PhysicalDevice pdev : this.pdevices.values()) {
			transformedDistSys.pdevices.put(pdev.name, pdev.getCopy());
		}

		/* Lan specification will remain same in new transformed system */
		for (Lan lan : this.lans.values()) {
			transformedDistSys.lans.put(lan.name, lan.getCopy());
		}
		
		for (LanLink link : this.links.values()) {
			transformedDistSys.links.put(link.name, link);
		}
		
		//TRANSCHECK
		for (SoftResource sr : this.softRes.values()) {
			transformedDistSys.softRes.put(sr.name, sr.getCopy());
		}

		for(PhysicalDevice powerManagedDevicePrototype: this.powerManagedDevicePrototypes.values()){
			transformedDistSys.powerManagedDevicePrototypes.put(powerManagedDevicePrototype.name, powerManagedDevicePrototype.getCopy());
		}
		
		for(PhysicalDevice powerManagedDevice: this.powerManagedDevices.values()){
			transformedDistSys.powerManagedDevicePrototypes.put(powerManagedDevice.name, powerManagedDevice.getCopy());
		}
		
		for(Variable variable: variables.values()){
			transformedDistSys.variables.put(variable.name, new Variable(variable.name, variable.value));
		}

		/* All Physical Machines will remain same in new transformed system */
		for (PhysicalMachine pm : this.pms.values()) {
			transformedDistSys.pms.put(pm.name, pm.getCopy());
			transformedDistSys.getPM(pm.name).virtualizationEnabled = false;
		}

		/*
		 * Initialize softServers with inputDistSys' softServers 
		 * If server is deployed on physical machine, tasks in transformed system will be same.
		 * In case of virtual machine, they will be modified completely and now set them to empty arraylist
		 */
		for (SoftServer server : this.softServers.values()) {
			transformedDistSys.softServers.put(server.name.toLowerCase(), server.getCopy());
			if (isDeployedOnVm(server.name)) {
				SoftServer srvr = transformedDistSys.getServer(server.name);
				srvr.tasks = new ArrayList<Task>();
				String vmname = srvr.machines.get(0);
				String pmname = getActualHostName(vmname);
				srvr.machines.set(0, pmname);
				transformedDistSys.pms.get(pmname).softServers.put(srvr.name, srvr);
			}
		}

		/*
		 * For each PM with support for virtualization, create hypervisor as softServer
		 */
		for (PhysicalMachine pm : this.pms.values()) {
			if (pm.virtualizationEnabled) {
				SoftServer hypervisor = new SoftServer();
				hypervisor.name = pm.getHypervisorName();
				hypervisor.size.setValue(VirtModelParameters.hypervisorStaticSize);
				hypervisor.threadSize.setValue(VirtModelParameters.hypervisorThreadSize);
				hypervisor.thrdCount.setValue(VirtModelParameters.hypervisorThreadCount);
				hypervisor.thrdBuffer.setValue(VirtModelParameters.hypervisorThreadBufferSize);
				hypervisor.schedp = VirtModelParameters.hypervisorSchedP;
				hypervisor.machines.add(pm.name);
				transformedDistSys.pms.get(pm.name).softServers.put(hypervisor.name, hypervisor);
				transformedDistSys.softServers.put(hypervisor.name.toLowerCase(), hypervisor);

				/* For hypervisor server, create its network_task */
				Task network_task = new Task("network_call_" + hypervisor.name, -1);
				DeviceCategory cpudevcat = pm.getCpuDevCategory();
				Distribution constDist = new Distribution(VirtModelParameters.nwOverheadDist, VirtModelParameters.networkingOverhead); 
				ServiceTime st = new ServiceTime(cpudevcat, constDist, pm.getDeviceBaseSpeed(cpudevcat));
				network_task.subtaskServiceTimes.add(st);
				network_task.softServerName = hypervisor.name;
				transformedDistSys.tasks.put(network_task.name, network_task);
				hypervisor.tasks.add(network_task);
			}
		}

		/* For each CPUTYPE device in VM, create corresponding soft server */
		for (VirtualMachine vm : this.vms.values()) {
			ArrayList<SoftServer> templist = new ArrayList<SoftServer>();
			transformedDistSys.vservers.put(vm.name, templist);
			for (Device dev : vm.devices.values()) {
				if (dev.category.type == DeviceType.CPU) {
					SoftServer server = new SoftServer();
					server.name = vm.name.replaceAll("\\[", "").replaceAll("\\]", "") + "_" + dev.name + "_server";
					server.size.setValue(VirtModelParameters.vmStaticSize);
					server.threadSize.setValue(VirtModelParameters.vmThreadSize);
					server.thrdCount.value = dev.count.value;
					//System.out.println("name:" + server.name + "thrdCount:" + server.thrdCount.value);
					server.thrdBuffer.value = dev.buffer.value;
					server.schedp = dev.schedulingPolicy;
					if(vm.host != null){
						/* Virtual Machine is deployed */
						String actualhost = getActualHostName(vm.name);
						server.machines.add(actualhost);
						transformedDistSys.pms.get(actualhost).softServers.put(server.name, server);
					}
					transformedDistSys.softServers.put(server.name.toLowerCase(), server);
					transformedDistSys.vservers.get(vm.name).add(server);
				}
			}
		}
		
		for(VirtualMachine vm : vms.values()){
			PhysicalMachine pm = transformedDistSys.pms.get(getActualHostName(vm.name));
			
			if(pm.vservers == null){
				pm.vservers = new HashMap<String, ArrayList<SoftServer>>();
			}
			pm.vservers.put(vm.name, transformedDistSys.vservers.get(vm.name));
			
			if(pm.serversDeployedOnVm == null){
				pm.serversDeployedOnVm = new HashMap<String, ArrayList<SoftServer> >();
			}
			ArrayList<SoftServer> tmp = new ArrayList<SoftServer>();
			for(String srvrname : vm.softServers.keySet()){
				tmp.add(transformedDistSys.getServer(srvrname));
			}
			pm.serversDeployedOnVm.put(vm.name, tmp);
		}
		
		HashMap<String, ArrayList<Task>> taskgroup = new HashMap<String, ArrayList<Task>>();
		addTransformedTasks(transformedDistSys.tasks, ModelParameters.inputDistSys.tasks, ModelParameters.inputDistSys.vms, transformedDistSys.pms, ModelParameters.inputDistSys.softServers, transformedDistSys.softServers, transformedDistSys.vservers, taskgroup);

		transformedDistSys.scenarios = buildTransformedScenarios(ModelParameters.inputDistSys.scenarios,ModelParameters.inputDistSys.vms, ModelParameters.inputDistSys.softServers, transformedDistSys.softServers, transformedDistSys.pms, ModelParameters.inputDistSys.tasks, transformedDistSys.tasks, taskgroup);
		
		addNwOverheadToScenarios(transformedDistSys.scenarios, ModelParameters.inputDistSys.vms, transformedDistSys.tasks, ModelParameters.inputDistSys.softServers, transformedDistSys.softServers, transformedDistSys.pms, transformedDistSys.vservers);
		
		transformedDistSys.migrationPolicyInfo = migrationPolicyInfo;
		transformedDistSys.checkParameters();
		transformedDistSys.isTransformed = true;
		//transformedDistSys.print();
		
		/* For computing RAM Utilization, initializing metrics */
		for(VirtualMachine vm : vms.values()){
			PhysicalMachine pm = transformedDistSys.pms.get(getActualHostName(vm.name));
			if(pm.avgVmRamUtils == null){
				pm.avgVmRamUtils = new HashMap<String, Metric>();
			}
			pm.avgVmRamUtils.put(vm.name, new Metric());
		}
			
		return transformedDistSys;
	}
	
	
	public void addTransformedTasks(HashMap<String, Task> transformedTasks, HashMap<String, Task> origTasks, HashMap<String, VirtualMachine> origVms, HashMap<String, PhysicalMachine> transformedPms, HashMap<String, SoftServer> origServers, HashMap<String, SoftServer> transformedServers, HashMap<String, ArrayList<SoftServer>> vservers, Map<String, ArrayList<Task>> taskgroup){
		/* For each vm which is not deployed directly on Physical Machine, create two start and end tasks for its vcpu server */ 
		for(VirtualMachine vm : origVms.values()){
			if(origVms.containsKey(vm.host.name)){
				//System.out.println("Vm.name:" + vm.name + " pm.name:" + getActualHostName(vm.name));
				PhysicalMachine pm = transformedPms.get(getActualHostName(vm.name));
				DeviceCategory cpudevcat = pm.getCpuDevCategory();
				Distribution constDist = new Distribution("const", 0); 
				ServiceTime st = new ServiceTime(cpudevcat, constDist, pm.getDeviceBaseSpeed(cpudevcat));
				
				Task tstart = new Task(vm.name.replaceAll("\\[", "").replaceAll("\\]", ""), -1);
				tstart.name = tstart.name + "_task_start";
				tstart.softServerName = pickVServer(vm.name, vservers);				
				tstart.subtaskServiceTimes.add(st);
				transformedTasks.put(tstart.name, tstart);
				transformedServers.get(tstart.softServerName).tasks.add(tstart);
				
				Task tend = new Task(vm.name.replaceAll("\\[", "").replaceAll("\\]", ""), -1);
				tend.name = tend.name + "_task_end";
				tend.softServerName = tstart.softServerName;				
				tend.subtaskServiceTimes.add(st);
				transformedTasks.put(tend.name, tend);
				transformedServers.get(tend.softServerName).tasks.add(tend);
			}
		}
		
		/* Transformation of Tasks */
		for (SoftServer server : origServers.values()) {
			if (!isDeployedOnVm(origServers.get(server.name), origVms)) {
				/* Server is deployed on PM */
				for (Task task : server.tasks) {
					Task _task = task.getCopy();
					_task.softServerName = server.name;
					transformedTasks.put(_task.name, _task);
				}
				continue;
			}
			
			/*
			 * For each task whose server is deployed on VM, create a new start task with no service time
			 * requirement to make sure that thread is held till execution of actual task has completed 
			 * Create arraylist of consecutive subtask service times of similar device type 
			 * And group them into one task
			 */
			for (Task task : server.tasks) {
				ArrayList<ArrayList<ServiceTime>> cpustgroups = new ArrayList<ArrayList<ServiceTime>>();
				ArrayList<ArrayList<ServiceTime>> noncpustgroups = new ArrayList<ArrayList<ServiceTime>>();
				int stsize = task.subtaskServiceTimes.size();
				ArrayList<Task> eqtasklist = new ArrayList<Task>();

				/* Creation of start task */
				Task tstart = task.getCopy();
				if (tstart.subtaskServiceTimes.size() == 0) {
					throw new Error("ERROR:Task " + task.name + " doesn't have subtasks");
				}
				tstart.name = tstart.name + "_start";
				tstart.softServerName = server.name;
				for (int i = 1; i < tstart.subtaskServiceTimes.size(); i++) {
					tstart.subtaskServiceTimes.remove(i);
				}
				Distribution constDist = new Distribution("const", 0.0);
				tstart.subtaskServiceTimes.get(0).dist = constDist;

				transformedTasks.put(tstart.name, tstart);
				transformedServers.get(server.name).tasks.add(tstart);
				eqtasklist.add(tstart);

				/* Grouping sub tasks with similar device type service demand */
				for (int i = 0; i < stsize;) {
					ArrayList<ServiceTime> temp = new ArrayList<ServiceTime>();
					if (task.subtaskServiceTimes.get(i).devCategory.type == DeviceType.CPU) {
						while (i < stsize && task.subtaskServiceTimes.get(i).devCategory.type == DeviceType.CPU) {
							temp.add(task.subtaskServiceTimes.get(i).getCopy());
							i++;
						}
						cpustgroups.add(temp);
					} else {
						while (i < stsize && task.subtaskServiceTimes.get(i).devCategory.type == DeviceType.NONCPU) {
							temp.add(task.subtaskServiceTimes.get(i).getCopy());
							i++;
						}
						PhysicalMachine pm = transformedPms.get(getActualHostName(transformedServers.get(server.name)));
						DeviceCategory cpudevcat = pm.getCpuDevCategory();
						Distribution dist = new Distribution(VirtModelParameters.ioOverheadDist, VirtModelParameters.ioOverhead); 
						ServiceTime st = new ServiceTime(cpudevcat, dist, pm.getDeviceBaseSpeed(cpudevcat));
						temp.add(st);
						noncpustgroups.add(temp);
					}
				}
				
				/* Combining sequence of sub task with CPU device service demand into one */
				for (int i = 0; i < cpustgroups.size(); i++) {
					String vservername = pickVServer(getLowestVm(server.machines.get(0)), vservers);
					String tname = task.name + "_" + vservername + "_" + String.valueOf(i + 1) + "_"
							+ String.valueOf(cpustgroups.size());
					Task temptask = new Task(tname, -1);
					temptask.subtaskServiceTimes = cpustgroups.get(i);
					temptask.softServerName = vservername;
					transformedTasks.put(temptask.name, temptask);
					transformedServers.get(vservername).tasks.add(temptask);
					
					/* If vm is deployed on another vm, then we have to add extra call between them */
					VirtualMachine vm = getVM(ModelParameters.inputDistSys.getServer(server.name).machines.get(0));
					Stack<Task> stack = new Stack<Task>();
					while(ModelParameters.inputDistSys.vms.containsKey(vm.host.name)){
						Task tempstart = transformedTasks.get(vm.name.replaceAll("\\[", "").replaceAll("\\]", "") + "_task_start");
						Task tempend = transformedTasks.get(vm.name.replaceAll("\\[", "").replaceAll("\\]", "") + "_task_end");
						stack.push(tempend);
						eqtasklist.add(tempstart);
						vm = getVM(vm.host.name);
					}
					eqtasklist.add(temptask);
					while(!stack.isEmpty()){
						eqtasklist.add(stack.pop());
					}
					
					
				}
				
				/* Combining sequence of sub task with NonCPU device service demand into one */
				for (int i = 0; i < noncpustgroups.size(); i++) {
					String sname = getHypervisorName(server.name);
					String tname = task.name + "_" + sname;
					Task temptask = new Task(tname, -1);
					temptask.softServerName = server.name;
					temptask.subtaskServiceTimes = noncpustgroups.get(i);
					temptask.softServerName = sname;
					eqtasklist.add(temptask);
					transformedTasks.put(temptask.name, temptask);
					transformedServers.get(sname).tasks.add(temptask);
				}

				/* Based on synchronicity of actual tasknode, taskend can or can't be used
				 * If next tasknode  is asynchronous 
				 * i.e. this tasknode to next tasknode message in message sequence chart is asynchronous, 
				 * then following task will be used and added into equivalent tasklist.
				 */
				Task tend = tstart.getCopy();
				tend.name = tstart.name.substring(0, tstart.name.length() - 6) + "_end";
				tend.softServerName = server.name;
				eqtasklist.add(tend);
				transformedTasks.put(tend.name, tend);
				transformedServers.get(server.name).tasks.add(tend);
				taskgroup.put(task.name, eqtasklist);
				
				/*TRANSCHECK:Transformation of virtual resources should be done here */

			}
		}
	}
	
	public HashMap<String, Scenario> buildTransformedScenarios(HashMap<String, Scenario> origScenarios, HashMap<String, VirtualMachine> origVms, HashMap<String, SoftServer> origServers, HashMap<String, SoftServer> transformedServers, HashMap<String, PhysicalMachine> transformedPms, HashMap<String, Task> origTasks, HashMap<String, Task> transformedTasks, HashMap<String, ArrayList<Task>> taskgroup){
		HashMap<String, Scenario> transformedScenarios = new HashMap<String, Scenario>();
		/* For each scenario, create new scenario with new tasknodes corresponding to new tasks */
		for (Scenario scenario : origScenarios.values()) {
			if(!scenario.isValidated){
				scenario.validate();
			}
			Scenario newsce = new Scenario();
			newsce.name = scenario.name;
			newsce.setProbability(new Variable("local", scenario.getProbability()));
			
			/* To store idx of all the tasknodes which are syncronous responses */ 
			Set<Integer> syncresponses = new HashSet<Integer>();
			
			/* Traverse Scenario Tasknode tree level-wise
			 * Side-by-side build equivalent tasknode tree in new scenario by putting tasknode sequence for original tasknode
			 * Note that, before adding any node to level array, it is already processed.
			 * When any entry is retrieved from level, only its children are processed and added to next level 
			 */
			ArrayList<TaskNode> level = new ArrayList<TaskNode>();
			ArrayList<TaskNode> newlevel = new ArrayList<TaskNode>();
			TaskNode root;
			
			/* Special case handling involving use of "user" task
			 * If user task is used and next tasknode is deployed on vm, then we don't need to have user task in new scenario tree
			 */
			if(scenario.rootNode.name.equals("user") && isDeployedOnVm(origServers.get(getTask(scenario.rootNode.children.get(0).name).softServerName), origVms)){
				root = scenario.rootNode.children.get(0);
				level.add(root);
			}else{
				root = scenario.rootNode;
				level.add(root);
			}
			
			/* Based on original task's server's deployment, construct its equivalent tasknode in transformed scenario */
			if(!isDeployedOnVm(origServers.get(origTasks.get(root.name).softServerName), origVms)){
				newsce.rootNode = root.getCopyWithoutChildren();
				newsce.rootNode.servername = transformedTasks.get(newsce.rootNode.name).softServerName;
				newlevel.add(newsce.rootNode);
			}else{
				ArrayList<TaskNode> list = buildStartToEndTaskNode(taskgroup.get(root.name));
				TaskNode startNode = list.get(0);
				TaskNode endNode = list.get(list.size()-1);
				newsce.rootNode = startNode;
				newlevel.add(endNode);
				newsce.rootNode.children.get(0).issync = true;
				copyNodeInfo(root, startNode, endNode);
			}
			
			
			while(!level.isEmpty()){
				ArrayList<TaskNode> nextlevel = new ArrayList<TaskNode>();
				ArrayList<TaskNode> newnextlevel = new ArrayList<TaskNode>();
				
				/* Traversing current level */
				for(int i=0;i<level.size();i++){
					TaskNode elem = level.get(i);
					TaskNode newelem = newlevel.get(i);
					/* Processing children of each element in current level and adding them to next level */
					for(int j=0;j<elem.children.size();j++){
						TaskNode child = elem.children.get(j);
						//System.out.println("oldchild:" + oldchild.name);
						
						/* Special case handling involving user task
						 * User task can only happen in tree with structure user-another_task_node-user
						 * If another_task_node's server is deployed on phyiscal machine, then new scenario will be similar to original 
						 */
						if(child.name.equals("user")){
							if((child.isRoot && !isDeployedOnVm(origServers.get(origTasks.get(child.children.get(0).name).softServerName), origVms))
								|| (!child.isRoot && isDeployedOnVm(origServers.get(origTasks.get(child.parent.name).softServerName), origVms))){
								//TaskNode newchild = child.getCopyWithoutChildren();
								//newchild.parent = newelem;
								//newelem.children.add(newchild);
								//newnextlevel.add(newchild);
								continue;
							}
						}
						nextlevel.add(child);
						if(!isDeployedOnVm(origServers.get(origTasks.get(child.name).softServerName), origVms)){
							/* Task'server is deployed on Physical Machine. No need to make changes  */
							TaskNode newchild = child.getCopyWithoutChildren();
							newchild.servername = transformedTasks.get(child.name).softServerName;
							newchild.parent = newelem;
							newelem.children.add(newchild);
							newnextlevel.add(newchild);
						}else{
							/* Task'server is deployed on Virtual Machine. Need to make additional changes 	 */
							ArrayList<TaskNode> list = buildStartToEndTaskNode(taskgroup.get(child.name));
							TaskNode startNode = list.get(0);
							TaskNode endNode = list.get(list.size()-1);
							copyNodeInfo(child, startNode, endNode);
							TaskNode newchildstart = startNode;
							newchildstart.children.get(0).issync = true;
							newchildstart.parent = newelem;
							newelem.children.add(newchildstart);
							
							/* Synchronous call handling  */
							if(child.isSync()){
								TaskNode endprevchild = newchildstart.parent;
								//System.out.println("oldchild.parent:" + child.parent.name + " endprevchild:" + endprevchild.name + " parent:" + endprevchild.parent.name);
								endprevchild.parent.children.clear();
								for(TaskNode tnode : endprevchild.children){
									endprevchild.parent.children.add(tnode);
								}
								
								/*
								System.out.println("Child.name:"+ child.name);
								for(Integer key : scenario.syncpairs.keySet()){
									System.out.print(key + ":");
									for(Integer val : scenario.syncpairs.get(key)){
										System.out.print("," + val);
									}
									System.out.println();
								}
								*/
								for(int k=0;k<scenario.syncpairs.get(child.index).size();k++){
									syncresponses.add(scenario.syncpairs.get(child.index).get(k));
								}
							}
							if(syncresponses.contains(child.index)){
								newchildstart.parent.children.clear();
								for(TaskNode tnode : newchildstart.children){
									tnode.issync = false;
									newchildstart.parent.children.add(tnode);
								}
							}
							newnextlevel.add(endNode);
							
							
						}
					}
				}
				level = nextlevel;
				newlevel = newnextlevel;
			}
			transformedScenarios.put(newsce.name, newsce);
		}
		
		return transformedScenarios;
	}
	
	public void addNwOverheadToScenarios(HashMap<String, Scenario> transformedScenarios, HashMap<String, VirtualMachine> origVms, HashMap<String, Task> transformedTasks, HashMap<String, SoftServer> origServers, HashMap<String, SoftServer> transformedServers, HashMap<String, PhysicalMachine> transformedPms, HashMap<String, ArrayList<SoftServer>> _vservers){
		/* For newly formed scenario, look for pair of tasknodes whose servers are deployed on virtual machine and those vms are not co-located 
		 * Add communication overhead task nodes between them
		 */
		/* vserverSet is used to check later in transformed distributed system, a server is newly created corresponding to virtual CPU device in vm */
		HashSet<SoftServer> vserverSet = new HashSet<SoftServer>();
		for(ArrayList<SoftServer> srvrlist : _vservers.values()){
			for(SoftServer server : srvrlist){
				vserverSet.add(server);
			}
		}
		for(Scenario scenario : transformedScenarios.values()){
			ArrayList<TaskNode> level = new ArrayList<TaskNode>();
			level.add(scenario.rootNode);
			/* For each scenario do level traversal
			 */
			while(!level.isEmpty()){
				ArrayList<TaskNode> _level = new ArrayList<TaskNode>();
				for(int i=0;i<level.size();i++){
					TaskNode fromnode = level.get(i);
					SoftServer fromserver = transformedServers.get(transformedTasks.get(fromnode.name).softServerName);
					
					/* If first fromnode is user, then scenario tree will be like user-another_task_node-user.
					 * Another_task_node must have been deployed on pm or else it "user" task would have been removed in previous processing.
					 * So it is safe to continue loop on finding user at fromnode  
					 */
					if(fromnode.name.equals("user") && fromserver.name.equals("user"))
						continue;
					
					PhysicalMachine frompm = transformedPms.get(fromserver.machines.get(0));
					
					/* Process children of current level's each element and add them to next level */
					for(int j=0;j<fromnode.children.size();j++){
						TaskNode tonode = fromnode.children.get(j);
						_level.add(tonode);
						SoftServer toserver = transformedServers.get(transformedTasks.get(tonode.name).softServerName);
						//System.out.println("fromtask:" + fromnode.name + " fromserver: " + fromserver.name + " frompm:" + frompm.name);
						//System.out.println("totask:" + tonode.name + " toserver: " + toserver.name );
						PhysicalMachine topm = transformedPms.get(toserver.machines.get(0));
						
						/* Check whether source and destination PMs are same or not 
						 */
						if(!frompm.name.equals(topm.name)){
							TaskNode from_nwtasknode = new TaskNode("network_call_" + frompm.getHypervisorName());
							from_nwtasknode.servername = frompm.getHypervisorName();
							TaskNode to_nwtasknode = new TaskNode("network_call_" + topm.getHypervisorName());
							to_nwtasknode.servername = topm.getHypervisorName();
							/* Check whether source and destination servers were originally deployed on virtual machine or not 
							 */
					
							if((vserverSet.contains(fromserver) || isDeployedOnVm(origServers.get(fromserver.name), origVms)) && (vserverSet.contains(toserver) || isDeployedOnVm(origServers.get(toserver.name), origVms))){
								fromnode.children.set(j, from_nwtasknode);
								from_nwtasknode.parent = fromnode;
								from_nwtasknode.children.add(to_nwtasknode);
								//to_nwtasknode.parent = from_nwtasknode; // this is not required as this will be done in copyNodeInfo
								copyNodeInfo(fromnode, from_nwtasknode, to_nwtasknode);
								/* Network calls should have probability equal to "tonode"*/
								from_nwtasknode.prob.value = tonode.prob.value;
								to_nwtasknode.prob.value = tonode.prob.value;
								to_nwtasknode.children.add(tonode);
								tonode.parent = to_nwtasknode;
							}else if((vserverSet.contains(fromserver) || isDeployedOnVm(origServers.get(fromserver.name), origVms))){
								fromnode.children.set(j, from_nwtasknode);
								from_nwtasknode.parent = fromnode;
								from_nwtasknode.children.add(tonode);
								tonode.parent = from_nwtasknode;
								copyNodeInfo(fromnode, from_nwtasknode, from_nwtasknode);
								from_nwtasknode.prob.value = tonode.prob.value;
							}else if((vserverSet.contains(toserver) || isDeployedOnVm(origServers.get(toserver.name), origVms))){
								fromnode.children.set(j, to_nwtasknode);
								to_nwtasknode.parent = fromnode;
								copyNodeInfo(fromnode, to_nwtasknode, to_nwtasknode);
								to_nwtasknode.children.add(tonode);
								tonode.parent = to_nwtasknode;
								to_nwtasknode.prob.value = tonode.prob.value;
							}else{
								//System.out.println("Kuch nahi hua");
							}
							
						}
					}
				}
				level = _level;
			}
		}

		/* For each scenario, if servers of start and end tasks are deployed on VM, then add netwokr overhead */
		for(Scenario scenario : transformedScenarios.values()){
			SoftServer fromServer = transformedServers.get(scenario.rootNode.servername);
			if(isDeployedOnVm(origServers.get(fromServer.name), origVms)){
				String hypervisorname = getHypervisorName(origServers.get(fromServer.name), origVms, transformedPms);
				TaskNode nwcall = new TaskNode("network_call_" + hypervisorname);
				nwcall.servername = hypervisorname;
				nwcall.parent = null;
				copyNodeInfo(scenario.rootNode, nwcall, nwcall);
				nwcall.isRoot = true;
				scenario.rootNode.parent = nwcall;
				scenario.rootNode.isRoot = false;
				nwcall.children.add(scenario.rootNode);
				scenario.rootNode = nwcall;
			}
			ArrayList<TaskNode> endnodes = new ArrayList<TaskNode>();
			
			ArrayList<TaskNode> level = new ArrayList<TaskNode>();
			level.add(scenario.rootNode);
			/* Level traversal  */
			while(!level.isEmpty()){
				ArrayList<TaskNode> nxtlevel = new ArrayList<TaskNode>();
				for(int i=0;i<level.size();i++){
					TaskNode node = level.get(i);
					if(node.children.size() == 0){
						endnodes.add(node);
						continue;
					}
					for(TaskNode child : node.children){
						nxtlevel.add(child);
					}
				}
				level = nxtlevel;
			}
			
			for(TaskNode endnode : endnodes){
				SoftServer endserver = transformedServers.get(endnode.servername);
				if(isDeployedOnVm(origServers.get(endserver.name), origVms)){
					String hypervisorname = getHypervisorName(endnode.servername);
					TaskNode nwcall = new TaskNode("network_call_" + hypervisorname);
					nwcall.servername  = getHypervisorName(origServers.get(endserver.name), origVms, transformedPms);
					copyNodeInfo(endnode, nwcall, nwcall);
					nwcall.parent = endnode;
					endnode.children.add(nwcall);
				}
			}
			
		}
	}

	// scenario arrival rate is based on scenario probability
	public void setScenarioArrivalRate() {
		for (int slot = 0; slot < ModelParameters.intervalSlotCount; slot++) {
			for (Scenario sc : ModelParameters.inputDistSys.scenarios.values()) {
				sc.setArateToScenario(slot, ModelParameters.getArrivalRate(slot).getValue() * sc.getProbability());
			}
		}
	}

	/********************************************************************/
	// Add power-managed device into the list
	public void addPowerManagedDevices(PhysicalDevice pdevice) {
		powerManagedDevices.put(pdevice.name, pdevice);
	}
	/********************************************************************/
}