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

	/** Size of each request, nikhil */
	public Variable requestSize;

	/** Scheduling policy */
	public SchedulingPolicy schedp = SchedulingPolicy.FCFS;

	/** List of server tasks */
	public ArrayList<Task> simpleTasks = new ArrayList<Task>();
	Logger logger = Logger.getLogger("SoftServer");

	/** List of hosts on which this server is deployed */
	public ArrayList<String> hosts = new ArrayList<String>();

	public Random r;

	/** energy collector, used by code of RAM. Not sure how this is used. */
	public double totalServerEnergy; 

	public SoftServer() {
		name = "undef";
		thrdCount = new Variable("local", 1);
		thrdBuffer = new Variable("local", 9999999);
		size = new Variable("local", 0);
		threadSize = new Variable("threadSize", 0.0);
		requestSize = new Variable("requestSize", 0.0);
		r = new Random();
		totalServerEnergy = 0.0;

	}

	public SoftServer(String servname) {
		name = servname;
		thrdCount = new Variable("local", 1);
		thrdBuffer = new Variable("local", 9999999);
		size = new Variable("local", 0);
		threadSize = new Variable("local", 0.0);
		requestSize = new Variable("requestSize", 0.0);
		r = new Random();
		totalServerEnergy = 0.0;

	}

	public String getName() {
		return name;
	}

	public int getNumCopies() {
		return hosts.size();
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
		if (requestSize.getName().compareToIgnoreCase("local") != 0) {
			sscpy.requestSize = requestSize;
		} else {
			sscpy.requestSize.value = requestSize.value;
		}

		for (Task task : simpleTasks) {
			Task tcpy = task.getCopy();
			sscpy.addTask(tcpy);
		}
		for (String h : hosts) {
			String hcpy = new String(h);
			sscpy.addHost(hcpy);
		}
		return sscpy;
	}

	public void validate() {
		if (hosts.isEmpty())
			logger.warn("Warning:Server \"" + name + "\" is not deployed on to any host ");
		if (simpleTasks.isEmpty())
			logger.warn("Warning:Server \"" + name + "\" does not have tasks defined ");
	}

	public void print() {
		System.out.println("ServerName " + name);
		System.out.println(" Thread Count " + thrdCount.name + ":" + thrdCount.value);
		System.out.println(" Thread Buffer " + thrdBuffer.name + ":" + thrdBuffer.value);
		System.out.println(" Static Size " + size.name + ":" + size.value);
		System.out.println(" SchedPolicy " + schedp.toString());
		System.out.println(" Server Tasks:");
		for (Task task : simpleTasks) {
			task.print();
		}
		System.out.println(" Deployed on ");
		for (String host : hosts) {
			System.out.println("   Host " + host);
		}
	}

	/** add host name on which this server is deployed */
	public void addHost(String name) {
		hosts.add(name);
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
	public Task getSimpleTask(String name) {
		for (Task task : simpleTasks) {
			if (task.name.compareToIgnoreCase(name) == 0)
				return task;
		}
		throw new Error(name + " is not Simple Task");
	}

	/** Add a task to the list simpleTasks */
	public void addTask(Task task) {
		Task tcpy = task.getCopy();
		simpleTasks.add(tcpy);
	}

	/** 
	 * Remove the host name. (called when server is un deployed from a host)
	 */
	public void removeHost(String name) {
		hosts.remove(name);
	}

	/**
	 * Checks if softserver has any tasks that require virtual resource.
	 * If there are then those virtual resources are deployed on the host
	 * @param h
	 * @param vrList
	 * @throws Exception
	 */
	void deployVirtResRecursive(Host h, ArrayList<String> vrList) throws Exception {
		for (String vr : vrList) {
			// deploy the virtual resource
			h.deployVirtualRes(vr, this.name);

			// check if the virtual res calls other virtual resource.
			VirtualResource currvr = ModelParameters.inputDistributedSystem.getVirtualRes(vr);
			if (currvr.virtRes.size() != 0) {
				deployVirtResRecursive(h, currvr.virtRes);
			}
		}
	}

	/** Checks if softserver has any tasks that require virtual resource.
	 *  If it does then those virtual resources are undeployed from the host
	 */
	void undeployVirtResRecursive(Host h, ArrayList<String> vrList) throws Exception {
		for (String vr : vrList) {
			// undeploy virtual resource
			h.unDeployVirtualRes(vr, this.name);

			// check if the virtual resource calls other virtual resources
			VirtualResource currvr = ModelParameters.inputDistributedSystem.getVirtualRes(vr);
			if (currvr.virtRes.size() == 0) {
				undeployVirtResRecursive(h, currvr.virtRes);
			}
		}
	}

	/** deploys virtual resource on the host.
	 * A virtual resource can call other virtual resources. hence this deployment is done recursively */
	public void deployVirtualResOnHost(Host h) throws Exception {
		for (Task t : simpleTasks) {
			if (t.virRes.size() > 0) {
				deployVirtResRecursive(h, t.virRes);
			}

		}
	}

	/** undeploys virtual resource from the host. 
	 * A virtual resource can call other virtual resources. hence this undeployment is done recursively
	 */
	public void unDeployVirtualResOnHost(Host h) throws Exception {
		for (Task t : simpleTasks) {
			if (t.virRes.size() > 0) {
				undeployVirtResRecursive(h, t.virRes);
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

	public void setRequestSize(Variable size) {
		requestSize = size;
	}

	public double getRequestSize() {
		return (requestSize.value);
	}

	public Double getTotalEnergyConsumption() {
		return totalServerEnergy;
	}
}