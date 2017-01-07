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


/**
 * Defines the Task and operations done on task
 * 
 * @author akhila
 */
public class Task {
	/** Task name */
	public String name;
	
	/** Name of server to which this task belongs */
	public String softServerName;

	/** List of devices and service times required by this task. 
	 *  It is required that these devices are on the same host to which this task's software server belongs. 
	 **/
	public ArrayList<ServiceTime> subtaskServiceTimes = new ArrayList<ServiceTime>();

	/** List of soft resources required by this task */
	public ArrayList<String> softRes = new ArrayList<String>();

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
		return softServerName;
	}

	public Task getCopy() {
		Task tcpy = new Task(name, 0);
		for (ServiceTime st : subtaskServiceTimes) {
			ServiceTime stcpy = st.getCopy();
			tcpy.addSubTaskServiceTime(stcpy);
		}
		for (String sr : softRes) {
			String srcpy = new String(sr);
			tcpy.addSoftRes(srcpy);
		}
		return tcpy;
	}

	public void print() {
		System.out.println("Task " + name);
		for (ServiceTime servt : subtaskServiceTimes) {
			servt.print();
		}
		for (String sres : softRes) {
			System.out.println(" Soft Resource name :" + sres);
		}
	}

	public void validate() {
		if (softServerName == null) {
			logger.warn("Lineno" + lineno + ".Warning:Task \"" + name + "\" does not belong to any server ");
		}
		if (subtaskServiceTimes.isEmpty()) {
			if (name != "user")
				logger.warn("Lineno" + lineno + ". Warning:Task \"" + name + "\" does not have any  device");
		}

	}

	public void addSubTaskServiceTime(ServiceTime st) {// made public by niranjan
		subtaskServiceTimes.add(st);
		//System.out.println( "Task " + this.toString() + " " + this.name + " addSubTaskServiceTime 1 " + "Device Category:" + st.devCategory.name  + " basespeed:" + st.basespeed + " subtaskServiceTimes.size():" + this.subtaskServiceTimes.size());
	}

	
	public void createAndAddSubTaskServiceTime(String devcatname, Distribution dist) {
		 /*	for (DeviceServiceTime st : deviceServiceTimes) {
				if (st.devName.compareToIgnoreCase(devicename) == 0) {
					st.dist = dist;
					st.basespeed = basespeed.value;
					return;
				}
			} */
			DeviceCategory devcat = ModelParameters.inputDistSys.getDeviceCategory(devcatname);
			ServiceTime subtaskservt = new ServiceTime(devcat, dist);
			subtaskServiceTimes.add(subtaskservt);
			//System.out.println( "Task " + this.toString() + " " + this.name + " createAndAddSubTaskServiceTime 1 " + "Device Category:" + devcat.name  + " subtaskServiceTimes.size():" + this.subtaskServiceTimes.size());
	}
	
	public void createAndAddSubTaskServiceTime(String devcatname, Distribution dist, Variable basespeed) {
	 /*	for (DeviceServiceTime st : deviceServiceTimes) {
			if (st.devName.compareToIgnoreCase(devicename) == 0) {
				st.dist = dist;
				st.basespeed = basespeed.value;
				return;
			}
		} */
		DeviceCategory devcat = ModelParameters.inputDistSys.getDeviceCategory(devcatname);
		ServiceTime subtaskservt = new ServiceTime(devcat, dist, basespeed.value);
		subtaskServiceTimes.add(subtaskservt);
		//System.out.println( this.toString() + ":Task " + name  + " "+ this.name + " createAndAddSubTaskServiceTime 2 " + "Device Category:" + devcat.name + " basespeed:" + basespeed.value + " subtaskServiceTimes.size():" + this.subtaskServiceTimes.size());
	}
	//TODO: Make sure that this method is called with argument device category instead of device name.
	//TODO: URGENT: Change this method to modify service time based on sub task id.
	public void modifyServiceTime(String devName, Distribution _dist) {
		DeviceCategory devcat = ModelParameters.inputDistSys.getDeviceCategory(devName);
		for (ServiceTime st : subtaskServiceTimes) {
			if (st.devCategory == devcat) {
				if ((st.dist.value1_.name.compareToIgnoreCase("local") != 0) || (st.dist.value2_.name.compareToIgnoreCase("local") != 0)) {
					throw new Error("Lineno" + lineno + ". Attempt to modify the service time of task " + name
							+ ", instead variable should be modified");
				} else
					st.dist = _dist;
				return;
			}
		}
		throw new Error("Lineno" + lineno + ".Attempt to modify service time of non existing device category " + devcat + " of task " + name);
	}

	public void addSoftRes(String name) {
		softRes.add(name);
	}

	public void addSoftRes(SoftResource virres) {
		// softResDef.add(softres);
	}

	/** Add a server name to which the task belongs */
	public void addServer(String serv) {
		if (softServerName == null) {
			this.softServerName = serv;
		} else {
			throw new Error("Attempt to reassign Task " + name + "(" + softServerName + ")" + " to " + serv + " server");
		}
	}

	/** get the next device name from the device list */
	public DeviceCategory getNextDeviceCategory(int id) { //MAKECHANGE 
		if (id < subtaskServiceTimes.size()) {
			//System.out.println("returning next device category : "+ subtaskServiceTimes.get(id).devCategory.name);
			return subtaskServiceTimes.get(id).devCategory;
		}

		return null;

	}

	/** get next soft resource from the soft resource list */
	public String getNextSoftResName(int id) {
		if (id < softRes.size())
			return softRes.get(id);

		return null;
	}

	/** given the subtask id get its service time distribution */
	public Distribution getServiceTimeDist(int _stid) {
		//System.out.print("Task.getServiceTimeDist():" + this.toString() + "Task.subtaskServiceTimes.size():" + this.subtaskServiceTimes.size() );
		if(_stid < subtaskServiceTimes.size()){
			return subtaskServiceTimes.get(_stid).dist;
		}
		return null;
		//throw new Error("Sub task id is greater than total sub tasks. This should not happen. BUG !!");
		/*
		for (ServiceTime st : subtaskServiceTimes) {
			System.out.println("Subtask id:" + st.stid + "devCategory:" + st.devCategory);
			if (st.stid == _stid) {
				System.out.println("Subtask id:" + _stid + " Distribution:"+ st.dist.name_);
				return st.dist;
			}
		}
		*/
	}

	public ServiceTime getSubTaskServt(int _stid) {
		if(_stid < subtaskServiceTimes.size()){
			return subtaskServiceTimes.get(_stid);
		}
		return null;
		//throw new Error("Sub task id is greater than total sub tasks. This should not happen. BUG !!");
		/*
		for (ServiceTime st : subtaskServiceTimes) {
			if (st.stid == _stid) {
				return st;
			}
		}
		return null; */
	}

	/** initialize (used by analytical part) */
	public void initialize() {
		arrivalRate = 0;
		for (ServiceTime st : subtaskServiceTimes) {
			st.initialize();
		}
	}
}
