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
package perfcenter.simulator.request;

import java.util.ArrayList;
import perfcenter.baseclass.enums.DeviceType;
import perfcenter.baseclass.DeviceCategory;
import java.util.Stack;
import java.util.Vector;

import org.apache.log4j.Logger;

import perfcenter.baseclass.ModelParameters;
import perfcenter.baseclass.TaskNode;
import perfcenter.baseclass.Task;
import perfcenter.baseclass.enums.SystemType;
import perfcenter.simulator.DeviceSim;
import perfcenter.simulator.Event;
import perfcenter.simulator.EventType;
import perfcenter.baseclass.Machine;
import perfcenter.simulator.PhysicalMachineSim;
import perfcenter.simulator.ScenarioSim;
import perfcenter.simulator.SimulationParameters;
import perfcenter.simulator.SoftResSim;

/**
 * Request is an instance of scenario. Contains all the information about the request and its current position in the system.
 */
public class Request {
	/** request id */
	public int id;

	/** scenario name */
	public ScenarioSim scenario;

	/** pointer to current node */
	public TaskNode currentTaskNode;

	/** pointer to next node */
	public TaskNode nextTaskNode;

	/** time when the request arrives in the system */
	public double scenarioArrivalTime;

	/** time when the request arrived at the current s/w server */
	public double softServArrivalTime;

	/** time when the thread was allocated to the request at current s/w server */
	public double softServStartTime;

	/** id of the current software server */
	public String softServName; //comes from Node class

	/** Name of current server to which request belongs, added by nikhil */
	public String fromServer;

	/** Name of current host to which request belongs, added by nikhil */
	public String machineName;
	public String devName; //this is always from the current host // comes from Task and VirtualResSim class
	public String taskName; // comes from Node

	/** to track the number of softResources required by a single task */
	public String softResName;
	public String linkName = "";
	public String srcLanName = "";
	public String destLanName = "";

	/** instance id of device that is free. */
	public int devInstance = -1; //ARCHITECTURE: is this duplicate of instanceID?

	/** id of the thread held by the resource */
	public int threadNum = -1;

	/** instance id of network link that is free. right now only one instance of link is available */
	public int nwInstance = -1;

	/** instance id of virtual res that is free. right now there is be only one instance of virtual resource */
	public int softResInstance = -1;

	/** for RR scheduling.... the service time remaining */
	public double serviceTimeRemaining;
	public boolean serviceStarted;

	/** if the request is sync request then its state at different s/w servers is saved in this vector */
	public Vector<SyncRequest> synReqVector;

	/** to track the number of devices required by a single task or virtual res */
	private int subTaskIdx = 0; // Bhavin

	/** to track the number of softResources required by a single task */
	public int softResIdx = 0;

	/** public Vector<VirResVector> resourceLayerStateVector; */
	public Stack<SoftResVector> virtResStack;
	public int nwDataSize = 0;
	
	private boolean requestFromTask = false;
	private boolean requestFromSoftRes = false;
	
	Logger logger = Logger.getLogger("Request");
	public double scenarioTimeout;
	public boolean timeoutFlagInBuffer;
	public boolean timeoutFlagAfterService;
	public boolean retryRequest = false;
	public int qServerInstanceID;

	public double numberOfRetries = 0;

	/** for accounting energy consumption */
	public double energyConsumed = 0.0;

	/** used for closed system cyclic workload */
	public boolean userStatus;
	public boolean inService;

	/** making a deep copy for request */
	public Request(Request another) {
		// ARCHITECTURE: change all the copy constructors to use Object.clone() method
		id = another.id;
		numberOfRetries = another.numberOfRetries;
		currentTaskNode = another.currentTaskNode;
		destLanName = another.destLanName;
		devInstance = another.devInstance;
		subTaskIdx = another.subTaskIdx;
		fromServer = another.fromServer;
		this.requestFromTask = another.requestFromTask;
		this.requestFromSoftRes = another.requestFromSoftRes;
		this.devName = another.devName;
		machineName = another.machineName;
		linkName = another.linkName;
		scenario = another.scenario;
		nextTaskNode = another.nextTaskNode;
		nwDataSize = another.nwDataSize;
		nwInstance = another.nwInstance;
		threadNum = another.threadNum;
		scenarioArrivalTime = another.scenarioArrivalTime;
		scenarioTimeout = another.scenarioTimeout;
		synReqVector = another.synReqVector;
		serviceStarted = another.serviceStarted;
		this.taskName = another.taskName;
		timeoutFlagAfterService = another.timeoutFlagAfterService;
		timeoutFlagInBuffer = another.timeoutFlagInBuffer;
		this.softServName = another.softServName;
		nextTaskNode = another.nextTaskNode;
		softServArrivalTime = another.softServArrivalTime;
		softServStartTime = another.softServStartTime;
		fromServer = another.fromServer;
		srcLanName = another.srcLanName;
		destLanName = another.destLanName;
		softResName = another.softResName;
		softResIdx = another.softResIdx;
		softResInstance = another.softResInstance;
		retryRequest = another.retryRequest;
		energyConsumed = another.energyConsumed;
		userStatus = another.userStatus;
		inService = another.inService;
	}

	public void printRequest() {
		System.out.println("=============================================");
		System.out.println("Printing Request with id:" + id);
		System.out.println("Number Of Retries:" +  numberOfRetries);
		System.out.println("Current Node:" + currentTaskNode.name );
        System.out.println("Next Node: " + nextTaskNode.name);
		System.out.println("Device Instance: " + devInstance + " Device Index:" + subTaskIdx );
		System.out.println("From server: " + fromServer );
		System.out.println("Is request from task: " + requestFromTask );
		System.out.println("Is request from virtual resource: " + requestFromSoftRes) ;
		System.out.println("Device Name: " + devName );
		System.out.println("Host Object: " + machineName);
		System.out.println("Scenario Name: " + scenario.getName() );
		System.out.println("Thread Number: " + threadNum );
		System.out.println("Scenario Arrival Time: " + scenarioArrivalTime + " Scenario Timeout: " + scenarioTimeout);
		System.out.println("Sync req vector size: " + synReqVector.size());
		System.out.println("Service Started Time: " + serviceStarted );
		System.out.println("Task Name: " + taskName );
		System.out.println("Timeout Flag after service: " + timeoutFlagAfterService );
		System.out.println("Timeout Flag in Buffer: " + timeoutFlagInBuffer );
		System.out.println("Software server name: " + softServName + " Software server arrival time: " + softServArrivalTime + " Software Server Start Time: " + softServStartTime );
		System.out.println("Software Resource Name: " + softResName + "index: " + softResIdx + " instance : " + softResInstance );
		System.out.println("=============================================");
	}
	public Request(int rid, ScenarioSim sce_name, double arrivalTime) {
		synReqVector = new Vector<SyncRequest>();
		scenario = sce_name;
		scenarioArrivalTime = arrivalTime;
		id = rid;
		serviceStarted = false;
		qServerInstanceID = -1;
		timeoutFlagInBuffer = false;
		timeoutFlagAfterService = false;
		serviceTimeRemaining = -2;
		virtResStack = new Stack<SoftResVector>();
		userStatus = true;
		inService = true;
	}

	/** returns true if request is making sync call */
	public boolean isSyncRequest(String ss_name, String task_name, int thrdNum, int reqID, double servArrTime, double servStartTime) {
		if (synReqVector.size() < 1) {
			return false;
		}
		SyncRequest sr = synReqVector.get(synReqVector.size() - 1);
		if ((sr.softServerName.compareToIgnoreCase(ss_name) == 0) && (sr.taskName.compareToIgnoreCase(task_name) == 0) && sr.threadNum == thrdNum
				&& sr.swServerArrivalTime == servArrTime && sr.swServerStartTime == servStartTime) {
			return true;
		} else {
			return false;
		}
	}

	/**
	 * this method returns the s/w server arrival time saved for a sync request saved on the syncVector helps calculate response time etc.
	 */
	public double getServerArrivalTime(String ss_name, int rqID) {
		int i = synReqVector.size() - 1, flag = 0;
		double servArrTime = 0;
		while (i >= 0) {
			SyncRequest sr = synReqVector.get(i);
			if ((sr.softServerName.compareToIgnoreCase(ss_name) == 0) && sr.reqID == rqID) {
				flag = 1;
				servArrTime = sr.swServerArrivalTime;
				break;
			}
			i--;
		}
		if (flag == 0) {
			System.out.println("Error in SYNC VECTORservarr");
			System.exit(0);
		}
		return servArrTime;
	}

	/**
	 * this method returns the s/w server start time saved for a sync request saved on the syncVector helps calculate busytime of threads etc.
	 */
	public double getServerStartTime(String ss_name, int rqID) {
		int i = synReqVector.size() - 1, flag = 0;
		double servStartTime = 0;
		while (i >= 0) {
			SyncRequest sr = synReqVector.get(i);
			if ((sr.softServerName.compareToIgnoreCase(ss_name) == 0) && sr.reqID == rqID) {
				flag = 1;
				servStartTime = sr.swServerStartTime;
				break;
			}
			i--;
		}
		if (flag == 0) {
			logger.error("Error in SYNC VECTOR servstart");
			System.exit(0);
		}
		return servStartTime;
	}

	/**
	 * this method returns the thread no. saved for a sync request saved on the syncVector
	 */
	public int getThreadNum(String ss_name, int rqID) {
		int i = synReqVector.size() - 1, flag = 0;
		int thrdNum = 0;
		while (i >= 0) {
			SyncRequest sr = synReqVector.get(i);
			if ((sr.softServerName.compareToIgnoreCase(ss_name) == 0) && sr.reqID == rqID) {
				flag = 1;
				thrdNum = sr.threadNum;
				break;
			}
			i--;
		}
		if (flag == 0) {
			logger.error("Error in SYNC VECTOR threadnum");
			System.exit(0);
		}
		return thrdNum;
	}

	/**
	 * get the hostname associated with request id.
	 * 
	 * @author akhila
	 * @param ss_name
	 * @param rqID
	 * @return
	 */
	public String getHostName(String ss_name, int rqID) {
		int i = synReqVector.size() - 1, flag = 0;

		while (i >= 0) {
			SyncRequest sr = synReqVector.get(i);
			if ((sr.softServerName.compareToIgnoreCase(ss_name) == 0) && sr.reqID == rqID) {
				flag = 1;
				if(SimulationParameters.distributedSystemSim.serverMigrated(ss_name)){
					sr.setMachineName(SimulationParameters.distributedSystemSim.softServerMap.get(ss_name).machines.get(0));
				}
				return sr.getMachineName();

			}
			i--;
		}
		if (flag == 0) {
			logger.error("Error in SYNC VECTOR hostname");
			System.exit(0);
		}
		return null;
	}

	/**
	 * if sync reply returns true, else returns false
	 * 
	 * @param ss_name
	 * @return
	 */
	public boolean isSyncReply(String ss_name) {
		int i = synReqVector.size() - 1;
		if (i >= 0) {
			if ((synReqVector.get(i).softServerName.compareToIgnoreCase(ss_name) == 0)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * used to determine whether the request is a sync request holding resources on some upstream server
	 * 
	 * @return = true if synReqVector is not empty, false otherwise
	 */
	public boolean isHoldingResourcesSync() {
		if (synReqVector.size() > 0) {
			return true;
		} else {
			return false;
		}
	}

	/**
	 * free resources held by this sync request on upstream servers
	 * 
	 * this function iterates through the synReqVector and corresponding to each entry 
	 * 1.free the associated thread 
	 * 2.update the performance
	 * measures(busytime(for calculating utilization) , avgQLength)
	 */
	public void freeHeldResourcesSync() {
		try {
			while (synReqVector.size() > 0) {
				SyncRequest sr = synReqVector.get(synReqVector.size() - 1);
				PhysicalMachineSim machineObject = SimulationParameters.distributedSystemSim.machineMap.get(sr.getMachineName());
				machineObject.getServer(sr.softServerName).abortThread(sr.threadNum, SimulationParameters.currTime);
				synReqVector.remove(synReqVector.size() - 1);
			}
		} catch (Exception e) {
			logger.error("freeHeldResourcesSync error" + e.getMessage());
			System.exit(0);
		}
	}

	/**
	 * all the request variables are initialized except id . same request is reinitialized and used(used by closed loop) akhila
	 */
	public void clear() {
		scenario = null;
		currentTaskNode = null;
		nextTaskNode = null;
		scenarioArrivalTime = 0;
		scenarioTimeout = 0;
		softServArrivalTime = 0;
		softServStartTime = 0;
		this.softServName = "";
		machineName = null;
		this.devName = "";
		this.taskName = "";
		softResName = null;
		linkName = "";
		srcLanName = "";
		destLanName = "";
		devInstance = -1;
		threadNum = -1;
		nwInstance = -1;
		softResInstance = -1;
		serviceTimeRemaining = 0;
		synReqVector.clear();
		subTaskIdx = 0;
		softResIdx = 0;
		this.clearRequestFromFlags();
		nwDataSize = 0;
		energyConsumed = 0.0;
		virtResStack.clear();
		userStatus = false;
		inService = false;
	}

	/**
	 * Aborts any resources held at upper layers.
	 * This function is basically used for cleanup operations related to discarded requests.
	 */
	public void drop() throws Exception {
		logger.debug("Request dropped " + id);
		while (virtResStack.size() > 1) {
			SoftResVector vv = virtResStack.pop();
			SoftResSim vrs = vv.getHostObject().getSoftRes(vv.softResName_);
			vrs.abort(vv.instanceNo_, SimulationParameters.currTime);
		}

		if (isHoldingResourcesSync()) {
			freeHeldResourcesSync();
		}

		// update the scenario measures
		scenario.noOfReqDropped.recordValue(1);

		// check if dropped request should be retried or not.
		processRequest(SimulationParameters.currTime);
	}

	/** get next virtual resource name from virtual res */
	public String getVirtualResourceNameFromVirRes() throws Exception {
		PhysicalMachineSim machineObject = SimulationParameters.distributedSystemSim.machineMap.get(machineName);
		String vres = null;
		try {
			vres = ((SoftResSim) machineObject.getSoftRes(softResName)).getNextSoftResName(softResIdx);
			// } catch (Exception e) {
			// throw new Exception("getvrname from virres error"); //Bhavin
		} finally {
		}
		return vres;
	}

	/** get next virtual resource name from task */
	public String getVirtualResourceNameFromTask() {
		PhysicalMachineSim machineObject = SimulationParameters.distributedSystemSim.machineMap.get(machineName);
		return machineObject.getServer(softServName).getTaskObject(taskName).getNextSoftResName(softResIdx);
	}

	/**
	 * One Task or Software Resource is served by sequence of one or more devices having different service times. 
	 * Next device is retrieved from either task or software resource based on a requestFromTask, requestFromSoftRes flags.
	 */
	public String getNextDeviceName() throws Exception {  //MAKECHANGE 
		DeviceCategory nextDevCat = null;
		PhysicalMachineSim machineObject = SimulationParameters.distributedSystemSim.machineMap.get(machineName);
		String nextDev = null;
		if (this.requestFromTask) {
			Task t = machineObject.getServer(softServName).getTaskObject(currentTaskNode.name);
			nextDevCat = t.getNextDeviceCategory(subTaskIdx);
		} else if (this.requestFromSoftRes) {
			SoftResSim sr = (SoftResSim) machineObject.getSoftRes(softResName);
		//TODO: DOCHECK	//Assume there is only one soft server in softRes's softServers ArrayList element
			if(sr.softServers.size() != 1){
				logger.debug("SoftResource " + sr.name + " has multiple softServers");
				return null;
			}
			nextDevCat = sr.getNextDeviceCategory(softResIdx);			
		}
		if(nextDevCat == null)
			return nextDev;

		nextDev = machineObject.getDeviceName(nextDevCat);
		return nextDev;
	}

	/** get virtual res name from next layer */
	public String getNextLayerVRName() throws Exception {
		String dev = null;
		PhysicalMachineSim machineObject = SimulationParameters.distributedSystemSim.machineMap.get(machineName);
		if (this.isRequestFromSoftRes()) {
			dev = machineObject.getSoftRes(softResName).getNextSoftResName(0);
		}
		return dev;

	}

	public int getSubTaskIdx() {
		return subTaskIdx;
	}

	public void setSubTaskIdx(int deviceIndex, String methodAndClassName) {
		int temp = this.subTaskIdx;
		this.subTaskIdx = deviceIndex;
		//System.out.println(methodAndClassName + " set deviceIndex from " + temp + " to " + deviceIndex);
		changeLog.add(methodAndClassName);
	}

	public ArrayList<String> changeLog = new ArrayList<String>(10); //FIXME debugging datastructures, should be commented from main code

	public int getCountInChangeLog(String str) {
		int count = 0;
		for (String s : changeLog) {
			if (s.equals(str)) {
				count++;
			}
		}
		return count;
	}

	public void processRequest(double time) throws Exception {
		ScenarioSim newScenarioName = scenario;
		double newArrivalTime = SimulationParameters.currTime;
		boolean retryFlag = false;

		// check if the request should retry
		if ((timeoutFlagAfterService == true || timeoutFlagInBuffer == true) && SimulationParameters.getRequestRetryOnProb() == true) {
			numberOfRetries++;
			retryFlag = true;
		}
		if (ModelParameters.getSystemType() == SystemType.CLOSED) {
			// If request is not subject to retry, develop new data.
			if (retryFlag == false) {
				newScenarioName = SimulationParameters.getRandomScenarioSimBasedOnProb();
				newArrivalTime = SimulationParameters.currTime + ModelParameters.getThinkTime().nextRandomVal(1);
			}
			Request previous = new Request(this);
			clear();
			if (previous.userStatus == true) {
				// clear all the data with request
				// sceName and arrivalTime values depends on if request is retrying.
				scenario = newScenarioName;
				scenarioArrivalTime = newArrivalTime;
				timeoutFlagInBuffer = false;
				timeoutFlagAfterService = false;
				userStatus = true;
				inService = true;
				if (SimulationParameters.lastScenarioArrivalTime < newArrivalTime) {
					SimulationParameters.lastScenarioArrivalTime = newArrivalTime;
				}
				SimulationParameters.offerEvent(new Event(newArrivalTime, EventType.SCENARIO_ARRIVAL, this));

				// if timeout in Buffer then just remove that request and deque next request to process
				if (previous.timeoutFlagInBuffer) { //XXX requests timed out during service should also be counted as blocked requests
					SimulationParameters.currTime = time;
					Request temprq = new Request(previous);
					PhysicalMachineSim machineObject = SimulationParameters.distributedSystemSim.machineMap.get(temprq.machineName);
					machineObject.getServer(temprq.softServName).processTaskEndEventTimeout(
							temprq, temprq.threadNum, SimulationParameters.currTime);
				}
			}
		}

		// if open loop remove the request from the list.
		if (ModelParameters.getSystemType() == SystemType.OPEN) {

			// check if the request should retry, and create new arrival accordingly.
			if (retryFlag == true && (numberOfRetries <= ModelParameters.getMaxRetry())) {
				Request previousRequest = new Request(this);
				newScenarioName = SimulationParameters.getRandomScenarioSimBasedOnProb();
				newArrivalTime = SimulationParameters.lastScenarioArrivalTime + SimulationParameters.exp.nextExp(1 / newScenarioName.getArateToScenario());
				clear();
				scenario = newScenarioName;
				scenarioArrivalTime = newArrivalTime;
				timeoutFlagInBuffer = false;
				timeoutFlagAfterService = false;
				retryRequest = true;

				// change lastscenarioarrivaltime only if scenario arrival event have greater value
				if (SimulationParameters.lastScenarioArrivalTime < newArrivalTime) {
					SimulationParameters.lastScenarioArrivalTime = newArrivalTime;
				}
				SimulationParameters.offerEvent(new Event(newArrivalTime, EventType.SCENARIO_ARRIVAL, this));

				// if timeout in Buffer then deque the next request and remove the current request
				if (previousRequest.timeoutFlagInBuffer) {
					SimulationParameters.currTime = time;
					Request temprq = new Request(previousRequest);

					// get the host and server object
					PhysicalMachineSim machineObject = SimulationParameters.distributedSystemSim.machineMap.get(temprq.machineName);
					machineObject.getServer(temprq.softServName).processTaskEndEventTimeout(
							temprq, temprq.threadNum, SimulationParameters.currTime);
				}
			} else if (timeoutFlagInBuffer) {
				// when the request gets timeout at buffer we have to deque next requests by generating START_SOFTWARE_TASK event for that request
				SimulationParameters.currTime = time;
				PhysicalMachineSim machineObject = SimulationParameters.distributedSystemSim.machineMap.get(machineName);
				machineObject.getServer(softServName).processTaskEndEventTimeout(this, threadNum, SimulationParameters.currTime);
				SimulationParameters.removeRequest(id);
			} else {
				// Depending upon the request if not timeout at all or timeout after receiving service then just remove that request
				SimulationParameters.removeRequest(id);
			}
		}
	}

	public void setHost(String hostName) {
		this.machineName = hostName;
	}

	public boolean isRequestFromTask() {
		return requestFromTask;
	}

	public void setRequestFromTask() {
		this.requestFromTask = true;
		this.requestFromSoftRes = false;
	}

	public boolean isRequestFromSoftRes() {
		return requestFromSoftRes;
	}

	public void setRequestFromVirtRes() {
		this.requestFromSoftRes = true;
		this.requestFromTask = false;
	}
	
	public void clearRequestFromFlags() {
		this.requestFromTask = false;
		this.requestFromSoftRes = false;
	}
}