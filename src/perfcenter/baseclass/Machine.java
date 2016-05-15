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
 */package perfcenter.baseclass;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Collection;
import java.util.Random;

import org.apache.log4j.Logger;

import perfcenter.baseclass.enums.PowerManagementGovernor;
import perfcenter.baseclass.enums.SchedulingPolicy;
import perfcenter.baseclass.exception.DeviceNotFoundException;
import perfcenter.baseclass.DeviceCategory;

/**
 * This represents the machine definition in the input file.
 * 
 * A machine can have software servers deployed onto them. Machine has set of devices and an machine is
 * deployed on to a single LAN. Machine also has machine resources and devices deployed on them.
 * 
 * @author akhila
 * @author rakesh
 * @author bhavin
 */
public class Machine {

	/** Name of the machine. */
	public String name;
	
	/** Devices that are part of Machine. In simulation, this would contain DeviceSim objects. */
	public HashMap<String, Device> devices = new HashMap<String, Device>();
	/** SoftResources deployed on machine. In simulation, this would contain SoftResourceSim objects. */
	public HashMap<String, SoftResource> softResources = new HashMap<String, SoftResource>();
	/** Arrival rate to machine. Used by analytical part. */
	public double arrivalRate;
	/** SoftServers deployed on this machine. In simulation, this would contain SoftServerSim objects. */
	public HashMap<String, SoftServer> softServers = new HashMap<String, SoftServer>();
	/** lan onto which machine will be deployed */
	public String lan = "";
	private Logger logger = Logger.getLogger("Machine");
	
	public Random r;
	public Machine() {
		r = new Random();
	}

	public Machine(String hname, int num) {
		r = new Random();
		name = hname + "[" + num + "]";
	}

	public String getName() {
		return name;
	}

	public void validate() {
		if (devices.isEmpty()) {
			logger.warn("Warning:Machine \"" + name + "\" does not have devices ");
		}
		if (softServers.isEmpty()) {
			logger.warn("Warning:Machine \"" + name + "\" does not have servers deployed ");
		}
		if (lan.length() == 0) {
			logger.warn("Warning:Machine \"" + name + "\" is not deployed on any lan ");
		}
	}

	public void print() {
		System.out.println("MachineName " + name);
		for (Device dev : devices.values()) {
			dev.print();
		}
		System.out.println(" Deploys ");
		for (SoftServer serv : softServers.values()) {
			serv.print();
		}
		for (SoftResource sr : softResources.values()) {
			sr.print();
		}
		if (lan != null) {
			System.out.println(" Deployed on " + lan);
		}
	}


	/**
	 * Returns the specified device object.
	 */
	public Device getDevice(String dname) throws DeviceNotFoundException {
		if (devices.containsKey(dname.toLowerCase())) {
			return devices.get(dname.toLowerCase());
		}
		throw new DeviceNotFoundException(" \"" + dname + "\" is not device for machine " + name);
	}
	
	//TODO:IMPROVE //URGENT
	public String getDeviceName(DeviceCategory devcat){
		double d = r.nextDouble();
		ArrayList<String> candidates = new ArrayList<String>();
		for(Device device: devices.values()){
			if(device.category.name.compareToIgnoreCase(devcat.name) == 0){
				candidates.add(device.name);
			}
		}
		int idx = (int)d*candidates.size();
		if(candidates.size() == 0)
			return null;
		return candidates.get(idx);
	}


	public boolean isDeviceDeployed(String deviceName) {
		return devices.containsKey(deviceName.toLowerCase());
	}

	protected void addDevice(Device dev) {
		devices.put(dev.name, dev);
	}

	protected void addSoftRes(SoftResource sr) {
		softResources.put(sr.name, sr);
	}

	protected void addSoftServer(SoftServer ss) {
		softServers.put(ss.name, ss);
	}
	
	public void modifyDeviceCount(String dname, double count) throws DeviceNotFoundException {
		Object dev = getDevice(dname);
		if (((Device) dev).count.name.compareToIgnoreCase("local") == 0) {
			((Device) dev).count.value = count;
			return;
		}
		throw new Error("Attempt to modify the device count of machine " + name + ", instead variable " + ((Device) dev).count.name
				+ " should be modified");
	}

	public void modifyDeviceBuffer(String dname, double buffersize) throws DeviceNotFoundException {
		Object dev = getDevice(dname);
		if (((Device) dev).buffer.name.compareToIgnoreCase("local") == 0) {
			((Device) dev).buffer.value = buffersize;
			return;
		}
		throw new Error("Attempt to modify the device buffer of machine " + name + ", instead variable " + ((Device) dev).count.name
				+ " should be modified");
	}

	public void modifyDeviceSchedPol(String name, SchedulingPolicy pol) throws DeviceNotFoundException {
		getDevice(name).schedulingPolicy = pol;
	}

	public void addLan(String name) {
		lan = name;
	}

	public void removeLan(String s) {
		if (lan.length() == 0) {
			throw new Error(name + " was not deployed on lan " + s);
		} else {
			lan = "";
		}
	}

	public SoftServer getServer(String sname) {
		if (softServers.containsKey(sname.toLowerCase())) {
			return softServers.get(sname.toLowerCase());
		}
		throw new Error("\"" + sname + "\" is not Server on Machine " + name);
	}

	public boolean isServerDeployed(String sname) {
		return softServers.containsKey(sname.toLowerCase());
	}

	public void addServer(SoftServer srv) {
		SoftServer scpy = srv.getCopy();
		softServers.put(scpy.name, scpy);
	}

	/** 
	 * Checks if server exists and removes it from the list.
	 * Called when undeploying server from machine.
	 */
	public void removeServer(String srvname) {
		if (softServers.containsKey(srvname.toLowerCase())) {
			softServers.remove(srvname);
			return;
		}
		throw new Error(srvname + " was not deployed on machine " + name);
	}

	/** check if the software resource is already deployed. If it is then update
	 *  its servers list. If it is not deployed, get a copy and deploy on machine
	 */  
	void deploySoftRes(String srName, String srvName) throws Exception {
		if (isSoftResourceDeployed(srName) == true) {
			SoftResource sr = getSoftRes(srName);
			sr.addSoftServer(srvName);
		} else {
			// get new copy of soft resource and add to list
			SoftResource sr = ModelParameters.inputDistSys.getSoftRes(srName);
			SoftResource srcpy = sr.getCopy();
			srcpy.addSoftServer(srvName);
			softResources.put(srcpy.name, srcpy);
		}
	}

	/** check if the soft resource is used by more than one server. If it is, then update its
	 * servers list. If it is used by any server then remove the soft resource */
	void unDeploySoftRes(String srName, String srvName) throws Exception {
		SoftResource sr = getSoftRes(srName);
		if (sr.isDeployedOnSoftServer(srvName) == true) {
			sr.removeSoftServer(srvName);
		}
		softResources.remove(srName);
	}

	public boolean isSoftResourceDeployed(String srName) {
		return softResources.containsKey(srName.toLowerCase());
	}

	public SoftResource getSoftRes(String srName) throws Exception {
		if (softResources.containsKey(srName.toLowerCase())) {
			return softResources.get(srName.toLowerCase());
		}
		throw new Exception("softres " + srName + " is not deployed on Machine " + name);
	}

	public Collection<SoftServer> getSoftServersList() {
		return softServers.values();
	}

	public Collection<Device> getDevicesList() {
		return devices.values();
	}

	public void setDeviceAsPowerManaged(String pdevname) throws DeviceNotFoundException {
		if (isDeviceDeployed(pdevname) == false) {
			DeviceCategory devcat = ModelParameters.inputDistSys.getPDevice(pdevname).category;
			PhysicalDevice pdev = new PhysicalDevice(pdevname, devcat);
			devices.put(pdev.name, pdev);
		}
		// set device-type as power-managed with governor as
		// ONDEMAND[default gov for power-managed devices]
		PhysicalDevice pdev = (PhysicalDevice)getDevice(pdevname);
		pdev.isDevicePowerManaged = true;
		pdev.governor = PowerManagementGovernor.ONDEMAND;

		// Set other PowerManaged attributes of this device
		for (Device dev : ModelParameters.inputDistSys.powerManagedDevicePrototypes.values()) {
			if (dev.name.equalsIgnoreCase(pdevname)) // device type is found
			{
				// copy all attributes from device type object to this device
				// System.out.println("----attributes copying----");
				pdev.availableSpeedLevels = dev.availableSpeedLevels;
				pdev.powerConsumptionsLevels = dev.powerConsumptionsLevels;
				pdev.idlePower = dev.idlePower;
				pdev.deviceProbeInterval = dev.deviceProbeInterval;
				// set probe interval min & max
				pdev.downThreshold = dev.downThreshold;
				pdev.upThreshold = dev.upThreshold;

				// setting max_freq_index
				pdev.totalFrequencyLevels = dev.totalFrequencyLevels;

			}
		}
		ModelParameters.inputDistSys.addPowerManagedDevices(pdev);
	}

	/** Set governor of power-managed devices */
	public void addPMGovernor(String name, PowerManagementGovernor gov) throws DeviceNotFoundException {
		getDevice(name).governor = gov;
	}

	/** Set device speed level index here. Setting device speed is common with USERSPACE governor
	 * for other governors this attribute cannot be set. */
	public void addSetSpeedLevelIndex(String name, Variable v) throws DeviceNotFoundException, Exception {
		if (getDevice(name).isDevicePowerManaged && getDevice(name).governor == PowerManagementGovernor.USERSPACE
				&& getDevice(name).totalFrequencyLevels > v.value) {
			getDevice(name).userspaceSpeedIndex = v.value;
		} else {
			throw new Exception("Provide proper user sapce index for device : " + getDevice(name).name + " in between 0 to "
					+ (getDevice(name).totalFrequencyLevels - 1) + "");

		}
	}

	/** Set device probe interval. Only applicable for powermanaged devices. */
	public void addProbeInterval(String name, Variable v) throws DeviceNotFoundException {
		getDevice(name).deviceProbeInterval = v.value;
	}

	/**
	 * Set device governor UpThreshold. If utilization of device exceeds upthreshold
	 * Device governor speeds up device execution by reducing service times associated with this device.
	 * Only applicable for powermanaged devices.
	 */ 
	public void addGovernorUpThreshold(String name, Variable v) throws DeviceNotFoundException {
		getDevice(name).upThreshold = v.value;
	}

	public void addGovernorDownThreshold(String name, Variable v) throws DeviceNotFoundException {
		getDevice(name).downThreshold = v.value;
	}

	/** modify probe interval of a PM device from setstmt */
	public void modifyProbeInterval(String dname, double new_probe_int) throws DeviceNotFoundException {
		Object dev = getDevice(dname);
		if (((Device) dev).count.name.compareToIgnoreCase("local") == 0) {
			((Device) dev).deviceProbeInterval = new_probe_int;
			return;
		}
		throw new Error("Attempt to modify the device probe_interval of machine " + name + ", instead variable " + ((Device) dev).deviceProbeInterval
				+ " should be modified");
	}

	/** modify governor up_threshold of a PM device from setstmt */
	public void modifyGovUpThreshold(String device, double gov_up_threshold) throws DeviceNotFoundException {
		Object dev = getDevice(device);
		if (((Device) dev).count.name.compareToIgnoreCase("local") == 0) {
			((Device) dev).upThreshold = gov_up_threshold;
			return;
		}
		throw new Error("Attempt to modify the device up_threshold of machine " + name + ", instead variable " + ((Device) dev).upThreshold
				+ " should be modified");
	}

	/** modify governor down_threshold of a PM device from setstmt */
	public void modifyGovDownThreshold(String device, double gov_down_threshold) throws DeviceNotFoundException {
		Object dev = getDevice(device);
		if (((Device) dev).count.name.compareToIgnoreCase("local") == 0) {
			((Device) dev).downThreshold = gov_down_threshold;
			return;
		}
		throw new Error("Attempt to modify the device down_threshold of machine " + name + ", instead variable " + ((Device) dev).downThreshold
				+ " should be modified");
	}
	

}
