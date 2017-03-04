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

import static perfcenter.simulator.DistributedSystemSim.computeConfIvalForMetric;

import java.util.ArrayList;

import perfcenter.baseclass.Device;
import perfcenter.baseclass.ModelParameters;
import perfcenter.baseclass.PhysicalMachine;
import perfcenter.baseclass.SoftServer;
import perfcenter.baseclass.Task;
//import perfcenter.baseclass.enums.DeviceCategory;
import perfcenter.baseclass.SoftResource;
import perfcenter.baseclass.exception.DeviceNotFoundException;
import perfcenter.simulator.request.Request;
import perfcenter.simulator.metric.TimeAverageMetric;
import perfcenter.simulator.queue.QueueSim;

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
	
	/* This stores overall ram utilization of host machine 
	 * Ram Utilization of host = Sum of ram utilization of all softservers deployed on it
	 */
	public TimeAverageMetric ramUtilSim;
	
	/* See vmRamUtil in PhysicalMachine
	 */
	public HashMap<String, TimeAverageMetric> vmRamUtilSim;
	
	public double currRamUtil;
	
	public HashMap<String, Double> currVmRamUtil;
	
	// constructor
	public PhysicalMachineSim(PhysicalMachine m, HashMap<String, SoftServerSim> softServerMap) {
		name = m.name;
		lan = m.lan;
		for (SoftServer s : m.softServers.values()) {
			SoftServerSim softServerSim2 = softServerMap.get(s.name);
			softServers.put(softServerSim2.getName(), softServerSim2);
			this.softServerMap.put(s.getName(), softServerSim2);
			
		}

		for (Device dev : m.devices.values()) {
			DeviceSim devs = new DeviceSim(dev);
			devices.put(devs.name, devs);
			deviceMap.put(devs.getDeviceName(), devs);
		}

		for (SoftResource sr : m.softResources.values()) {
			SoftResSim srcpy = sr.getCopySim();
			softResources.put(srcpy.name, srcpy);
			virtualResourceMap.put(srcpy.name, srcpy);
		}
		arrivalRate = 0;
		
		if(m.vservers != null){
			vservers = new HashMap<String, ArrayList<SoftServer> >();
			for(String vmname : m.vservers.keySet()){
				ArrayList<SoftServer> temp = new ArrayList<SoftServer>();
				for(SoftServer srvr : m.vservers.get(vmname)){
					temp.add((SoftServer)softServerMap.get(srvr.name));
				}
				vservers.put(vmname, temp);
			}
		}
		if(m.serversDeployedOnVm != null){
			serversDeployedOnVm = new HashMap<String, ArrayList<SoftServer> >();
			for(String vmname : m.serversDeployedOnVm.keySet()){
				ArrayList<SoftServer> temp = new ArrayList<SoftServer>();
				for(SoftServer srvr : m.serversDeployedOnVm.get(vmname)){
					temp.add((SoftServer)softServerMap.get(srvr.name));
				}
				serversDeployedOnVm.put(vmname, temp);
			}
		}
		ramUtilSim = new TimeAverageMetric(0.95); 
		
		if(m.avgVmRamUtils != null){
			avgVmRamUtils = m.avgVmRamUtils;
			vmRamUtilSim  = new HashMap<String, TimeAverageMetric>();
			for(String vmname : m.avgVmRamUtils.keySet()){
				vmRamUtilSim.put(vmname, new TimeAverageMetric(0.95));
			}
			
			currVmRamUtil = new HashMap<String, Double>();
		}
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
		PhysicalMachineSim machineObject = SimulationParameters.distributedSystemSim.machineMap.get(req.machineName);
		machineObject.getSoftRes(req.softResName).enqueue(req, currTime);
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
			DeviceSim dev = SimulationParameters.distributedSystemSim.machineMap.get(req.machineName).getDevice(req.devName);
			// get the service time for the device
			if (req.isRequestFromTask()) {
				Task t = ((SoftServerSim) getServer(req.softServName)).getTaskObject(req.taskName);
				// set the value for total service demand on hw resource
				req.serviceTimeRemaining = t.getServiceTimeDist(req.getSubTaskIdx()).nextRandomVal(dev.speedUpFactor.getValue());
			} else if (req.isRequestFromSoftRes()) {
				SoftResSim sr = (SoftResSim) getSoftRes(req.softResName);
				// set the value for total service demand on hw resource
				req.serviceTimeRemaining = sr.getServiceTimeDist(req.softResIdx).nextRandomVal(dev.speedUpFactor.getValue());
			}

			// add to request to device queue
			dev.enqueue(req, currTime);
			return true;
		} catch (DeviceNotFoundException e) {
			req.setSubTaskIdx(req.getSubTaskIdx() + 1, "MachineSim:offeredRequestToNextDevice:catch");
			return offerReqToNextDevice(req, currTime);
		}
	}
	
	public void updateRamUtil(Request req){
		double ramutil = 0.0;
		for(SoftServerSim srvrsim : softServerMap.values()){
			ramutil += srvrsim.getCurrRamUtil();
		}
		currRamUtil = ramutil;
		ramUtilSim.recordValue(req, ramutil);
		if(vservers != null){
			for(String vmname : vservers.keySet()){
				double temp = 0.0;
				for(SoftServer server : vservers.get(vmname)){
					temp += softServerMap.get(server.name).getCurrRamUtil();
				}
				for(SoftServer server : serversDeployedOnVm.get(vmname)){
					temp += softServerMap.get(server.name).getCurrRamUtil();
				}
				currVmRamUtil.put(vmname, temp);
				vmRamUtilSim.get(vmname).recordValue(req, temp);
			}
		}
	}
	
	public void clearValuesButKeepConfIvals(){
		ramUtilSim.clearValuesButKeepConfInts();
		if(vmRamUtilSim != null){
			for(String vmname : vmRamUtilSim.keySet()){
				vmRamUtilSim.get(vmname).clearValuesButKeepConfInts();
			}
		}
	}
	
	public void computeConfIvalsAtEndOfRepl() {
		computeConfIvalForMetric(avgRamUtil, ramUtilSim);
		if(vmRamUtilSim != null){
			for(String vmname : vmRamUtilSim.keySet()){
				computeConfIvalForMetric(avgVmRamUtils.get(vmname), vmRamUtilSim.get(vmname));
			}
		}
	 }
	
	void recordCISampleAtTheEndOfSimulation() {
		updateRamUtil(null);
		for (int slot = 0; slot < ModelParameters.intervalSlotCount; slot++) {
			ramUtilSim.recordCISample(slot);
			if(vmRamUtilSim != null){
				for(String vmname : vmRamUtilSim.keySet()){
					vmRamUtilSim.get(vmname).recordCISample(slot);
				}
			}
		}
	}
}
