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
import java.util.Stack;
import java.util.Vector;

import org.apache.log4j.Logger;

import perfcenter.baseclass.ModelParameters;
import perfcenter.baseclass.Node;
import perfcenter.baseclass.Task;
import perfcenter.baseclass.enums.SystemType;
import perfcenter.simulator.DeviceSim;
import perfcenter.simulator.Event;
import perfcenter.simulator.EventType;
import perfcenter.simulator.HostSim;
import perfcenter.simulator.ScenarioSim;
import perfcenter.simulator.SimulationParameters;
import perfcenter.simulator.VirtualResSim;

/**
 * Request is an instance of scenario. Contains all the information about the request and its current position in the system.
 */
public class Request {
	/** request id */
	public int id;

	/** scenario name */
	public ScenarioSim scenario;

	/** pointer to current node */
	public Node currentNode;

	/** pointer to next node */
	public Node nextNode;

	/** time when the request arrives in the system */
	public double scenarioArrivalTime;

	/** time when the request arrived at the current s/w server */
	public double softServerArrivalTime;

	/** time when the thread was allocated to the request at current s/w server */
	public double softServerStartTime;

	/** id of the current software server */
	public String softServerName; //comes from Node class

	/** Name of current server to which request belongs, added by nikhil */
	public String fromServer;

	/** Name of current host to which request belongs, added by nikhil */
	public HostSim hostObject;
	public String devName; //this is always from the current host // comes from Task and VirtualResSim class
	public DeviceSim deviceObject;
	public String taskName; // comes from Node

	/** to track the number of softResources required by a single task */
	public String virtResName;
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
	public int virtualResInstance = -1;

	/** for RR scheduling.... the service time remaining */
	public double serviceTimeRemaining;
	public boolean serviceStarted;

	/** if the request is sync request then its state at different s/w servers is saved in this vector */
	public Vector<SyncRequest> synReqVector;

	/** to track the number of devices required by a single task or virtual res */
	private int deviceIndex = 0; // Bhavin

	/** to track the number of softResources required by a single task */
	public int virtualResIndex = 0;

	/** public Vector<VirResVector> resourceLayerStateVector; */
	public Stack<VirResVector> virtResStack;
	public int nwDataSize = 0;
	
	private boolean requestFromTask = false;
	private boolean requestFromVirtRes = false;
	
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
		currentNode = another.currentNode;
		destLanName = another.destLanName;
		devInstance = another.devInstance;
		deviceIndex = another.deviceIndex;
		fromServer = another.fromServer;
		this.requestFromTask = another.requestFromTask;
		this.requestFromVirtRes = another.requestFromVirtRes;
		this.devName = another.devName;
		hostObject = another.hostObject;
		linkName = another.linkName;
		scenario = another.scenario;
		nextNode = another.nextNode;
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
		this.softServerName = another.softServerName;
		nextNode = another.nextNode;
		softServerArrivalTime = another.softServerArrivalTime;
		softServerStartTime = another.softServerStartTime;
		fromServer = another.fromServer;
		srcLanName = another.srcLanName;
		destLanName = another.destLanName;
		virtResName = another.virtResName;
		virtualResIndex = another.virtualResIndex;
		virtualResInstance = another.virtualResInstance;
		retryRequest = another.retryRequest;
		energyConsumed = another.energyConsumed;
		userStatus = another.userStatus;
		inService = another.inService;
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
		virtResStack = new Stack<VirResVector>();
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
				return sr.getHostObject().getName();

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
	 * this function iterates through the synReqVector and corresponding to each entry 1.free the associated thread 2.update the performance
	 * measures(busytime(for calculating utilization) , avgQLength)
	 */
	public void freeHeldResourcesSync() {
		try {
			while (synReqVector.size() > 0) {
				SyncRequest sr = synReqVector.get(synReqVector.size() - 1);
				sr.getHostObject().getServer(sr.softServerName).abortThread(sr.threadNum, SimulationParameters.currentTime);
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
		currentNode = null;
		nextNode = null;
		scenarioArrivalTime = 0;
		scenarioTimeout = 0;
		softServerArrivalTime = 0;
		softServerStartTime = 0;
		this.softServerName = "";
		hostObject = null;
		this.devName = null;
		this.taskName = "";
		virtResName = null;
		linkName = "";
		srcLanName = "";
		destLanName = "";
		devInstance = -1;
		threadNum = -1;
		nwInstance = -1;
		virtualResInstance = -1;
		serviceTimeRemaining = 0;
		synReqVector.clear();
		deviceIndex = 0;
		virtualResIndex = 0;
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
			VirResVector vv = virtResStack.pop();
			VirtualResSim vrs = vv.getHostObject().getVirtualRes(vv.virtResName_);
			vrs.abort(vv.instanceNo_, SimulationParameters.currentTime);
		}

		if (isHoldingResourcesSync()) {
			freeHeldResourcesSync();
		}

		// update the scenario measures
		scenario.numOfRequestsDropped.recordValue(1);

		// check if dropped request should be retried or not.
		processRequest(SimulationParameters.currentTime);
	}

	/** get next virtual resource name from virtual res */
	public String getVirtualResourceNameFromVirRes() throws Exception {
		String vres = null;
		try {
			vres = ((VirtualResSim) hostObject.getVirtualRes(virtResName)).getNextVirtualResName(virtualResIndex);
			// } catch (Exception e) {
			// throw new Exception("getvrname from virres error"); //Bhavin
		} finally {
		}
		return vres;
	}

	/** get next virtual resource name from task */
	public String getVirtualResourceNameFromTask() {
		return hostObject.getServer(softServerName).getSimpleTask(taskName).getNextVirtualResName(virtualResIndex);
	}

	/**
	 * Devices are part of tasks and virtual resources. Next device is got from either task or virtual resource based on a variable getDeviceFrom
	 */
	public String getNextDeviceName() throws Exception { 
		String dev = null;
		if (this.requestFromTask) {
			Task t = hostObject.getServer(softServerName).getSimpleTask(taskName);
			dev = t.getNextDeviceName(deviceIndex);
		} else if (this.requestFromVirtRes) {
			VirtualResSim sr = (VirtualResSim) hostObject.getVirtualRes(virtResName);
			dev = sr.getNextDeviceName(deviceIndex);
		}
		return dev;
	}

	/** get virtual res name from next layer */
	public String getNextLayerVRName() throws Exception {
		String dev = null;
		if (this.isRequestFromVirtRes()) {
			dev = hostObject.getVirtualRes(virtResName).getNextVirtualResName(0);
		}
		return dev;

	}

	public int getDeviceIndex() {
		return deviceIndex;
	}

	public void setDeviceIndex(int deviceIndex, String methodAndClassName) {
		this.deviceIndex = deviceIndex;
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
		double newArrivalTime = SimulationParameters.currentTime;
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
				newArrivalTime = SimulationParameters.currentTime + ModelParameters.getThinkTime().nextRandomVal(1);
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
					SimulationParameters.currentTime = time;
					Request temprq = new Request(previous);

					temprq.hostObject.getServer(temprq.softServerName).processTaskEndEventTimeout(
							temprq, temprq.threadNum, SimulationParameters.currentTime);
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
					SimulationParameters.currentTime = time;
					Request temprq = new Request(previousRequest);

					// get the host and server object
					temprq.hostObject.getServer(temprq.softServerName).processTaskEndEventTimeout(
							temprq, temprq.threadNum, SimulationParameters.currentTime);
				}
			} else if (timeoutFlagInBuffer) {
				// when the request gets timeout at buffer we have to deque next requests by generating START_SOFTWARE_TASK event for that request
				SimulationParameters.currentTime = time;

				hostObject.getServer(softServerName).processTaskEndEventTimeout(this, threadNum, SimulationParameters.currentTime);
				SimulationParameters.removeRequest(id);
			} else {
				// Depending upon the request if not timeout at all or timeout after receiving service then just remove that request
				SimulationParameters.removeRequest(id);
			}
		}
	}

	public void setHostName(String hostName) {
		this.hostObject = SimulationParameters.distributedSystemSim.getHost(hostName);
	}

	public boolean isRequestFromTask() {
		return requestFromTask;
	}

	public void setRequestFromTask() {
		this.requestFromTask = true;
		this.requestFromVirtRes = false;
	}

	public boolean isRequestFromVirtRes() {
		return requestFromVirtRes;
	}

	public void setRequestFromVirtRes() {
		this.requestFromVirtRes = true;
		this.requestFromTask = false;
	}
	
	public void clearRequestFromFlags() {
		this.requestFromTask = false;
		this.requestFromVirtRes = false;
	}
}