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

/**
 * Defines the Task and operations done on task
 * 
 * @author akhila
 */
public class Task {
	/** Task name */
	public String name;

	/** Name of server to which this task belongs */
	public String server;

	/** list of devices and service times required by his task */
	public ArrayList<DeviceServiceTime> deviceServiceTimes = new ArrayList<DeviceServiceTime>();

	/** List of virtual resources required by this task */
	public ArrayList<String> virRes = new ArrayList<String>();

	/** Arrival rate to the task. used by analytical part for open analysis */
	double arrivalRate = 0;

	/** line no used by parser print error messages */
	int lineno;

	Logger logger = Logger.getLogger("Task");

	public Task(String taskname, int line) {
		name = taskname;
		lineno = line;
	}

	public void setArrRate(double arate) {
		arrivalRate = arate;
	}

	public double getArrRate() {
		return arrivalRate;
	}

	public String getServerName() {
		return server;
	}

	public Task getCopy() {
		Task tcpy = new Task(name, 0);
		for (DeviceServiceTime dst : deviceServiceTimes) {
			DeviceServiceTime dstcpy = dst.getCopy();
			tcpy.addDeviceServiceTime(dstcpy);
		}
		for (String sr : virRes) {
			String srcpy = new String(sr);
			tcpy.addVirtualRes(srcpy);
		}
		return tcpy;
	}

	public void print() {
		System.out.println("Task : " + name);
		for (DeviceServiceTime servt : deviceServiceTimes) {
			servt.print();
		}
		for (String sres : virRes) {
			System.out.println(" Virtual Resource name :" + sres);
		}
	}

	public void validate() {
		if (server == null) {
			logger.warn("Lineno" + lineno + ".Warning:Task \"" + name + "\" does not belong to any server ");
		}
		if (deviceServiceTimes.isEmpty()) {
			if (name != "user")
				logger.warn("Lineno" + lineno + ". Warning:Task \"" + name + "\" does not have any  device");
		}

	}

	public void addDeviceServiceTime(DeviceServiceTime dst) {// made public by niranjan
		deviceServiceTimes.add(dst);
	}

	public void addDeviceAndServiceTime(String devicename, Distribution dist) {
		for (DeviceServiceTime st : deviceServiceTimes) {
			if (st.devName.compareToIgnoreCase(devicename) == 0) {
				st.dist = dist;
				return;
			}
		}
		DeviceServiceTime servt = new DeviceServiceTime(devicename, dist);
		deviceServiceTimes.add(servt);
	}

	public void modifyServiceTime(String devicename, Distribution dist) {
		for (DeviceServiceTime st : deviceServiceTimes) {
			if (st.devName.compareToIgnoreCase(devicename) == 0) {
				if ((st.dist.value1_.name.compareToIgnoreCase("local") != 0) || (st.dist.value2_.name.compareToIgnoreCase("local") != 0)) {
					throw new Error("Lineno" + lineno + ". Attempt to modify the service time of task " + name
							+ ", instead variable should be modified");
				} else
					st.dist = dist;
				return;
			}
		}
		throw new Error("Lineno" + lineno + ".Attempt to modify service time of non existing device " + devicename);
	}

	public void addVirtualRes(String name) {
		virRes.add(name);
	}

	public void addVirtualRes(VirtualResource virres) {
		// softResDef.add(softres);
	}

	/** Add a server name to which the task belongs */
	public void addServer(String serv) {
		if (server == null) {
			this.server = serv;
		} else {
			throw new Error("Attempt to reassign Task " + name + "(" + server + ")" + " to " + serv + " server");
		}
	}

	/** get the next device name from the device list */
	public String getNextDeviceName(int id) {
		if (id < deviceServiceTimes.size()) {
			// System.out.println("returning next device : "+deviceSTimes.get(id).devName);
			return deviceServiceTimes.get(id).devName;
		}

		return null;

	}

	/** get next virtual resource from the virtual res list */
	public String getNextVirtualResName(int id) {
		if (id < virRes.size())
			return virRes.get(id);

		return null;
	}

	/** given the device name get its service time */
	public Distribution getServiceTime(String devicename) {
		for (DeviceServiceTime st : deviceServiceTimes) {
			if (st.devName.compareToIgnoreCase(devicename) == 0) {
				return st.dist;
			}
		}
		return null;
	}

	public DeviceServiceTime getDevice(String devicename) {
		for (DeviceServiceTime st : deviceServiceTimes) {
			if (st.devName.compareToIgnoreCase(devicename) == 0) {
				return st;
			}
		}
		return null;
	}

	/** initialize (used by analytical part) */
	public void initialize() {
		arrivalRate = 0;
		for (DeviceServiceTime st : deviceServiceTimes) {
			st.initialize();
		}
	}
}
