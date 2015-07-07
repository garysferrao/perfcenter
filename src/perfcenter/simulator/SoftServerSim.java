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

/**
 * Contains simulation book-keepings for the SoftServer objects.
 * @author  akhila
 */
import java.util.ArrayList;

import perfcenter.baseclass.ModelParameters;
import perfcenter.baseclass.SoftServer;
import perfcenter.baseclass.Task;
import perfcenter.baseclass.exception.DeviceNotFoundException;
import perfcenter.simulator.queue.QueueServer;
import perfcenter.simulator.queue.QueueSim;
import perfcenter.simulator.request.Request;
import perfcenter.simulator.request.SyncRequest;
import perfcenter.simulator.request.VirResVector;

public class SoftServerSim extends SoftServer implements QueueServer {
	
	public ArrayList<HostSim> hostObjects = new ArrayList<HostSim>();

	static int count = 0;
	public SoftServerSim(SoftServer s) {
		name = s.name;
		thrdCount = s.thrdCount;
		thrdBuffer = s.thrdBuffer;
		schedp = s.schedp;
		simpleTasks = s.simpleTasks;

		// dynamically load the scheduling policy class
		resourceQueue = QueueSim.loadSchedulingPolicyClass(schedp.toString(), (int) thrdBuffer.getValue(), (int) thrdCount.getValue(), /* "swRes", */this);

		resourceQueue.initialize();
		hosts = s.hosts;
		for (Task t : simpleTasks) {
			t.initialize();
		}
	}

	public SoftServerSim(String name) {
		super(name);
	}

	/**
	 * A Software server can be deployed on more than one host. This function randomly returns name of one such host. this function is used by
	 * simulation part
	 */ 
	public HostSim getRandomHostObject() throws Exception {
		// String hostN="-1";

		// generate a random number(d) between 0 and 1
		// Random r = new Random();
		if (hostObjects.size() == 0)
			throw new Exception("SoftServer " + name + " is not deployed on any hosts");
		
		double d = r.nextDouble();
		double prob = 1.0 / (double) hostObjects.size();
		double val = 0;
		for (HostSim host : hostObjects) {
			val += prob;
			if (val >= d)
				return host;
		}

		return hostObjects.get(hostObjects.size()-1);
	}

	
	/**
	 *  generates software task starts event. This method is called by queue.
	 */
	public void createStartTaskEvent(Request req, int threadNum, double currTime) {

		// creating new event of type SOFTWARE_TASK_STARTS
		Event ev = new Event(currTime, EventType.SOFTWARE_TASK_STARTS, req);

		// this is the thread on which will serve the request
		req.threadNum = threadNum;

		/*
		 * here we check if the task makes sync call. IF it does, we save its state(the thread it is holding ) in a vector, for retrieval when the
		 * reply to the sync call is received
		 */
		if (req.nextNode != null) {
			if (req.nextNode.issync == true) {
				/**
				 * chk this for sync request reply softwareArrival time , softwareStart time. bug fixed (params to syncreq changed)- akhila
				 */
				SyncRequest sr = new SyncRequest(req.hostObject, req.softServerName, req.taskName, req.threadNum, req.id, req.softServerArrivalTime,
						req.softServerStartTime);
				req.synReqVector.add(sr);
			}
		}

		SimulationParameters.offerEvent(ev);

	}

	public void enqueue(Request req, double time) throws Exception {
		((QueueSim) resourceQueue).enqueue(req, time);
	}

	public void endService(Request request,int instanceId, double time) throws Exception {
		((QueueSim) resourceQueue).endService(request, instanceId, time);
	}

	public void endServiceTimeout(int instanceId, double time) throws Exception {
		((QueueSim) resourceQueue).endServiceTimeout(instanceId, time);
	}

	// not called anytime

	public void dequeue() {
	}

	// checks if next device exists. if it exists puts request into
	// device queue and returns 0 else returns -1
	public int offerRequestToDevice(Request req, double currTime) throws Exception {
		try {

			// get the task name
			req.setRequestFromTask();
			Task task = getSimpleTask(req.taskName);

			// get the next device name
			String resName = task.getNextDeviceName(req.getDeviceIndex());
			// System.out.println("ResName : " + resName);
			// if next device does not exist return -1
			if (resName == null) {
				return -1;
			}
			// if it exists then put request in device Q

			// update request with the device name
			req.devName = resName;

			// Keeps track of the current request is for which server. Nikhil
			// System.out.println("Req Id:"+req.id+" ,m assigned to:"+name);
			req.fromServer = name;
			
			// get to the device queue
			DeviceSim deviceSim = req.hostObject.getDevice(req.devName);

			// set the value for total service demand on hw resource
			req.serviceTimeRemaining = task.getServiceTime(req.devName).nextRandomVal(deviceSim.speedUpFactor.getValue());

			// add request to device queue
			deviceSim.enqueue(req, currTime);

			// successful return
			return 0;

		} catch (DeviceNotFoundException e) {
			req.setDeviceIndex(req.getDeviceIndex() + 1, "SoftServerSim:offerRequestToDevice");
			return offerRequestToDevice(req, currTime);
			// } catch (Exception e) {
			// throw new Exception(e.getMessage()); //Bhavin
		}

	}

	void recordCISampleAtTheEndOfSimulation() {
		((QueueSim) resourceQueue).recordCISampleAtTheEndOfSimulation();
	}

	// even when the request is dropped it contributes to busy time and
	// thus utilization.
	public void abortThread(int threadNum, double time) {
		((QueueSim) resourceQueue).discard(threadNum, time);
	}

	public void processTaskEndEventTimeout(Request rq, int instanceId, double currTime) throws Exception {
		endServiceTimeout(instanceId, currTime);
	}

	// this processing for software task ends.
	// when software task ends

	public void processTaskEndEvent(Request rq, int instanceId, double currTime) throws Exception {

		VirResVector vr1 = rq.virtResStack.pop();
		if (vr1.virtResName_.compareToIgnoreCase(rq.taskName) != 0) {
			throw new Exception("wrong stack");
		}

		// update the total energy consumption of the server
		totalServerEnergy += rq.energyConsumed;
		rq.energyConsumed = 0.0;
		/**
		 * if request (i.e. compound task) is done (i.e. there are no more synch calls), then stats are updated and resources freed else nothing is
		 * done ...the thread held simply blocks
		 */
		if (!rq.isSyncRequest(rq.softServerName, rq.taskName, instanceId, rq.id, rq.softServerArrivalTime, rq.softServerStartTime)) {

			// queue book keeping structures are updated
			endService(rq,instanceId, currTime);
		} else {
			//System.err.println("Thread blocked permanently on a request - This usually means wrong configuration of SYNC parameter in the Scenario block of input file. Response from downstream server to upstream server (db to web) should not be SYNC!!");
		}

		// ScenarioSim sce = (ScenarioSim) SimulationParameters.distributedSystemSim.getScenario(rq.scenarioName);

		// check if the request is timed out during service,
		// if so, then do not process the request further and declear the request as done with timeout flag set.
		if (ModelParameters.timeoutEnabled == true) {
			if ((SimulationParameters.currentTime >= rq.scenarioTimeout)) {
				rq.timeoutFlagAfterService = true;
				Event ev = new Event(SimulationParameters.currentTime, EventType.REQUEST_DONE, rq);
				SimulationParameters.offerEvent(ev);
				return;
			}
		}

		// check if next node exist
		if ((rq.nextNode == null) || (rq.nextNode.name.compareToIgnoreCase("user") == 0)) {
			// this was the last node.. now create a new event of request
			// completed.
			Event ev = new Event(SimulationParameters.currentTime, EventType.REQUEST_DONE, rq);
			SimulationParameters.offerEvent(ev);
		} else {
			// System.out.println("Calling for : "+ rq.nextNode.name);
			offerRequestToNextNode(rq);
		}

	}

	// request is offered to the hardware device
	public void processTaskStartEvent(Request r, double currTime) throws Exception {

		if (r.virtResStack.isEmpty() == false) {
			throw new Exception("Stack not empty");
		} // FIXME: remove comments: find out why this check is put here?

		// task is the first virtual resource hence just add it
		// to the stack
		VirResVector vr = new VirResVector(r.virtualResInstance, r.virtualResIndex, r.taskName, null);
		r.virtResStack.push(vr);

		// offer request to the first device belonging to task
		int retval = offerRequestToDevice(r, currTime);
		if (retval == -1) {
			// throw new Exception("No devices found for task " + r.taskName); //Not sure whether this is correct
			// FIXME: why the check is here? remove it after "appropriate" inspection
		}
	}

	public void dropRequest(Request request, double currTime) throws Exception {
		request.drop();
	}

	// if request is not offered returns false
	// if request is added to lan link queue returns true
	public boolean offerRequestToLink(String srcLanName, Request rq) throws Exception {
		String destLanName;
		destLanName = rq.hostObject.lan;

		// hosts are not deployed on to any lan
		if ((destLanName.length() <= 0) || (srcLanName.length() <= 0)) {
			return false;
		}
		// both the hosts are on same lan
		if (srcLanName.compareToIgnoreCase(destLanName) == 0) {
			return false;
		}

		// both hosts are deployed on different lans get details and
		// add to link queue
		LanLinkSim lks = (LanLinkSim) SimulationParameters.distributedSystemSim.getLink(srcLanName, destLanName);
		rq.linkName = lks.getName();
		rq.nwDataSize = (int) rq.currentNode.pktsize.getValue();
		rq.srcLanName = srcLanName;
		rq.destLanName = destLanName;
		lks.enqueue(rq, SimulationParameters.currentTime);
		return true;
	}

	// request is offered to next node
	public void offerRequestToNextNode(Request rq) throws Exception {
		String srcLanName;
		srcLanName = rq.hostObject.lan;

		rq.currentNode = rq.nextNode;
		rq.nextNode = SimulationParameters.distributedSystemSim.findNextNode(rq.currentNode);
		rq.taskName = rq.currentNode.name;
		rq.setDeviceIndex(0, "SoftServerSim:offerRequestToNextNode");
		rq.devInstance = 0;
		rq.virtualResIndex = 0;
		rq.virtualResInstance = 0;
		rq.clearRequestFromFlags();
		rq.softServerName = rq.currentNode.servername;

		// here we detect whether the request is a reply for a
		// previous sync request.
		if (rq.isSyncReply(rq.softServerName)) {
			// this is sync reply

			// retrieve sync request state saved on the vector to
			// get information about the thread etc. to be used
			// and remove it from the vector
			rq.softServerArrivalTime = rq.getServerArrivalTime(rq.softServerName, rq.id);
			rq.softServerStartTime = rq.getServerStartTime(rq.softServerName, rq.id);

			// get the thread num where compound task had started execution
			rq.threadNum = rq.getThreadNum(rq.softServerName, rq.id);

			// the host name on which a sync req ends should be same
			// as the host that has started the sync req. Akhila
			rq.setHostName(rq.getHostName(rq.softServerName, rq.id));

			// check if link exists between the nodes. if it exists
			// request is offered to link
			boolean isReqOfferedToLink = offerRequestToLink(srcLanName, rq);
			if (!isReqOfferedToLink) {
				rq.synReqVector.remove(rq.synReqVector.size() - 1);

				// This request is not added to queue, but processing begins
				// immediately
				rq.hostObject.getServer(rq.softServerName).createStartTaskEvent(rq, rq.threadNum, SimulationParameters.currentTime);
			}

		} else {
			// If soft server is deployed on more than one host then get a random
			// host name
			rq.hostObject = SimulationParameters.distributedSystemSim.getServer(rq.softServerName).getRandomHostObject();
			// get soft server
			// this is not sync reply, but an ordinary request
			rq.softServerArrivalTime = SimulationParameters.currentTime;

			// offer request to link
			boolean isReqOfferedToLink = offerRequestToLink(srcLanName, rq);
			if (!isReqOfferedToLink) {
				// link not present. add to software queue for processing
				rq.hostObject.getServer(rq.softServerName).enqueue(rq, SimulationParameters.currentTime);
			}
		}
	}
	
	public void clearValuesButKeepConfInts() {
		((QueueSim) resourceQueue).clearValuesButKeepConfInts();
	}
}
