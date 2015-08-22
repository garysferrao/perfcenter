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

import java.util.HashMap;

import perfcenter.baseclass.Device;
import perfcenter.baseclass.Host;
import perfcenter.baseclass.SoftServer;
import perfcenter.baseclass.Task;
import perfcenter.baseclass.VirtualResource;
import perfcenter.baseclass.exception.DeviceNotFoundException;
import perfcenter.simulator.request.Request;

/**
 * Inherits from Host. Has some more functions for Simulation.
 * @author  akhila
 */
public class HostSim extends Host {

	HashMap<String, SoftServerSim> softServerMap = new HashMap<String, SoftServerSim>();
	HashMap<String, DeviceSim> deviceMap = new HashMap<String, DeviceSim>();
	HashMap<String, VirtualResSim> virtualResourceMap = new HashMap<String, VirtualResSim>();
	
	//Below data structures are used for Xen's Credit Scheduling Policy
	public int cap = 16000; //FIXME: No hard coding please
	public int deduction = 100; //FIXME: No hard coding please
	//Device name and their credits
	public HashMap<String, Integer> deviceCreditMap = new HashMap<String, Integer>(); 
	//Soft Server name and their status. 1 for "under" and 0 for "over"
	public HashMap<String, Integer> softServerCreditMap = new HashMap<String, Integer>(); 
	//Virtual Resource name and their credits
	public HashMap<String, Integer> virtResCreditMap = new HashMap<String, Integer>(); 
	
	//Device name and their status:1 for "under" and 0 for "over"
	public HashMap<String, Integer> deviceStatusMap = new HashMap<String, Integer>(); 
	//Softservername and their status. 1 for "under" and 0 for "over"
	public HashMap<String, Integer> softServerStatusMap = new HashMap<String, Integer>(); 
	//Virtual Resource name and their status:1 for "under" and 0 for "over"
	public HashMap<String, Integer> virtResStatusMap = new HashMap<String, Integer>();
	

	// constructor
	public HostSim(Host h, HashMap<String, SoftServerSim> softServerMap) {
		name = h.name;
		lan = h.lan;

		for (SoftServer s : h.softServers) {
			SoftServerSim softServerSim2 = softServerMap.get(s.name);
			softServers.add(softServerSim2);
			this.softServerMap.put(s.getName(), softServerSim2);
			
			softServerCreditMap.put(s.getName(), cap);
			softServerStatusMap.put(s.getName(), cap);
		}

		for (Device dev : h.devices) {
			DeviceSim devs = new DeviceSim(dev);
			devices.add(devs);
			deviceMap.put(devs.getDeviceName(), devs);
			
			deviceCreditMap.put(dev.getDeviceName(), cap);
			deviceStatusMap.put(dev.getDeviceName(), cap);
		}

		for (VirtualResource sr : h.virResources) {
			VirtualResSim srcpy = sr.getCopySim();
			virResources.add(srcpy);
			virtualResourceMap.put(srcpy.name, srcpy);
			
			virtResCreditMap.put(sr.name, cap);
			virtResStatusMap.put(sr.name, cap);
		}
		arrivalRate = 0;
	}
	
	/**
	 * Gets a DeviceSim object given the device name, from this HostSim.
	 * 
	 * This is the performance enhanced method for the simulation use, and it overrides the method in Host class.
	 */
	public DeviceSim getDevice(String dname) throws DeviceNotFoundException {
		DeviceSim device = deviceMap.get(dname);
		if (device != null) {
			return device;
		}
		throw new DeviceNotFoundException(" \"" + dname + "\" is not device for host " + name);
	}

	/**
	 * checks whether device is deployed on this host.
	 * 
	 * This is the performance enhanced method for the simulation use, and it overrides the method in Host class.
	 */
	public boolean isDeviceDeployed(String deviceName) {
		return deviceMap.get(deviceName) != null;
	}

	/**
	 * gets the server given name.
	 * 
	 * This is the performance enhanced method for the simulation use, and it overrides the method in Host class.
	 */
	public SoftServerSim getServer(String sname) {
		SoftServerSim softServer = softServerMap.get(sname);
		if (softServer != null) {
			return softServer;
		}
		throw new Error("\"" + sname + "\" is not Server on Host " + name);
	}

	/**
	 * Checks if the given name is a server name.
	 * 
	 * This is the performance enhanced method for the simulation use, and it overrides the method in Host class.
	 */
	public boolean isServerDeployed(String sname) {
		return softServerMap.get(sname) != null;
	}

	/**
	 * is this virtual resource deployed on host.
	 * 
	 * This is the performance enhanced method for the simulation use, and it overrides the method in Host class.
	 */
	public boolean isVirtualResourceDeployed(String srName) {
		return virtualResourceMap.get(srName) != null;
	}

	/**
	 * get the virtual resource deployed on this host.
	 * 
	 * This is the performance enhanced method for the simulation use, and it overrides the method in Host class.
	 */
	public VirtualResSim getVirtualRes(String srName) throws Exception {
		VirtualResSim virtualResource = virtualResourceMap.get(srName);
		if (virtualResource != null) {
			return virtualResource;
		}
		throw new Exception("virtualres " + srName + " is not deployed on Host " + name);
	}

	/**
	 *  get lower virtual resource layer name. and offer request to it.
	 * @param req
	 * @param currTime
	 * @return
	 * @throws Exception
	 */
	public boolean offeredRequestToNextLayerVirtRes(Request req, double currTime) throws Exception {
		String nextLayerVRName = req.getNextLayerVRName();
		if (nextLayerVRName == null) {
			return false;
		}

		req.virtResName = nextLayerVRName;
		req.setRequestFromVirtRes();
		// reset the indices
		req.setDeviceIndex(0, "HostSim:offeredRequestToNextLayerVirtRes");
		req.virtualResIndex = 0;

		// offer request to the virtual resource
		VirtualResSim sr = (VirtualResSim) getVirtualRes(req.virtResName);
		sr.enqueue(req, currTime);
		return true;

	}

	// offer request to virtual resource.this is to support
	// implementing virtual resource layers
	public boolean offeredRequestToVirtualRes(Request req, double currTime) throws Exception {
		String virtResName = null;
		if (req.virtResStack.size() == 1) {
			// get next virtual resource name from the task
			virtResName = req.getVirtualResourceNameFromTask(); // t.getNextVirtualResName(req.virtualResIndex);
		} else {
			// get next virtual resource name from the virtual resource
			virtResName = req.getVirtualResourceNameFromVirRes();
		}

		// no virtual res found return false
		if (virtResName == null) {
			return false;
		}

		req.setRequestFromVirtRes();
		req.virtResName = virtResName;
		// reset the device index
		req.setDeviceIndex(0, "HostSim:offeredRequestToVirtualRes");

		// get host and virtual resource and enqueue request to that virtual res
		req.hostObject.getVirtualRes(req.virtResName).enqueue(req, currTime);
		return true;
	}

	// offer request to next device
	public boolean offeredRequestToNextDevice(Request req, double currTime) throws Exception {
		try { // get device name
			String devName = req.getNextDeviceName();
			if (devName == null) {
				return false;
			}
			req.devName = devName;

			// get host and device
			DeviceSim dev = req.hostObject.getDevice(req.devName);

			// get the service time for the device
			if (req.isRequestFromTask()) {
				Task t = ((SoftServerSim) getServer(req.softServName)).getSimpleTask(req.taskName);

				// set the value for total service demand on hw resource
				req.serviceTimeRemaining = t.getServiceTime(req.devName).nextRandomVal(dev.speedUpFactor.getValue());
			} else if (req.isRequestFromVirtRes()) {
				VirtualResSim sr = (VirtualResSim) getVirtualRes(req.virtResName);
				// set the value for total service demand on hw resource
				req.serviceTimeRemaining = sr.getServiceTime(req.devName).nextRandomVal(dev.speedUpFactor.getValue());
			}

			// add to request to device queue
			dev.enqueue(req, currTime);
			return true;
		} catch (DeviceNotFoundException e) {
			req.setDeviceIndex(req.getDeviceIndex() + 1, "HostSim:offeredRequestToNextDevice:catch");
			return offeredRequestToNextDevice(req, currTime);
		}
	}

}
