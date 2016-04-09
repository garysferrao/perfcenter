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
package perfcenter.simulator;

import org.apache.log4j.Logger;

import perfcenter.baseclass.Device;
import perfcenter.baseclass.Machine;
import perfcenter.baseclass.ModelParameters;
import perfcenter.baseclass.TaskNode;
import perfcenter.baseclass.enums.SystemType;
import perfcenter.baseclass.exception.*;
import perfcenter.simulator.queue.QServerInstance;
import perfcenter.simulator.queue.QueueSim;
import perfcenter.simulator.request.Request;

/**
 * This class has handling methods for all types of events.
 * @author bhavin
 *
 */
public class Event implements Comparable<Event> {

	//ARCHITECTURE: convert each event to its own class

	/** the time at which the event is supposed to happen. */
	double timestamp;
	/** type of the event */
	EventType type;
	/** Request object associated with this event. This can be null. */
	private Request reqObj;

	/** keep track of event name and device name for powermanaged devices */
	private PhysicalMachineSim hostObj = null;
	private DeviceSim devObj = null;

	Logger logger = Logger.getLogger("Event");

	public Event(double evTime, EventType evType, Request rqId) {
		timestamp = evTime;
		type = evType;
		reqObj = rqId;
	}

	/** added for changing workload model */
	public Event(double evTime, EventType evType) {
		timestamp = evTime;
		type = evType;
		reqObj = null;
	}

	/** added for device probe event */
	Event(double evTime, EventType eventType, PhysicalMachineSim host, DeviceSim device) {
		timestamp = evTime;
		type = eventType;
		this.hostObj = host;
		this.devObj = device;
		reqObj = null;
	}
	
	//CHECK: What if timestamps of both events is similar
	/** comparator implemented. Comparing is on "time" */
	public int compareTo(Event o) {
		Event r = (Event) o;
		if (timestamp < r.timestamp) {
			return -1;
		} else if (timestamp > r.timestamp) {
			return 1;
		} else {
			return 0;
		}
	}

	/**
	 * Function: external arrival. Handles the event when an external arrives in the system.
	 * Get the name of the 1st software server from the call flow node.
	 * Get the request object of the this event and update the parameters in the request.
	 * Add the request in the queue of the software server
	 */
	public void scenarioArrival() throws Exception {
		// update current time
		SimulationParameters.currTime = timestamp;
		SimulationParameters.totalReqArrived++;

		// get the request object corresponding to this arrival event from request list
		logger.debug("request: " + reqObj.scenario);

		// for open loop simulation generate next external arrival event
		// for the scenario. check if its not the retry event: yogesh
		if (!reqObj.retryRequest) {
			if (ModelParameters.getSystemType() == SystemType.OPEN) {
				// if arrival rate is zero do not generate any events/requests.
				if (reqObj.scenario.getArateToScenario() > 0) {
					double arrivalTime = timestamp + SimulationParameters.exp.nextExp(1 / reqObj.scenario.getArateToScenario());
					Request newRequest = new Request(SimulationParameters.reqIdGenerator++, reqObj.scenario, arrivalTime);
					newRequest.scenarioArrivalTime = arrivalTime;
					SimulationParameters.addRequest(newRequest);

					//schedule arrival of the next request
					SimulationParameters.offerEvent(
							new Event(arrivalTime, EventType.SCENARIO_ARRIVAL, newRequest));

					// change lastscenarioarrivaltime only if scenario arrival event have greater value
					if (SimulationParameters.lastScenarioArrivalTime < arrivalTime) {
						SimulationParameters.lastScenarioArrivalTime = arrivalTime;
					}
				}
			}
		}

		reqObj.scenarioArrivalTime = SimulationParameters.currTime;
		reqObj.scenarioTimeout = SimulationParameters.currTime + ModelParameters.getTimeout().nextRandomVal(1);

		// identify the software server to which this request was submitted
		reqObj.scenario.noOfReqArrived.recordValue(1);

		// The request will directly go to the first software server i.e. the root node of the scenario.
		TaskNode currentNode;
		if (reqObj.scenario.rootNode.name.compareToIgnoreCase("user") == 0) {
			currentNode = reqObj.scenario.rootNode.children.get(0);
		} else {
			currentNode = reqObj.scenario.rootNode;
		}
		reqObj.currentTaskNode = currentNode;
		reqObj.nextTaskNode = SimulationParameters.distributedSystemSim.findNextTaskNode(currentNode);
		reqObj.taskName = currentNode.name;
		reqObj.softServName = currentNode.servername;
		reqObj.machineObject = ((SoftServerSim)SimulationParameters.distributedSystemSim.getServer(reqObj.softServName)).getRandomHostObject();
		if(reqObj.machineObject == null) {
			System.out.println(((SoftServerSim)SimulationParameters.distributedSystemSim.getServer(reqObj.softServName)));
		}
		reqObj.softServArrivalTime = SimulationParameters.currTime;

		// get the soft server object to which this request will be offered
		SoftServerSim sserver = reqObj.machineObject.getServer(reqObj.softServName);

		logger.debug("Task_Name: " + reqObj.taskName);
		logger.debug("Server_Name: " + reqObj.softServName);
		logger.debug("Host_Name: " + reqObj.machineObject.getName());
		sserver.enqueue(reqObj, SimulationParameters.currTime);
	}

	

	/**
	 * Begins processing of Hardware execution
	 */
	public void hardwareTaskStarts() throws Exception {
		SimulationParameters.currTime = timestamp;
		reqObj.machineObject.getDevice(reqObj.devName).processTaskStartEvent(reqObj, SimulationParameters.currTime);
	}

	/**
	 * Hardware execution ending is handled.
	 */
	public void hardwareTaskEnds() throws Exception {
		SimulationParameters.currTime = timestamp;
		reqObj.machineObject.getDevice(reqObj.devName).processTaskEndEvent(reqObj, reqObj.devInstance, SimulationParameters.currTime);
	}

	/**
	 * Function: softwareTaskStarts Update the request object with the system arrival time etc. First find the hardware of the machine on which the
	 * software server is hosted. Find whether the hardware is idle. If yes, then create a new event of type hardwareTaskStarts and enqueue in the
	 * eventList. if no, then queue the request into the hardware.
	 */
	public void softwareTaskStarts() throws Exception {
		SimulationParameters.currTime = timestamp;

		// get the request object
		logger.debug("request: ");
		logger.debug("scenario_name: " + reqObj.scenario);
		logger.debug("s/w server name: " + reqObj.softServName);
		logger.debug("task_name: " + reqObj.taskName);
		logger.debug("timeout_time: " + reqObj.scenarioTimeout);

		// check if timed out in buffer.
		if ((ModelParameters.timeoutEnabled == true) && (reqObj.scenarioTimeout < SimulationParameters.currTime)) {
			timeoutInBuffer();
		} else {
			SoftServerSim softServerSim = reqObj.machineObject.getServer(reqObj.softServName);
			softServerSim.processTaskStartEvent(reqObj, SimulationParameters.currTime);
		}
	}

	/**
	 * Called when the software task ends.
	 */
	public void softwareTaskEnds() throws Exception {
		SimulationParameters.currTime = timestamp;
		reqObj.machineObject.getServer(reqObj.softServName).processTaskEndEvent(reqObj, reqObj.threadNum, SimulationParameters.currTime);
	}

	/**
	 * Event handling number of users chages.
	 * 
	 * @author yogesh
	 * @throws Exception
	 */
	public void numberOfUserChanged() throws Exception {
		SimulationParameters.currTime = timestamp;
		SimulationParameters.recordIntervalSlotRunTime();

		for (Machine host : SimulationParameters.distributedSystemSim.pms) {

			// Collect all the device metrics value within a interval
			for (Object device : host.devices) {
				DeviceSim deviceSim = (DeviceSim) device;
				// If the device is busy at the time of collection then find then appropriately update the value of totalBusyTime
				for (QServerInstance qServerInstance : ((QueueSim) deviceSim.resourceQueue).qServerInstances) {
					if (qServerInstance.isBusyStatus()) {
						qServerInstance.totalBusyTime.recordValue(SimulationParameters.currTime - qServerInstance.reqStartTime);
						qServerInstance.reqStartTime = SimulationParameters.currTime;
						qServerInstance.reqArrivalTime = SimulationParameters.currTime;
					}
				}
			}

			// Collect all the software metrics value within a interval
			for (Object softServer : host.softServers) {
				SoftServerSim softServerSim = (SoftServerSim) softServer;
				for (QServerInstance qsi : ((QueueSim) softServerSim.resourceQueue).qServerInstances) {
					if (qsi.isBusyStatus()) {
						qsi.totalBusyTime.recordValue(SimulationParameters.currTime - qsi.reqStartTime);
						qsi.reqStartTime = SimulationParameters.currTime;
						qsi.reqArrivalTime = SimulationParameters.currTime;
					}
				}
				// flush the values for next interval
				softServerSim.totalServerEnergy = 0.0;
			}
		}

		SimulationParameters.incrementIntervalSlotCounter();
		SimulationParameters.offerEvent(
				new Event(SimulationParameters.currTime + SimulationParameters.getCurrentSlotLength(), EventType.NO_OF_USERS_CHANGES));
		instantiateUsers();
	}

	/**
	 * Only for closed systems.
	 * @throws Exception
	 */
	public void instantiateUsers() throws Exception {
		int previousUserCount = (int)ModelParameters.getNumberOfUserss(SimulationParameters.getIntervalSlotCounter() - 1);
		int currentUserCount = (int)ModelParameters.getNumberOfUserss(SimulationParameters.getIntervalSlotCounter());

		// If the number of users increases for next interval then invoke the new users or if they are already present and idle then just reinvoked
		if (currentUserCount > previousUserCount) {

			// int startReqId = SimulationParameters.requestIDGenerator;
			for (int users = previousUserCount; users < currentUserCount; users++) {

				ScenarioSim sceName = SimulationParameters.getRandomScenarioSimBasedOnProb();
				double interArrivalTimeNext = SimulationParameters.currTime + ModelParameters.getThinkTime().nextRandomVal(1);
				// double interArrivalTimeNext = SimParams.currentTime + users * 0.001;
				if (SimulationParameters.reqIdGenerator < ModelParameters.getMaxUsers()) {
					Request req = new Request(SimulationParameters.reqIdGenerator++, sceName, interArrivalTimeNext);
					req.scenarioArrivalTime = interArrivalTimeNext;
					//All the scenarios are added into request list here
					SimulationParameters.addRequest(req);
					Event ev = new Event(interArrivalTimeNext, EventType.SCENARIO_ARRIVAL, req);
					// change lastscenarioarrivalcurrTime only if scenario arrival event have greater value
					if (SimulationParameters.lastScenarioArrivalTime < ev.timestamp) {
						SimulationParameters.lastScenarioArrivalTime = ev.timestamp;
					}
					SimulationParameters.offerEvent(ev);
					logger.debug("scenario name: " + sceName + "\t scenario_ID: " + users + "\t\t  scenario arrival time: " + interArrivalTimeNext);
				} else {
					//FIXME: consider a system extremely under load, where the following if condition will never be true. Then we are not generating enough users / enough arrivals. There must be an else part to this, where new requests are generated.
					Request existingUser = SimulationParameters.getRequest(users);
					if (!existingUser.inService) {
						existingUser.scenario = sceName;
						existingUser.scenarioArrivalTime = interArrivalTimeNext;
						existingUser.timeoutFlagInBuffer = false;
						existingUser.timeoutFlagAfterService = false;
						Event ev = new Event(interArrivalTimeNext, EventType.SCENARIO_ARRIVAL, existingUser);
						if (SimulationParameters.lastScenarioArrivalTime < ev.timestamp) {
							SimulationParameters.lastScenarioArrivalTime = ev.timestamp;
						}
						// this is where we create the corresponding arrival event
						// and add it to eventList
						SimulationParameters.offerEvent(ev);
					}
					existingUser.userStatus = true;
				}
			}
		}
		// else if the number of users decreases in the next interval then simply disable that many number of users
		else if (currentUserCount < previousUserCount) {
			for (int user = currentUserCount; user < ModelParameters.getMaxUsers(); user++) {
				if (requestPresent(user)) {
					Request tearReq = SimulationParameters.getRequest(user);
					// simply set status of user to idle for this interval also remove any scenario arrival events for those requests
					tearReq.userStatus = false;
				}
			}
		}
	}

	public boolean requestPresent(int id) {
		return SimulationParameters.requestMap.containsKey(id);
	}

	/**
	 * Event handling arrival rates value
	 * 
	 * @author yogesh
	 * @throws Exception
	 */
	public void arrivalRateChanged() {
		SimulationParameters.currTime = timestamp;
		SimulationParameters.recordIntervalSlotRunTime();

		// Collect all the device metrics value within a interval
		for (Machine host : SimulationParameters.distributedSystemSim.pms) {
			for (Object device : host.devices) {
				DeviceSim deviceSim = (DeviceSim) device;

				//check if the instance is busy then update the total busy time
				//Also set the request start time and arrival time of currrent request at this device instance to current time
				for (QServerInstance qServerInstance : ((QueueSim) deviceSim.resourceQueue).qServerInstances) {
					if (qServerInstance.isBusyStatus()) {
						qServerInstance.totalBusyTime.recordValue(qServerInstance.reqStartTime - SimulationParameters.currTime);
						qServerInstance.reqStartTime = SimulationParameters.currTime;
						qServerInstance.reqArrivalTime = SimulationParameters.currTime;
					}
				}
			}
			for (Object softServer : host.softServers) {
				SoftServerSim softServerSim = (SoftServerSim) softServer;
				for (QServerInstance qsi : ((QueueSim) softServerSim.resourceQueue).qServerInstances) {
					if (qsi.isBusyStatus()) {
						qsi.totalBusyTime.recordValue(qsi.reqStartTime - SimulationParameters.currTime);
						qsi.reqStartTime = SimulationParameters.currTime;
						qsi.reqArrivalTime = SimulationParameters.currTime;
					}
				}
				softServerSim.totalServerEnergy = 0.0;
			}
		}

		SimulationParameters.incrementIntervalSlotCounter();
		ModelParameters.arrivalRate = ModelParameters.getArrivalRate(SimulationParameters.getIntervalSlotCounter()); 
		SimulationParameters.offerEvent(
				new Event(SimulationParameters.currTime + SimulationParameters.getCurrentSlotLength(), EventType.ARRIVAL_RATE_CHANGES));
	}

	public void networkTaskStarts() throws Exception {
		SimulationParameters.currTime = timestamp;
		LanLinkSim ln = (LanLinkSim) SimulationParameters.distributedSystemSim.getLink(reqObj.linkName);
		ln.processTaskStartEvent(reqObj, SimulationParameters.currTime);
	}

	public void networkTaskEnds() throws Exception {
		SimulationParameters.currTime = timestamp;
		LanLinkSim ln = (LanLinkSim) SimulationParameters.distributedSystemSim.getLink(reqObj.linkName);
		ln.processTaskEndEvent(reqObj, reqObj.nwInstance, SimulationParameters.currTime);
	}

	/**
	 * requestCompleted
	 * 
	 * @param scenarioname
	 * 
	 *            Here we will calculate the average for (end to end) performance measures for scenario (corresponding to this request).
	 */
	public void requestCompleted() throws Exception {
		// get the request object
		SimulationParameters.currTime = timestamp;
		// Update the scenario Measures
		reqObj.scenario.updateMeasuresAtTheEndOfRequestCompletion(reqObj);

		if (ModelParameters.getSimulationEndTimeEnabled()) {
//			if (SimulationParameters.currentTime < ModelParameters.getSimulationEndTime()) {
				reqObj.processRequest(timestamp);
//			}
		} else {
			//following if is commented out as violation of that condition will result in a request disappearing from the simulation, and hence breaking the request generation chain, resulting in a limbo with no events
//			if ((SimulationParameters.getTotalRequestArrived() < ModelParameters.getTotalNumberOfRequests())) {
				reqObj.processRequest(timestamp);
//			}
		}

		// if end of simulation condition is satisfied, new simulation complete event is generated.
		checkForSimulationCompletionCriteria();
	}

	/**
	 * This method checks for the simulation completion criteria based on various conditions.
	 * 
	 * It checks for the end of simulation triggered by total number of requests processed. It also
	 * checks for the simendtime condition.
	 */
	private void checkForSimulationCompletionCriteria() {
//		if(SimulationParameters.currentTime > ModelParameters.getSimulationEndTime()) {
//			System.out.println("true");
//		}
		if(ModelParameters.getTotalNumberOfRequestEnabled()
				&& SimulationParameters.warmupEnabled
				&& ModelParameters.getStartUpSampleNumber() == SimulationParameters.getTotalRequestProcessed()) {
			SimulationParameters.offerEvent(new Event(SimulationParameters.currTime, EventType.WARMUP_ENDS));
//		} else if(ModelParameters.getTotalNumberOfRequestEnabled() &&
//				ModelParameters.getTotalNumberOfRequests() - ModelParameters.getCoolDownSampleNumber() == SimulationParameters.getTotalRequestProcessed()) {
//			SimulationParameters.offerEvent(new Event(SimulationParameters.currentTime, EventType.COOLDOWN_STARTS));
		} else if (ModelParameters.getTotalNumberOfRequestEnabled() && 
				SimulationParameters.getTotalRequestProcessed() >= ModelParameters.getTotalNumberOfRequests()) {
			SimulationParameters.offerEvent(new Event(SimulationParameters.currTime, EventType.SIMULATION_COMPLETE));
		} else if (ModelParameters.getSimulationEndTimeEnabled() && SimulationParameters.currTime >= ModelParameters.getSimulationEndTime()) {
			SimulationParameters.offerEvent(new Event(SimulationParameters.currTime, EventType.SIMULATION_COMPLETE));
		}
	}

	public void softResourceTaskStarts() throws Exception {
		SimulationParameters.currTime = timestamp;
		reqObj.machineObject.getSoftRes(reqObj.softResName).processTaskStartEvent(reqObj, SimulationParameters.currTime);
	}

	public void softResourceTaskEnds() throws Exception {
		SimulationParameters.currTime = timestamp;
		reqObj.machineObject.getSoftRes(reqObj.softResName).processTaskEndEvent(reqObj, reqObj.softResInstance, SimulationParameters.currTime);
	}

	/** This function is called when request is timed out in buffer */
	public void timeoutInBuffer() throws Exception {
		reqObj.timeoutFlagInBuffer = true;
		logger.debug("Timeout Occured for the Request : " + reqObj.id);

		// update the related book keeping structures.
		reqObj.scenario.noOfReqTimedoutInBuffer.recordValue(1);

		// Reduce the number of busy instances of softserver queue.
		QueueSim resourceQueue = (QueueSim) reqObj.machineObject.getServer(reqObj.softServName).resourceQueue;
		resourceQueue.numBusyInstances--;

		// Reset the queue server instance parameters.
		// This function skips the calculation of total busy time due to current request.
		int instanceID = reqObj.qServerInstanceID;
		resourceQueue.qServerInstances.get(instanceID).endServiceForInstanceInBuffTimeout(timestamp);

		// check if retry is done, and create a new request.
		reqObj.processRequest(timestamp);

		// if simulation end condition is satisfied, stop the simulation
		checkForSimulationCompletionCriteria();
	}

	/** For all power-managed devices this event is called in every probe interval */
	public void deviceProbe() throws Exception {
		SimulationParameters.currTime = timestamp;
		devObj.deviceProbeHandler(SimulationParameters.currTime, hostObj);
	}

	public String toString() {
		return timestamp + ":" + type.toString()+"\n";
	}

	public PhysicalMachineSim getHostObject() {
		return hostObj;
	}

	public void setHostObject(PhysicalMachineSim hostObject) {
		this.hostObj = hostObject;
	}

	public DeviceSim getDeviceObject() {
		return devObj;
	}

	public void setDeviceObject(DeviceSim deviceObject) {
		this.devObj = deviceObject;
	}

	public Request getRequestObject() {
		return reqObj;
	}

	public void setRequestObject(Request requestObject) {
		this.reqObj = requestObject;
	}
}
