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

import org.apache.log4j.Logger;

import perfcenter.baseclass.enums.PowerManagementGovernor;
import perfcenter.baseclass.enums.SchedulingPolicy;
import perfcenter.baseclass.exception.DeviceNotFoundException;

/**
 * This represents the host definition in the input file.
 * 
 * A host can have software servers deployed onto them. Host has set of devices and an host is
 * deployed on to a single LAN. Host also has virtual resources and devices deployed on them.
 * 
 * @author akhila
 * @author rakesh
 * @author bhavin
 */
public class Host {

	/** Name of the host. */
	public String name;
	
	//SCALABILITY: change the following ArrayList objects to TreeMap
	/** Devices that are part of Host. In simulation, this would contain DeviceSim objects. */
	public ArrayList<Device> devices = new ArrayList<Device>();
	/** VirtualResources deployed on host. In simulation, this would contain VirtualResourceSim objects. */
	public ArrayList<VirtualResource> virResources = new ArrayList<VirtualResource>();
	/** Arrival rate to host. Used by analytical part. */
	public double arrivalRate;
	/** SoftServers deployed on this host. In simulation, this would contain SoftServerSim objects. */
	public ArrayList<SoftServer> softServers = new ArrayList<SoftServer>();
	/** lan onto which host will be deployed */
	public String lan = "";
	private Logger logger = Logger.getLogger("Host");

	public Host() {}

	public Host(String hname, int num) {
		name = hname + "[" + num + "]";
	}

	public String getName() {
		return name;
	}

	public void validate() {
		if (devices.isEmpty()) {
			logger.warn("Warning:Host \"" + name + "\" does not have devices ");
		}
		if (softServers.isEmpty()) {
			logger.warn("Warning:Host \"" + name + "\" does not have servers deployed ");
		}
		if (lan.length() == 0) {
			logger.warn("Warning:Host \"" + name + "\" is not deployed on any lan ");
		}
	}

	public void print() {
		System.out.println("HostName " + name);
		for (Device dev : devices) {
			dev.print();
		}
		System.out.println(" Deploys ");
		for (SoftServer serv : softServers) {
			serv.print();
		}
		for (VirtualResource sr : virResources) {
			sr.print();
		}
		if (lan != null) {
			System.out.println(" Deployed on " + lan);
		}
	}

	public Host getCopy(String hname, int num) throws DeviceNotFoundException {
		Host hcpy = new Host(hname, num);
		for (Device d : devices) {
			Device dcpy = d.getCopy();
			hcpy.addDevice(dcpy);
		}
		for (VirtualResource sr : virResources) {
			VirtualResource srcpy = sr.getCopy();
			hcpy.addVirtualRes(srcpy);
		}
		for (SoftServer ss : softServers) {
			SoftServer sscpy = ss.getCopy();
			hcpy.addSoftServer(sscpy);
		}
		hcpy.addLan(lan);
		return hcpy;
	}

	/**
	 * Returns the specified device object.
	 */
	public Device getDevice(String dname) throws DeviceNotFoundException {

		for (Device dev : devices) {
			if (dev.name.compareToIgnoreCase(dname) == 0) {
				return (dev);
			}
		}
		throw new DeviceNotFoundException(" \"" + dname + "\" is not device for host " + name);
	}

	public boolean isDeviceDeployed(String deviceName) {
		for (Device device : devices) {
			if (device.name.compareToIgnoreCase(deviceName) == 0) {
				return true;
			}
		}
		return false;
	}

	private void addDevice(Device dev) {
		devices.add(dev);
	}

	private void addVirtualRes(VirtualResource sr) {
		virResources.add(sr);
	}

	private void addSoftServer(SoftServer ss) {
		softServers.add(ss);
	}

	public void addDeviceCount(String name, Variable count) throws DeviceNotFoundException {
		if (isDeviceDeployed(name) == false) {
			Device dev = new Device(name);
			devices.add(dev);
		}
		getDevice(name).count = count;
	}

	public void addDeviceBuffer(String Dname, Variable buffersize) throws DeviceNotFoundException {
		if (isDeviceDeployed(Dname) == false) {
			Device dev = new Device(Dname);
			devices.add(dev);
		}
		getDevice(Dname).buffer = buffersize;
		return;
	}

	public void addDeviceSpeedUp(String name, Variable pspeed) throws DeviceNotFoundException {
		if (isDeviceDeployed(name) == false) {
			Device dev = new Device(name);
			devices.add(dev);
		}
		getDevice(name).speedUpFactor = pspeed;
	}

	public void addDeviceSchedPol(String name, SchedulingPolicy pol) throws DeviceNotFoundException {
		if (isDeviceDeployed(name) == false) {
			Device dev = new Device(name);
			devices.add(dev);
		}
		getDevice(name).schedulingPolicy = pol;
	}

	public void modifyDeviceCount(String dname, double count) throws DeviceNotFoundException {
		Object dev = getDevice(dname);
		if (((Device) dev).count.name.compareToIgnoreCase("local") == 0) {
			((Device) dev).count.value = count;
			return;
		}
		throw new Error("Attempt to modify the device count of host " + name + ", instead variable " + ((Device) dev).count.name
				+ " should be modified");
	}

	public void modifyDeviceBuffer(String dname, double buffersize) throws DeviceNotFoundException {
		Object dev = getDevice(dname);
		if (((Device) dev).buffer.name.compareToIgnoreCase("local") == 0) {
			((Device) dev).buffer.value = buffersize;
			return;
		}
		throw new Error("Attempt to modify the device buffer of host " + name + ", instead variable " + ((Device) dev).count.name
				+ " should be modified");
	}

	public void modifyDeviceSchedPol(String name, SchedulingPolicy pol) throws DeviceNotFoundException {
		getDevice(name).schedulingPolicy = pol;
	}

	public void addLan(String name) {
		lan = name;
	}

	public void removeLan(String n) {
		if (lan.length() == 0) {
			throw new Error(name + " was not deployed on lan " + n);
		} else {
			lan = "";
		}
	}

	public SoftServer getServer(String sname) {
		for (SoftServer serv : softServers) {
			if (serv.name.compareToIgnoreCase(sname) == 0) {
				return serv;
			}
		}
		throw new Error("\"" + sname + "\" is not Server on Host " + name);
	}

	public boolean isServerDeployed(String sname) {
		for (Object serv : softServers) {
			if (((SoftServer) serv).name.compareToIgnoreCase(sname) == 0) {
				return true;
			}
		}
		return false;
	}

	public void addServer(SoftServer srv) {
		SoftServer scpy = srv.getCopy();
		softServers.add(scpy);
	}

	/** 
	 * Checks if server exists and removes it from the list.
	 * 
	 * Called when undeploying server from host.
	 */
	public void removeServer(String srvname) {
		for (Object s : softServers) {
			if (((SoftServer) s).name.compareToIgnoreCase(srvname) == 0) {
				softServers.remove(s);
				return;
			}
		}
		throw new Error(srvname + " was not deployed on host " + name);
	}

	/** check if the virtual resource is already deployed. If it is then update
	 *  its servers list. If it is not deployed, get a copy and deploy on host
	 */  
	void deployVirtualRes(String srName, String srvName) throws Exception {
		if (isVirtualResourceDeployed(srName) == true) {
			VirtualResource sr = getVirtualRes(srName);
			sr.addSoftServer(srvName);
		} else {
			// get new copy of virtual resource and add to list
			VirtualResource sr = ModelParameters.inputDistributedSystem.getVirtualRes(srName);
			VirtualResource srcpy = sr.getCopy();
			srcpy.addSoftServer(srvName);
			virResources.add(srcpy);
		}
	}

	/** check if the virtual resource is used by more than one server. If it is, then update its
	 * servers list. If it is used by any server then remove the virtual resource */
	void unDeployVirtualRes(String srName, String srvName) throws Exception {
		VirtualResource sr = getVirtualRes(srName);
		if (sr.isDeployedOnSoftServer(srvName) == true) {
			sr.removeSoftServer(srvName);
		}
		if (sr.softServers.size() == 0) {
			virResources.remove(sr);
		}
	}

	public boolean isVirtualResourceDeployed(String srName) {
		for (VirtualResource sr : virResources) {
			if (sr.name.compareToIgnoreCase(srName) == 0) {
				return true;
			}
		}
		return false;
	}

	public VirtualResource getVirtualRes(String srName) throws Exception {
		for (VirtualResource sr : virResources) {
			if (sr.name.compareToIgnoreCase(srName) == 0) {
				return sr;
			}
		}
		throw new Exception("virtualres " + srName + " is not deployed on Host " + name);
	}

	public ArrayList<SoftServer> getSoftServersList() {
		return (softServers);
	}

	public ArrayList<Device> getDevicesList() {
		return (devices);
	}

	public void setDeviceAsPowerManaged(String name) throws DeviceNotFoundException {
		if (isDeviceDeployed(name) == false) {
			Device dev = new Device(name);
			devices.add(dev);
		}
		// set device-type as power-managed with governor as
		// ONDEMAND[default gov for power-managed devices]
		Device device = getDevice(name);
		device.isDevicePowerManaged = true;
		device.governor = PowerManagementGovernor.ONDEMAND;

		// Set other PowerManaged attributes of this device
		for (Device dev : ModelParameters.inputDistributedSystem.powerManagedDevicePrototypes) {
			if (dev.name.equalsIgnoreCase(name)) // device type is found
			{
				// copy all attributes from device type object to this device
				// System.out.println("----attributes copying----");
				device.availabelSpeedLevels = dev.availabelSpeedLevels;
				device.powerConsumptionsLevels = dev.powerConsumptionsLevels;
				device.idlePower = dev.idlePower;
				device.deviceProbeInterval = dev.deviceProbeInterval;
				// set probe interval min & max
				device.downThreshold = dev.downThreshold;
				device.upThreshold = dev.upThreshold;

				// setting max_freq_index
				device.totalFrequencyLevels = dev.totalFrequencyLevels;

			}
		}
		ModelParameters.inputDistributedSystem.addPowerManagedDevices(device);
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
	 *  Set device governor UpThreshold. If utilization of device exceeds upthreshold
	 *
	 * device governor speedups device execution by reducing service times associated with this device.
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
		throw new Error("Attempt to modify the device probe_interval of host " + name + ", instead variable " + ((Device) dev).deviceProbeInterval
				+ " should be modified");
	}

	/** modify governor up_threshold of a PM device from setstmt */
	public void modifyGovUpThreshold(String device, double gov_up_threshold) throws DeviceNotFoundException {
		Object dev = getDevice(device);
		if (((Device) dev).count.name.compareToIgnoreCase("local") == 0) {
			((Device) dev).upThreshold = gov_up_threshold;
			return;
		}
		throw new Error("Attempt to modify the device up_threshold of host " + name + ", instead variable " + ((Device) dev).upThreshold
				+ " should be modified");
	}

	/** modify governor down_threshold of a PM device from setstmt */
	public void modifyGovDownThreshold(String device, double gov_down_threshold) throws DeviceNotFoundException {
		Object dev = getDevice(device);
		if (((Device) dev).count.name.compareToIgnoreCase("local") == 0) {
			((Device) dev).downThreshold = gov_down_threshold;
			return;
		}
		throw new Error("Attempt to modify the device down_threshold of host " + name + ", instead variable " + ((Device) dev).downThreshold
				+ " should be modified");
	}
}
