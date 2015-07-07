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

import org.apache.log4j.Logger;

import perfcenter.baseclass.enums.SchedulingPolicy;
import perfcenter.simulator.VirtualResSim;

/**
 * Defines virtual resource. A virtual resource can call another virtual resource, or real devices.
 * 
 * @author akhila
 */
public class VirtualResource extends QueuingResource {
	public String name;

	/** list of device and service times */
	public ArrayList<DeviceServiceTime> deviceSTimes = new ArrayList<DeviceServiceTime>();

	/** List of virtual resources */
	public ArrayList<String> virtRes = new ArrayList<String>();

	/** line number used by parser */
	int lineno;

	/** List of soft servers to which a virtual resource belongs */
	public ArrayList<String> softServers = new ArrayList<String>(); // made public by niranjan

	Logger logger = Logger.getLogger("VirtualRes");

	// number of instances of the queue server(virtual resource)
	public Variable count;

	// buffer size
	public Variable buffer;

	// scheduling policy
	public SchedulingPolicy pol;

	// task name to which this virtual resource belongs
	String taskName;

	// constructors
	public VirtualResource() {

	}

	public VirtualResource(String resname) {
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

	public void print() {
		System.out.println("Virtual Res : " + name);
		for (DeviceServiceTime servt : deviceSTimes) {
			servt.print();
		}
	}

	VirtualResource getCopy() {
		VirtualResource vrcpy = new VirtualResource(name);
		for (DeviceServiceTime dst : deviceSTimes) {
			DeviceServiceTime dstcpy = dst.getCopy();
			vrcpy.addDeviceServiceTime(dstcpy);
		}
		vrcpy.virtRes = virtRes;
		vrcpy.pol = pol;
		if (buffer.getName().compareToIgnoreCase("local") != 0) {
			vrcpy.buffer = buffer;
		} else {
			vrcpy.buffer.value = buffer.value;
		}
		if (count.getName().compareToIgnoreCase("local") != 0) {
			vrcpy.count = count;
		} else {
			vrcpy.count.value = count.value;
		}

		return vrcpy;
	}

	/** get a copy of virtual resource of type VirtualResSim */
	public VirtualResSim getCopySim() {
		VirtualResSim vrcpy = new VirtualResSim(name, count, buffer, pol);
		for (DeviceServiceTime dst : deviceSTimes) {
			DeviceServiceTime dstcpy = dst.getCopy();
			vrcpy.addDeviceServiceTime(dstcpy);
		}
		vrcpy.virtRes = virtRes;
		vrcpy.virtRes = virtRes;
		vrcpy.pol = pol;
		if (buffer.getName().compareToIgnoreCase("local") != 0) {
			vrcpy.buffer = buffer;
		} else {
			vrcpy.buffer.value = buffer.value;
		}
		if (count.getName().compareToIgnoreCase("local") != 0) {
			vrcpy.count = count;
		} else {
			vrcpy.count.value = count.value;
		}
		return vrcpy;
	}

	public void addVirtualRes(String name) {
		virtRes.add(name);
	}

	/** add devices required by this virtual resource */
	public void addDeviceAndServiceTime(String devicename, Distribution dist) {
		for (DeviceServiceTime st : deviceSTimes) {
			if (st.devName.compareToIgnoreCase(devicename) == 0) {
				st.dist = dist;
				return;
			}
		}
		DeviceServiceTime servt = new DeviceServiceTime(devicename, dist);
		deviceSTimes.add(servt);
	}

	public void addDeviceServiceTime(DeviceServiceTime dst) {
		deviceSTimes.add(dst);
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
	public String getNextDeviceName(int id) {
		if (id < deviceSTimes.size()) {
			return deviceSTimes.get(id).devName;
		}
		return null;
	}

	/** Get next virtual resource name from which current VirtualResource requires service. */
	public String getNextVirtualResName(int id) {
		if (id < virtRes.size()) {
			return virtRes.get(id);
		}
		return null;
	}

	/** get service time of a specified device */
	public Distribution getServiceTime(String devicename) {
		for (DeviceServiceTime st : deviceSTimes) {
			if (st.devName.compareToIgnoreCase(devicename) == 0) {
				return st.dist;
			}
		}
		return null;
	}
}