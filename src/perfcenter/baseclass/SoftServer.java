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
import java.util.Random;
import org.apache.log4j.Logger;

import perfcenter.baseclass.enums.SchedulingPolicy;

/**
 * Defines server . A server can be deployed on more than one host. Server has set of tasks.
 * 
 * @author akhila
 */
public class SoftServer extends QueuingResource {
	/** Server Name */
	public String name;

	/** number of threads */
	public Variable thrdCount;

	/** Size of thread buffer */
	public Variable thrdBuffer;

	/** Static size of the software server, nikhil */
	public Variable size;

	/** Size of each thread, nikhil */
	public Variable threadSize;

	/** Scheduling policy */
	public SchedulingPolicy schedp = SchedulingPolicy.FCFS;

	/** List of server tasks */
	public ArrayList<Task> tasks = new ArrayList<Task>();
	Logger logger = Logger.getLogger("SoftServer");

	/** List of hosts on which this server is deployed */
	public ArrayList<String> machines = new ArrayList<String>();

	public Random r;

	/** energy collector, used by code of RAM. Not sure how this is used. */
	public double totalServerEnergy; 
	
	public Metric availability = new Metric();
	
	public Metric ramUtil = new Metric();

	public SoftServer() {
		name = "undef";
		thrdCount = new Variable("local", 1);
		thrdBuffer = new Variable("local", 9999999);
		size = new Variable("local", 0);
		threadSize = new Variable("threadSize", 0.0);
		r = new Random();
		totalServerEnergy = 0.0;

	}

	public SoftServer(String servname) {
		name = servname;
		thrdCount = new Variable("local", 1);
		thrdBuffer = new Variable("local", 9999999);
		size = new Variable("local", 0);
		threadSize = new Variable("local", 0.0);
		r = new Random();
		totalServerEnergy = 0.0;

	}

	public String getName() {
		return name;
	}

	public int getNumCopies() {
		return machines.size();
	}

	public void setThreadCount(Variable count) {
		thrdCount = count;
	}

	public void setThreadBuffer(Variable count) {
		thrdBuffer = count;
	}

	public void setSchedPolicy(SchedulingPolicy sched) {
		schedp = sched;
	}
	
	public void setAvailability(double avail) {
		availability.setValue(avail);
	}

	public double getAvailability() {
		return availability.getValue();
	}

	SoftServer getCopy() {
		SoftServer sscpy = new SoftServer(name);
		sscpy.schedp = schedp;
		if (thrdBuffer.getName().compareToIgnoreCase("local") != 0) {
			sscpy.thrdBuffer = thrdBuffer;
		} else {
			sscpy.thrdBuffer.value = thrdBuffer.value;
		}
		if (thrdCount.getName().compareToIgnoreCase("local") != 0) {
			sscpy.thrdCount = thrdCount;
		} else {
			sscpy.thrdCount.value = thrdCount.value;
		}
		if (size.getName().compareToIgnoreCase("local") != 0) {
			sscpy.size = size;
		} else {
			sscpy.size.value = size.value;
		}
		if (threadSize.getName().compareToIgnoreCase("local") != 0) {
			sscpy.threadSize = threadSize;
		} else {
			sscpy.threadSize.value = threadSize.value;
		}

		for (Task task : tasks) {
			Task tcpy = task.getCopy();
			sscpy.addTask(tcpy);
		}
		for (String m : machines) {
			String mcpy = new String(m);
			sscpy.addMachine(mcpy);
		}
		return sscpy;
	}

	public void validate() {
		if (machines.isEmpty())
			logger.warn("Warning:Server \"" + name + "\" is not deployed on to any machine ");
		if (tasks.isEmpty())
			logger.warn("Warning:Server \"" + name + "\" does not have tasks defined ");
	}

	public void print() {
		System.out.println("ServerName " + name);
		System.out.println(" Thread Count " + thrdCount.name + ":" + thrdCount.value);
		System.out.println(" Thread Buffer " + thrdBuffer.name + ":" + thrdBuffer.value);
		System.out.println(" Static Size " + size.name + ":" + size.value);
		System.out.println(" SchedPolicy " + schedp.toString());
		System.out.println(" Server Tasks:");
		for (Task task : tasks) {
			task.print();
		}
		System.out.println(" Deployed on ");
		for (String machineName : machines) {
			if(ModelParameters.inputDistSys.isVM(machineName))
				System.out.println("   Virtual Machine: " + machineName);
			else if(ModelParameters.inputDistSys.isVM(machineName))
				System.out.println("   Physical Machine: " + machineName);
		}
	}

	/** add host name on which this server is deployed */
	public void addMachine(String name) {
		machines.add(name);
	}

	public void modifyThreadCount(double var1) {
		if (thrdCount.name.compareToIgnoreCase("local") == 0) {
			thrdCount.value = var1;
			return;
		}
		throw new Error("Attempt to modify the thread count of server " + name + ", instead variable " + thrdCount.name + " should be modified");
	}

	public void modifyThreadBuffer(double var1) {
		if (thrdBuffer.name.compareToIgnoreCase("local") == 0) {
			thrdBuffer.value = var1;
			return;
		}
		throw new Error("Attempt to modify the thread buffer of server " + name + ", instead variable " + thrdBuffer.name + " should be modified");
	}

	/** Get a simple Task object given its name */
	public Task getTaskObject(String name) {
		for (Task task : tasks) {
			if (task.name.compareToIgnoreCase(name) == 0)
				return task;
		}
		throw new Error(name + " is not Simple Task in server " + this.name);
	}

	/** Add a task to the list simpleTasks */
	public void addTask(Task task) {
		Task tcpy = task.getCopy();
		tasks.add(tcpy);
	}

	/** 
	 * Remove the host name. (called when server is un deployed from a host)
	 */
	public void removeMachine(String name) {
		machines.remove(name);
	}

	/**
	 * Checks if softserver has any tasks that require virtual resource.
	 * If there are then those virtual resources are deployed on the host
	 * @param h
	 * @param softResList
	 * @throws Exception
	 */
	void deploySoftResRecursive(Machine h, ArrayList<String> softResList) throws Exception {
		for (String sr : softResList) {
			// deploy the virtual resource
			h.deploySoftRes(sr, this.name);

			// check if the virtual res calls other virtual resource.
			SoftResource currSr = ModelParameters.inputDistSys.getSoftRes(sr);
			if (currSr.softRes.size() != 0) {
				deploySoftResRecursive(h, currSr.softRes);
			}
		}
	}

	/** Checks if softserver has any tasks that require virtual resource.
	 *  If it does then those virtual resources are undeployed from the host
	 */
	void undeployVirtResRecursive(Machine m, ArrayList<String> srList) throws Exception {
		for (String sr : srList) {
			// undeploy virtual resource
			m.unDeploySoftRes(sr, this.name);

			// check if the virtual resource calls other virtual resources
			SoftResource currsr = ModelParameters.inputDistSys.getSoftRes(sr);
			if (currsr.softRes.size() == 0) {
				undeployVirtResRecursive(m, currsr.softRes);
			}
		}
	}

	/** deploys virtual resource on the host.
	 * A virtual resource can call other virtual resources. hence this deployment is done recursively */
	public void deploySoftResOnHost(Machine h) throws Exception {
		for (Task t : tasks) {
			if (t.softRes.size() > 0) {
				deploySoftResRecursive(h, t.softRes);
			}
		}
	}

	/** undeploys virtual resource from the host. 
	 * A virtual resource can call other virtual resources. hence this undeployment is done recursively
	 */
	public void unDeploySoftResOnHost(Machine m) throws Exception {
		for (Task t : tasks) {
			if (t.softRes.size() > 0) {
				undeployVirtResRecursive(m, t.softRes);
			}
		}
	}
	
	public void setStaticSize(Variable v) {
		size = v;
	}

	public double getStaticSize() {
		return (size.value);
	}

	public void setThreadSize(Variable size) {
		threadSize = size;
	}

	public double getThreadSize() {
		return (threadSize.value);
	}

	public Double getTotalEnergyConsumption() {
		return totalServerEnergy;
	}
}