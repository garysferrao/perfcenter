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

import perfcenter.baseclass.Device;
import perfcenter.baseclass.DeviceCategory;
import perfcenter.baseclass.PhysicalMachine;
import perfcenter.baseclass.SoftServer;
import perfcenter.baseclass.Task;
//import perfcenter.baseclass.enums.DeviceCategory;
import perfcenter.baseclass.SoftResource;
import perfcenter.baseclass.exception.DeviceNotFoundException;
import perfcenter.simulator.request.Request;

/**
 * Inherits from Machine. Has some more functions for Simulation.
 * @author  akhila
 */
public class PhysicalMachineSim extends PhysicalMachine {
 
	HashMap<String, SoftServerSim> softServerMap = new HashMap<String, SoftServerSim>();
	HashMap<String, DeviceSim> deviceMap = new HashMap<String, DeviceSim>();
	HashMap<String, SoftResSim> virtualResourceMap = new HashMap<String, SoftResSim>();
	
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
	public PhysicalMachineSim(PhysicalMachine m, HashMap<String, SoftServerSim> softServerMap) {
		name = m.name;
		lan = m.lan;

		for (SoftServer s : m.softServers) {
			SoftServerSim softServerSim2 = softServerMap.get(s.name);
			softServers.add(softServerSim2);
			this.softServerMap.put(s.getName(), softServerSim2);
			
			softServerCreditMap.put(s.getName(), cap);
			softServerStatusMap.put(s.getName(), cap);
		}

		for (Device dev : m.devices) {
			//System.out.println("In MachineSim constructor:Device Name:" + dev.name + " basespeed:" + dev.basespeed);
			DeviceSim devs = new DeviceSim(dev);
			devices.add(devs);
			deviceMap.put(devs.getDeviceName(), devs);
			
			deviceCreditMap.put(dev.getDeviceName(), cap);
			deviceStatusMap.put(dev.getDeviceName(), cap);
		}

		for (SoftResource sr : m.softResources) {
			SoftResSim srcpy = sr.getCopySim();
			softResources.add(srcpy);
			virtualResourceMap.put(srcpy.name, srcpy);
			
			virtResCreditMap.put(sr.name, cap);
			virtResStatusMap.put(sr.name, cap);
		}
		arrivalRate = 0;
	}
	
	/**
	 * Gets a DeviceSim object given the device name, from this MachineSim.
	 * 
	 * This is the performance enhanced method for the simulation use, and it overrides the method in Machine class.
	 */
	public DeviceSim getDevice(String dname) throws DeviceNotFoundException {
		DeviceSim device = deviceMap.get(dname);
		if (device != null) {
			return device;
		}
		throw new DeviceNotFoundException(" \"" + dname + "\" is not device for machine " + name);
	}

	/**
	 * checks whether device is deployed on this machine.
	 * 
	 * This is the performance enhanced method for the simulation use, and it overrides the method in Machine class.
	 */
	public boolean isDeviceDeployed(String deviceName) {
		return deviceMap.get(deviceName) != null;
	}

	/**
	 * gets the server given name.
	 * 
	 * This is the performance enhanced method for the simulation use, and it overrides the method in Machine class.
	 */
	public SoftServerSim getServer(String sname) {
		SoftServerSim softServer = softServerMap.get(sname);
		
		if (softServer != null) {
			return softServer;
		}
		throw new Error("\"" + sname + "\" is not Server on Machine " + name);
	}

	/**
	 * Checks if the given name is a server name.
	 * 
	 * This is the performance enhanced method for the simulation use, and it overrides the method in Machine class.
	 */
	public boolean isServerDeployed(String sname) {
		return softServerMap.get(sname) != null;
	}

	/**
	 * is this virtual resource deployed on machine.
	 * 
	 * This is the performance enhanced method for the simulation use, and it overrides the method in Machine class.
	 */
	public boolean isSoftResourceDeployed(String srName) {
		return virtualResourceMap.get(srName) != null;
	}

	/**
	 * get the virtual resource deployed on this machine.
	 * 
	 * This is the performance enhanced method for the simulation use, and it overrides the method in Machine class.
	 */
	public SoftResSim getSoftRes(String srName) throws Exception {
		SoftResSim virtualResource = virtualResourceMap.get(srName);
		if (virtualResource != null) {
			return virtualResource;
		}
		throw new Exception("virtualres " + srName + " is not deployed on Machine " + name);
	}

	/**
	 *  get lower virtual resource layer name. and offer request to it.
	 * @param req
	 * @param currTime
	 * @return
	 * @throws Exception
	 */
	public boolean offerRequestToNextLayerSoftRes(Request req, double currTime) throws Exception {
		String nextLayerVRName = req.getNextLayerVRName();
		if (nextLayerVRName == null) {
			return false;
		}

		req.softResName = nextLayerVRName;
		req.setRequestFromVirtRes();
		// reset the indices
		req.setSubTaskIdx(0, "MachineSim:offeredRequestToNextLayerVirtRes");
		req.softResIdx = 0;

		// offer request to the virtual resource
		SoftResSim sr = (SoftResSim) getSoftRes(req.softResName);
		sr.enqueue(req, currTime);
		return true;

	}

	// offer request to virtual resource.this is to support
	// implementing virtual resource layers
	public boolean offerRequestToSoftRes(Request req, double currTime) throws Exception {
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
		req.softResName = virtResName;
		// reset the device index
		req.setSubTaskIdx(0, "MachineSim:offeredRequestToVirtualRes");

		// get machine and virtual resource and enqueue request to that virtual res
		req.machineObject.getSoftRes(req.softResName).enqueue(req, currTime);
		return true;
	}

	// offer request to next device on the same software server. 
	public boolean offerReqToNextDevice(Request req, double currTime) throws Exception {
		try { // get device name
			String devName = req.getNextDeviceName();
			if (devName == null) {
				return false;
			}
			req.devName = devName;

			// get machine and device
			DeviceSim dev = req.machineObject.getDevice(req.devName);
			// get the service time for the device
			if (req.isRequestFromTask()) {
				Task t = ((SoftServerSim) getServer(req.softServName)).getTaskObject(req.taskName);
				//System.out.println("Machine:" + req.machineRef.name + t.name+"Request SubTask Index:" + req.getSubTaskIdx() );
				// set the value for total service demand on hw resource
				req.serviceTimeRemaining = t.getServiceTimeDist(req.getSubTaskIdx()).nextRandomVal(dev.speedUpFactor.getValue());
			} else if (req.isRequestFromSoftRes()) {
				SoftResSim sr = (SoftResSim) getSoftRes(req.softResName);
				// set the value for total service demand on hw resource
				req.serviceTimeRemaining = sr.getServiceTimeDist(req.softResIdx).nextRandomVal(dev.speedUpFactor.getValue());
			}

			// add to request to device queue
			//System.out.println("MachineSim.offerReqToNextDevice():Enqueuing request, name of device:" + this.name);
			dev.enqueue(req, currTime);
			return true;
		} catch (DeviceNotFoundException e) {
			req.setSubTaskIdx(req.getSubTaskIdx() + 1, "MachineSim:offeredRequestToNextDevice:catch");
			return offerReqToNextDevice(req, currTime);
		}
	}
	
}
