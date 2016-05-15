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
/**
 * All Simulation parameters in one class
 * @author  akhila
 */
package perfcenter.simulator;

import java.util.HashMap;
import java.util.PriorityQueue;
import java.util.Random;

import perfcenter.baseclass.ModelParameters;
import perfcenter.baseclass.Scenario;
import perfcenter.simulator.request.Request;

/**
 * This class contains actual book-keepings done during simulation. This is mostly accessed in a static way.
 */
public class SimulationParameters {

	/** list of requests entering the system */
	public static HashMap<Integer, Request> requestMap = new HashMap<Integer, Request>();

	/** list of events to be processed by the system */
	public static PriorityQueue<Event> eventQueue;

	/** currentTime=time since the beginning of simulation */
	public static double currTime = 0.0f;

	/** has the complete state of simulation */
	public static DistributedSystemSim distributedSystemSim;

	public static int totalReqArrived = 0;

	/** parameters and data structures for confidence interval calculation */
	public static int replicationNo = 0; //changed from -1 to 1 BHAVIN
											// changed from 1 to 0 nadeesh

	/** current sample number for the current simulation run */
//	public static int sampleNumber = 1;

	/** generates id for every new request created. akhila */
	public static int reqIdGenerator = 0;

	/**************************************************************************/
	/** keeps track of current event being executed. rakesh */
	public static Event currEventBeingHandled = null;

	/**************************************************************************/
	/** keep track of last scenario arrival event */
	public static double lastScenarioArrivalTime = 0.0f;

	//CHECK: why ExponentialDistribution is here
	/** Distribution */
	public static ExponentialDistribution exp = new ExponentialDistribution();

	/** collect the energy consumption values for each run */
//	public static ArrayList<Double> energyArray = new ArrayList<Double>();

	/** Static counter for changing arrival rate */
	private static int ivalSlotCntr = 0;
	
	private static double[] ivalSlotRunTimes = new double[1000];// ARCHITECTURE: change this to ArrayList
	private static double lastSlotChangeTime = 0;

	/** 
	 * This flag indicates if the current part of the simulation is warmup or cooldown phase.
	 * 
	 * This single-handedly controls the metric hierarchy acting up on the recordValue and recordCISample
	 * methods. If the flag is true, then metric hierarchy would not absorb any values at all, and just
	 * return without doing anything to its compute flags. The info is still used to build the server names
	 * in the hashmaps inside the metric hierarchy.
	 */
	public static boolean warmupEnabled = false;

	/**
	 * This function is used for closed loop. every scenario has an associated probability.
	 * This function returns a single scenario from given set of
	 * scenario based on their probability used by simulation. akhila
	 */
	public static ScenarioSim getRandomScenarioSimBasedOnProb() throws Exception {
		Random r = new Random();
		// the value in d will be in between 0 and 1
		double d = r.nextDouble();
		double dbl = 0.0;
		for (Scenario c : distributedSystemSim.scenarios.values()) {
			dbl += c.getProbability();
			if (dbl >= d) {
				return (ScenarioSim)c;
			}
		}
		throw new Exception("getRandomScenarioNameBasedOnProb: could not get scenario");
	}

	public static boolean getRequestRetryOnProb() {
		Random r = new Random();
		double d = r.nextDouble();
		if (ModelParameters.getRetryProbability() >= d) {
			// System.out.println("Value:"+d);
			return true;
		} else {
			return false;
		}
	}

	/**
	 * retrives the specified request id.
	 * 
	 * @author akhila
	 * @param requestId
	 * @return
	 * @throws Exception
	 */
	public static Request getRequest(int requestId) throws Exception {
		Request req = requestMap.get(requestId);
		if(req != null) {
			return req;
		}
		throw new Exception("getRequest: given request id " + requestId + "is not present in the requestlist");
	}

	/**
	 * searches the request for specified id. and removes the request from the list.
	 * 
	 * @author akhila
	 * @param id
	 * @throws Exception
	 */
	public static void removeRequest(int id) throws Exception {
		requestMap.remove(id);
	}

	public static void addRequest(Request req) {
		requestMap.put(req.id, req);
	}

	/**
	 * adds up all the requests arrived to each scenario and returns the total
	 */
	public static int getTotalRequestArrived() {
		int totalreq = 0;
		for (Object c : distributedSystemSim.scenarios.values()) {

			totalreq += (((ScenarioSim) c).getNumOfRequestsArrived());
		}
		// totalreq=SimParams.totalRequestsArrived;
		return totalreq;
	}

	public static int getTotalRequestProcessed() {
		int totalreq = 0;
		for (Object c : distributedSystemSim.scenarios.values()) {
			totalreq += (((ScenarioSim) c).getNumOfRequestsProcessed());
		}
		return totalreq;
	}
	
	/**
	 * The offer method inserts an element if possible, otherwise returning false.
	 * This differs from the Collection.add method, which can fail to add an element only by throwing an unchecked exception.
	 * The offer method is designed for use when failure is a normal, rather than exceptional occurrence, for example, in fixed-capacity (or "bounded") queues.
	 */
	public static void offerEvent(Event ev) {
		if (eventQueue.offer(ev) == false) {
			throw new Error("could not offer in eventlist");
		}
	}

	public static int getIntervalSlotCounter() {
		return ivalSlotCntr;
	}

	public static void clearIntervalSlotCounter() {
		SimulationParameters.ivalSlotCntr = 0;
	}
	
	public static void incrementIntervalSlotCounter() {
		ivalSlotCntr++;
		ivalSlotCntr = ivalSlotCntr % ModelParameters.intervalSlotCount;
	}
	
	public static double getCurrentSlotLength() {
		return ModelParameters.ivalSlotDurCyclic[ivalSlotCntr].value;
	}
	public static double getIntervalSlotRunTime(int slot) {
		return ivalSlotRunTimes[slot];
	}
	
	public static void recordIntervalSlotRunTime() {
		ivalSlotRunTimes[ivalSlotCntr] += currTime - lastSlotChangeTime;
		lastSlotChangeTime = currTime;
	}
}
