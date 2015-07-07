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
package perfcenter.analytical;

import java.util.ArrayList;

import perfcenter.baseclass.DeviceServiceTime;
import perfcenter.baseclass.Node;

/**
 * CompoundTask is made up of one or more subtasks. This class stores list of subtasks
 * 
 * @author
 */
public class CompoundTask {

	String name;
	String sceName;
	double arrivalRate = 0;
	double holdingTime = 0;
	double responseTime = 0;
	double throughput = 0;
	/**
	 * Number of invocations made by the calling server on the called server 
	 * within the same compound task. User calling a software server is not considered 
	 * an invocation here.<br>
	 * <p>
	 * Used for think time calculation.<br>
	 * <p>
	 * BHAVIN: This is not used currently, as the assumption made is that all tasks will have different names within a compound task.
	 */
	public int numInvocations = 0;
	public ArrayList<SubTask> subTasks = new ArrayList<SubTask>();

	/* Supriya: Added for closed arrival support */
	double thinkTime = 0;
	int NoOfUsers = 0;
	double probability = 0;
	double conditionalProbability = 0; /*
								 * stores the conditional probability of the compound task being invoked wrt the other compound tasks on the server
								 */

	String parentServer;
	String parentCTTask;
	double followingHoldingTime = 0;
	double preceedingHoldingTime = 0;
	String servername;// added by niranjan so as to know the server to which this compound task belongs to
	String ClassName;// added by niranjan so as to differentiate Compound Tasks in case of closed arrival
	boolean jobClassSet = false;

	public class PairValue {

		double interTime = 0;
		double changeProb = 0;
		String jobClassName;
		String Scename;

		public PairValue(CompoundTask ct) {
			jobClassName = ct.getname();
			Scename = ct.getScenarioName();
		}

		public void print() {
			
		}
	}

	public ArrayList<PairValue> pairTasks = new ArrayList<PairValue>();

	// public ArrayList<String> FollowingTasks = new ArrayList<String>();
	public void setNoOfUsers(int nou) {
		NoOfUsers = nou;
	}

	public int getNoOfUsers() {
		return NoOfUsers;
	}

	public void setThinkTime(double ttime) {
		thinkTime = ttime;
	}

	public double getThinkTime() {
		return thinkTime;
	}

	public void setThroughput(double tput) {
		throughput = tput;
	}

	public double getThroughput() {
		return throughput;
	}

	public void setprobability(double p) {
		probability = p;
	}

	public double getProbability() {
		return probability;
	}

	public void setname(String nm) {
		name = nm;
	}

	public String getname() {
		return name;
	}

	public void setParentServer(String ps) {
		parentServer = ps;
	}

	public String getParentServer() {
		return parentServer;
	}

	public void setParentTask(String pc) {
		parentCTTask = pc;
	}

	public String getParentTask() {
		return parentCTTask;
	}

	/* End Supriya: Added for closed arrival support */

	public void setArrRate(double arate) {
		arrivalRate = arate;
	}

	public double getArrRate() {
		return arrivalRate;
	}

	public void setHoldingTime(double htime) {
		holdingTime = htime;
	}

	public double getHoldingTime() {
		return holdingTime;
	}

	public void setResponseTime(double rtime) {
		responseTime = rtime;
	}

	public double getResponseTime() {
		return responseTime;
	}

	public String getScenarioName() {
		return sceName;
	}

	public class SubTask {

		String name;
		String servername;
		double arrivalRate = 0;
		double holdingTime = 0;
		double responseTime = 0;
		double probability = 0;

		public SubTask(String taskname, String servname, double arrate, double prob) {
			name = taskname;
			servername = servname;
			arrivalRate = arrate;
			probability = prob;
		}

		public void print() {
			System.out.println("   SubTask " + name + " server " + servername + " arrrate " + arrivalRate + " prob " + probability);
		}

		public String getServerName() {
			return servername;
		}

		public String getName() {
			return name;
		}
	}

	void addSubTask(Node n) {
		SubTask st = new SubTask(n.name, n.servername, n.arrate, n.prob.getValue());
		subTasks.add(st);
	}

	void print() {
		System.out.println("Start CompoundTask");
		System.out.println("name " + name);
		System.out.println("--PairTasks");
		for(PairValue pv : this.pairTasks) {
			System.out.println("------jobclass " + pv.jobClassName + "  scename " + pv.Scename);
		}
		System.out.println("--End PairTasks");
		System.out.println("Arrival Rate " + arrivalRate);
		System.out.println("Probablity  " + probability);
		System.out.println("--Conditional Probability: " + conditionalProbability);
		System.out.println("ParentServer " + parentServer);
		System.out.println("ParentTask " + parentCTTask);
		for (SubTask st : subTasks) {
			st.print();
		}
		System.out.println("End CompoundTask");
		System.out.println("=====================");
	}

	public String getName() {
		return name;
	}

	/* moved to perfanaclosed */

	public void calThinkTime(DistributedSystemAna ds) {
		/*
		 * think time = weighted sum intermediate time, steady state prob intermediate time = holding time of following tasks + holding time
		 * preceeding tasks + calling thread idle time + time spent by parent server on other tasks. pair prob. = prob of following task
		 */
		if (!(parentServer.compareToIgnoreCase("user") == 0)) {
			SoftServerAna parentServ = (SoftServerAna) (ds.getServer(parentServer));
			CompoundTask parentCT = parentServ.getCTask(parentCTTask, sceName, this.ClassName);
			double temp = 0;

			for (SubTask t : parentCT.subTasks) {
				temp = 0;
				if (t.name != name) {
					if (t.getServerName().compareTo(((SoftServerAna) parentServ).getName()) == 0) {

						// if it is a simple task on the same machine ..

						for (DeviceServiceTime dst : ((SoftServerAna) parentServ).getSimpleTask(t.getName()).deviceServiceTimes) {
							temp += dst.getResponseTime();
						}
					} // else get the compound task response time adding network
						// delay
					else {
						temp += ((SoftServerAna) parentServ).getCTask(t.getName(), sceName, parentCT.ClassName).getHoldingTime();
					}
				} else {
					preceedingHoldingTime = temp;
					temp = 0;
				}

			}
			followingHoldingTime = temp;

		}
		for (PairValue pair : pairTasks) {
			// pair.interTime = followingHoldingTime + get pair.jobClassName
			/*
			 * int time = following time + pair.preceedingholdtime + parentTasks + parentIdle pairProb = pair.prob
			 */
		}
	}

	/* added by niranjan this function generates another Compound Task of having same contents(i.e deep copy or clone of object) */

	CompoundTask deepCopy() {
		CompoundTask ct = new CompoundTask();
		ct.arrivalRate = this.arrivalRate;
		ct.ClassName = this.ClassName;
		ct.conditionalProbability = this.conditionalProbability;
		ct.followingHoldingTime = this.followingHoldingTime;
		ct.holdingTime = this.holdingTime;
		ct.name = this.name;
		ct.NoOfUsers = this.NoOfUsers;
		ct.numInvocations = this.numInvocations;
		for (PairValue pair : this.pairTasks) {
			ct.pairTasks.add(pair);
		}
		ct.parentCTTask = this.parentCTTask;
		ct.parentServer = this.parentServer;
		ct.preceedingHoldingTime = this.preceedingHoldingTime;
		ct.probability = this.probability;
		ct.responseTime = this.responseTime;
		ct.sceName = this.sceName;
		for (SubTask task : this.subTasks) {
			ct.subTasks.add(task);
		}
		ct.thinkTime = this.thinkTime;
		ct.throughput = this.throughput;
		ct.jobClassSet = this.jobClassSet;

		return ct;
	}
}