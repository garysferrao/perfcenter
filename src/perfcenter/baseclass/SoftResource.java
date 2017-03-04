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
package perfcenter.baseclass;

import java.util.ArrayList;
import perfcenter.baseclass.enums.DeviceType;

import org.apache.log4j.Logger;

import perfcenter.baseclass.enums.SchedulingPolicy;
import perfcenter.simulator.SoftResSim;

/**
 * Defines soft resource. A virtual resource can call another soft resource, or real devices.
 * 
 * @author akhila
 */
public class SoftResource extends QueuingResource {
	public String name;

	/** list of device and service times */
	public ArrayList<ServiceTime> deviceServiceTimes = new ArrayList<ServiceTime>();

	/** List of virtual resources */ // Why is it here? In single class, there should be only one software resource
	public ArrayList<String> softRes = new ArrayList<String>();

	/** line number used by parser */
	int lineno;

	/** List of soft servers to which a virtual resource belongs */
	public ArrayList<String> softServers = new ArrayList<String>(); // made public by niranjan

	Logger logger = Logger.getLogger("SoftRes");

	// number of instances of the queue server(virtual resource)
	public Variable count;

	// buffer size
	public Variable buffer;

	// scheduling policy
	public SchedulingPolicy pol;

	// task name to which this virtual resource belongs
	String taskName;

	// constructors
	public SoftResource() {

	}

	public SoftResource(String resname) {
		count = new Variable("local", 1);
		buffer = new Variable("local", 99999);
		pol = SchedulingPolicy.FCFS;
		name = resname;
	}

	public void setCount(Variable c) {
		count = c;
	}

	public void setBuffer(Variable b) {
		buffer = b;
	}

	public void setSchedPolicy(SchedulingPolicy sp) {
		pol = sp;
	}

	public String toString() {
		String softRes = "Soft Res:" + name + "\n";
		for (ServiceTime servt : deviceServiceTimes) {
			softRes += servt.toString();
		}
		softRes += "\n";
		return softRes;
	}

	SoftResource getCopy() {
		SoftResource srcpy = new SoftResource(name);
		for (ServiceTime dst : deviceServiceTimes) {
			ServiceTime dstcpy = dst.getCopy();
			srcpy.addDeviceServiceTime(dstcpy);
		}
		srcpy.softRes = softRes;
		srcpy.pol = pol;
		if (buffer.getName().compareToIgnoreCase("local") != 0) {
			srcpy.buffer = buffer;
		} else {
			srcpy.buffer.value = buffer.value;
		}
		if (count.getName().compareToIgnoreCase("local") != 0) {
			srcpy.count = count;
		} else {
			srcpy.count.value = count.value;
		}

		return srcpy;
	}

	/** get a copy of virtual resource of type VirtualResSim */
	public SoftResSim getCopySim() {
		SoftResSim srcpy = new SoftResSim(name, count, buffer, pol);
		for (ServiceTime dst : deviceServiceTimes) {
			ServiceTime dstcpy = dst.getCopy();
			srcpy.addDeviceServiceTime(dstcpy);
		}
		srcpy.softRes = softRes;
		srcpy.softRes = softRes;
		srcpy.pol = pol;
		if (buffer.getName().compareToIgnoreCase("local") != 0) {
			srcpy.buffer = buffer;
		} else {
			srcpy.buffer.value = buffer.value;
		}
		if (count.getName().compareToIgnoreCase("local") != 0) {
			srcpy.count = count;
		} else {
			srcpy.count.value = count.value;
		}
		return srcpy;
	}

	public void addSoftRes(String name) {
		softRes.add(name);
	}
	
	/** add devices required by this virtual resource */
	public void addDeviceAndServiceTime(String devcatname, Distribution dist) {
	/*	for (SubtaskServiceTime st : deviceServiceTimes) { //MAKECHANGE
			if (st.devName.compareToIgnoreCase(devicename) == 0) {
				st.dist = dist;
				st.basespeed = basespeed.value;
				return;
			}
		}
	*/
		DeviceCategory devcat = ModelParameters.inputDistSys.getDeviceCategory(devcatname);
		ServiceTime servt = new ServiceTime(devcat, dist);
		deviceServiceTimes.add(servt);
	}

	/** add devices required by this virtual resource */
	public void addDeviceAndServiceTime(String devcatname, Distribution dist, Variable basespeed) {
	/*	for (SubtaskServiceTime st : deviceServiceTimes) { //MAKECHANGE
			if (st.devName.compareToIgnoreCase(devicename) == 0) {
				st.dist = dist;
				st.basespeed = basespeed.value;
				return;
			}
		}
	*/
		DeviceCategory devcat = ModelParameters.inputDistSys.getDeviceCategory(devcatname);
		ServiceTime servt = new ServiceTime(devcat, dist, basespeed.value);
		deviceServiceTimes.add(servt);
	}

	public void addDeviceServiceTime(ServiceTime dst) {
		deviceServiceTimes.add(dst);
	}

	// Add soft server which has tasks calling this resource
	void addSoftServer(String ssname) {
		// check if softserver name is in the list. if it is not present add to the list
		for (String ss : softServers) {
			if (ss.compareToIgnoreCase(ssname) == 0) {
				return;
			}
		}
		softServers.add(ssname);
	}

	void removeSoftServer(String ssname) {
		softServers.remove(ssname);
	}

	/** check whether virtual resource is deployed on soft server */
	boolean isDeployedOnSoftServer(String ssname) {
		for (String softServerName : softServers) {
			if (softServerName.compareToIgnoreCase(ssname) == 0) {
				return true;
			}
		}
		return false;
	}

	/** Get next device name from which VirtualResource requires service. */
	public DeviceCategory getNextDeviceCategory(int id) {
		if (id < deviceServiceTimes.size()) {
				return deviceServiceTimes.get(id).devCategory;
			}
		return null;
	}

	/** Get next virtual resource name from which current VirtualResource requires service. */
	public String getNextSoftResName(int id) {
		if (id < softRes.size()) {
			return softRes.get(id);
		}
		return null;
	}

	/** get service time distribution of a specified sub task id */
	public Distribution getServiceTimeDist(int _stid) {
		if(_stid < deviceServiceTimes.size()){
			return deviceServiceTimes.get(_stid).dist;
		}
		throw new Error("Device Index is greater than number of devices to be used for service in SoftResource " + name + ". This should not happen. BUG");
	}
}
