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
import java.util.List;
import perfcenter.baseclass.enums.DeviceType;
import perfcenter.baseclass.DeviceCategory;

import org.apache.log4j.FileAppender;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;

/**
 * This is the complete representation of the input file. Parser fills in this structure.
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
	/** List of softServers. used for reference only. They are deployed on Machines */
	public HashMap<String, SoftServer> softServers;
	/** All the Machines. For simulation, this would contain objects of MachineSim. */
	public HashMap<String, PhysicalMachine> pms;
	public HashMap<String, VirtualMachine> vms;
	/** All the Scenarios. For simulation, this would contain objects of ScenarioSim. */
	public HashMap<String, Scenario> scenarios;
	public HashMap<String, Lan> lans;
	/** All the LanLinks. For simulation, this would contain objects of LanLinkSim. */ 
	public HashMap<String, LanLink> links;
	
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

	public DistributedSystem() {
		pms = new HashMap<String, PhysicalMachine>();
		vms = new HashMap<String, VirtualMachine>();
		softRes = new HashMap<String, SoftResource>();
		devcats = new HashMap<String, DeviceCategory>();
		pdevices = new HashMap<String, PhysicalDevice>();
		vdevices = new HashMap<String, VirtualDevice>();

		/***************************************************************************/
		powerManagedDevicePrototypes = new HashMap<String, PhysicalDevice>(); // added by Rakesh
		powerManagedDevices = new HashMap<String, PhysicalDevice>(); // added by Rakesh
		/***************************************************************************/
		variables = new HashMap<String, Variable>();
		tasks = new HashMap<String, Task>();
		softServers = new HashMap<String, SoftServer>();
		scenarios = new HashMap<String, Scenario>();
		lans = new HashMap<String, Lan>();
		links = new HashMap<String, LanLink>();

		// This is done to have a dummy task "user" at the start and end of a scenario
		Task t = new Task("user", 0);

		SoftServer sdummy = new SoftServer("user");
		sdummy.addTask(t);
//		sdummy.addHost("dummy");
		softServers.put("user",sdummy);
		
		t.softServerName = "user";
		tasks.put("user",t);
		
		// why these things are here? they are local variables which are being used nowhere else
//		Host hdummy = new Host("dummy", 1);
//		hdummy.addServer(sdummy);
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
		return vms.containsKey(vmname.toLowerCase());
	}
	
	public DeviceCategory getDeviceCategory(String devcatname){
		if(devcats.containsKey(devcatname.toLowerCase())){
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
	
	public DeviceType getDeviceType(String devcatname){
		if(devcats.containsKey(devcatname.toLowerCase())){
			return devcats.get(devcatname.toLowerCase()).type;
		}
		throw new Error(devcatname + " is not Device Category");
	}
	
	public DeviceCategory getVDeviceCategory(String vdevname){
		if(vdevices.containsKey(vdevname.toLowerCase())){
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
	
	//CHECK : This method is not used anywhere
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
