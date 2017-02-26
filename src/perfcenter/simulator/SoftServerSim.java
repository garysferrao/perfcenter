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
import perfcenter.baseclass.DeviceCategory;
import perfcenter.baseclass.exception.DeviceNotFoundException;
import perfcenter.simulator.metric.TimeAverageMetric;
import perfcenter.simulator.queue.QueueServer;
import perfcenter.simulator.queue.QueueSim;
import perfcenter.simulator.request.Request;
import perfcenter.simulator.request.SyncRequest;
import perfcenter.simulator.request.SoftResVector;

import static perfcenter.simulator.DistributedSystemSim.computeConfIvalForMetric;

public class SoftServerSim extends SoftServer implements QueueServer {
	
	public ArrayList<PhysicalMachineSim> hostObjects = new ArrayList<PhysicalMachineSim>();
	
	private double currRamUtil = 0.0;
	
	TimeAverageMetric avgRamUtilSim = new TimeAverageMetric(0.95);
	static int count = 0;
	public SoftServerSim(SoftServer s) {
		name = s.name;
		size = s.size;
		threadSize = s.threadSize;
		thrdCount = s.thrdCount;
		thrdBuffer = s.thrdBuffer;
		schedp = s.schedp;
		tasks = s.tasks;
		
		
		// dynamically load the scheduling policy class
		resourceQueue = QueueSim.loadSchedulingPolicyClass(schedp.toString(), (int) thrdBuffer.getValue(), (int) thrdCount.getValue(), /* "swRes", */this);

		resourceQueue.initialize();
		machines = s.machines;
		for (Task t : tasks) {
			t.initialize();
		}
	}

	public SoftServerSim(String name) {
		super(name);
	}
	
	public double getCurrRamUtil(){
		return currRamUtil;
	}

	/**
	 * A Software server can be deployed on more than one host. This function randomly returns name of one such host. this function is used by
	 * simulation part
	 */ 
	public PhysicalMachineSim getRandomHostObject() throws Exception {
		// String hostN="-1";

		// generate a random number(d) between 0 and 1
		// Random r = new Random();
		if (hostObjects.size() == 0)
			throw new Exception("SoftServer " + name + " is not deployed on any hosts");
		
		double d = r.nextDouble();
		int idx = (int)d*hostObjects.size();
		return hostObjects.get(idx);
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
		if (req.nextTaskNode != null) {
			if (req.nextTaskNode.issync == true) {
				/**
				 * chk this for sync request reply softwareArrival time , softwareStart time. bug fixed (params to syncreq changed)- akhila
				 */
				
				SyncRequest sr = new SyncRequest(req.machineName, req.softServName, req.taskName, req.threadNum, req.id, req.softServArrivalTime,
						req.softServStartTime);
				req.synReqVector.add(sr);
			}
		}

		SimulationParameters.offerEvent(ev);

	}
	
	/* Here, every time we compute ram utilization for ground up. Only problem is we don't know which different tasknodes 
	 * are occupying busy instance of resourceQueue which help in correctly computing correct request size based on req.nwDataSize
	 */
	private void updateRamUtil(Request req){
		double ramutil = 0.0;
		ramutil += size.getValue();
		/* Add static size of server to ram util */
		//FIXME: multiplying current nwDataSize(or in its absence, with 200)  with number of busy instances needs to be corrected.
		/* Add Ram Utilization by scheduled or busy threads */
		if(req == null){
			ramutil += ((200 + threadSize.getValue()) * (((QueueSim)resourceQueue).numBusyInstances));
		}else if(req.currentTaskNode == null){	//FIXME: Find out reasons why a request with no task assigned will be queued
			return;
		}else{
			ramutil += ((req.currentTaskNode.pktsize.getValue() + threadSize.getValue()) * (((QueueSim)resourceQueue).numBusyInstances));
		}
		
		/* Add Ram Utilization by buffered threads */
		ramutil += (threadSize.getValue() * (((QueueSim)resourceQueue).getBuffersize()));
		//if(req != null)
		//System.out.println("Name:" + name + " request.nwDataSize:" + req.currentTaskNode.pktsize.getValue() +" size:" + size.getValue() + " threadSize:" + threadSize.getValue() +" Ram Util:" + ramutil);
		
		currRamUtil = ramutil;
		avgRamUtilSim.recordValue(this.currRamUtil);
		
		/* Update host RAM utilization */
		for(String mname : machines){
			SimulationParameters.distributedSystemSim.machineMap.get(mname).updateRamUtil(req);
		}
	}

	public void enqueue(Request req, double time) throws Exception {
		((QueueSim) resourceQueue).enqueue(req, time);
		updateRamUtil(req);
	}

	public void endService(Request req,int instanceId, double time) throws Exception {
		((QueueSim) resourceQueue).endService(req, instanceId, time);
		updateRamUtil(req);
	}

	public void endServiceTimeout(int instanceId, double time) throws Exception {
		((QueueSim) resourceQueue).endServiceTimeout(instanceId, time);
		updateRamUtil(null);
	}

	public void dequeue() {
		
	}

	/* This method is called only once in the life-line of a request. 
	 * When SOFTWARE_TASK_STARTS event is handled, this method is called to enqueue it to DeviceSim for first time.
	 * Later, the request will be transferred from one device to another while handling events HARDWARE_TASK_ENDS and SOFTWARE_TASK_ENDS 
	 */
	public int offerRequestToDevice(Request req, double currTime) throws Exception {
		try {
			// get the task name
			req.setRequestFromTask();
			
			Task t = getTaskObject(req.taskName);
			//System.out.println("Softserversim.offerRequestToDevice() TaskName:" + req.taskName );
			DeviceCategory nextDevCat = t.getNextDeviceCategory(req.getSubTaskIdx());
			//if next device does not exist return -1
			if(nextDevCat == null)
				return -1;
			
			// get the next device name
			
			String nextDeviceName = SimulationParameters.distributedSystemSim.machineMap.get(req.machineName).getDeviceName(nextDevCat);
			//String nextDeviceName = getDeviceName(nextDevCat, req.machineObject );
			if(nextDeviceName == null)
				return -1;
		
			// update request with the device name
			req.devName = nextDeviceName;
			
			// Keeps track of the current request is for which server. Nikhil
			// System.out.println("Req Id:"+req.id+" ,m assigned to:"+name);
			req.fromServer = name;
			
			// get to the device queue
			DeviceSim deviceSim = SimulationParameters.distributedSystemSim.machineMap.get(req.machineName).getDevice(req.devName);
			// set the value for total service demand on hw resource
			req.serviceTimeRemaining = t.getServiceTimeDist(req.getSubTaskIdx()).nextRandomVal(deviceSim.speedUpFactor.getValue());

			// add request to device queue
			deviceSim.enqueue(req, currTime);
			// successful return
			return 0;

		} catch (DeviceNotFoundException e) {
			req.setSubTaskIdx(req.getSubTaskIdx() + 1, "SoftServerSim:offerRequestToDevice");
			return offerRequestToDevice(req, currTime);
			// } catch (Exception e) {
			// throw new Exception(e.getMessage()); //Bhavin
		}

	}

	void recordCISampleAtTheEndOfSimulation() {
		((QueueSim) resourceQueue).recordCISampleAtTheEndOfSimulation();
		
		/* Recoding CI for ramUtilSim */
		updateRamUtil(null);
		for (int slot = 0; slot < ModelParameters.intervalSlotCount; slot++) {
			if ((int) ((QueueSim)resourceQueue).totalNumberOfRequestsServed.getTotalValue(slot) > 0) {
				avgRamUtilSim.recordCISample(slot);
			}
		}
	}

	// even when the request is dropped it contributes to busy time and
	// thus utilization.
	public void abortThread(int threadNum, double time) {
		((QueueSim) resourceQueue).discard(threadNum, time);
		updateRamUtil(null);
	}

	public void processTaskEndEventTimeout(Request rq, int instanceId, double currTime) throws Exception {
		endServiceTimeout(instanceId, currTime);
	}

	// This function processes Software Task Ends event
	public void processTaskEndEvent(Request rq, int instanceId, double currTime) throws Exception {
		//System.out.println("In SoftServerSim.processTaskEndEvent(): Before: "  + name + " : " + ((QueueSim)resourceQueue).qServerInstances.size() + " : " + ((QueueSim)resourceQueue).freeQServerInstances.size());
		SoftResVector sr1 = rq.virtResStack.pop();
		if (sr1.softResName_.compareToIgnoreCase(rq.taskName) != 0) {
			throw new Exception("wrong stack");
		}

		// update the total energy consumption of the server
		totalServerEnergy += rq.energyConsumed;
		rq.energyConsumed = 0.0;
		/**
		 * if request (i.e. compound task) is done (i.e. there are no more synch calls), then states are updated and resources freed else nothing is
		 * done ...the thread held simply blocks
		 */
		if (!rq.isSyncRequest(rq.softServName, rq.taskName, instanceId, rq.id, rq.softServArrivalTime, rq.softServStartTime)) {

			// queue book keeping structures are updated`and next thread is scheduled
			endService(rq,instanceId, currTime);
		} else {
			//System.err.println("Thread blocked permanently on a request - This usually means wrong configuration of SYNC parameter in the Scenario block of input file. Response from downstream server to upstream server (db to web) should not be SYNC!!");
		}

		// ScenarioSim sce = (ScenarioSim) SimulationParameters.distributedSystemSim.getScenario(rq.scenarioName);

		// check if the request is timed out during service,
		// if so, then do not process the request further and declare the request as done with timeout flag set.
		if (ModelParameters.timeoutEnabled == true) {
			if ((SimulationParameters.currTime >= rq.scenarioTimeout)) {
				rq.timeoutFlagAfterService = true;
				Event ev = new Event(SimulationParameters.currTime, EventType.REQUEST_DONE, rq);
				SimulationParameters.offerEvent(ev);
				return;
			}
		}

		// check if next node exist
		if ((rq.nextTaskNode == null) || (rq.nextTaskNode.name.compareToIgnoreCase("user") == 0)) {
			// this was the last node.. now create a new event of request
			// completed.
			Event ev = new Event(SimulationParameters.currTime, EventType.REQUEST_DONE, rq);
			SimulationParameters.offerEvent(ev);
		} else {
			offerRequestToNextNode(rq);
		}
		//System.out.println("In SoftServerSim.processTaskEndEvent(): After: "  + name + " : " + ((QueueSim)resourceQueue).qServerInstances.size() + " : " + ((QueueSim)resourceQueue).freeQServerInstances.size());
	}

	// request is offered to the hardware device
	public void processTaskStartEvent(Request r, double currTime) throws Exception {
			
		if (r.virtResStack.isEmpty() == false) {
			throw new Exception("Stack not empty");
		} // FIXME: remove comments: find out why this check is put here? // CHECK

		// task is the first virtual resource hence just add it
		// to the stack
		SoftResVector vr = new SoftResVector(r.softResInstance, r.softResIdx, r.taskName, null);
		r.virtResStack.push(vr);

		// offer request to the first device belonging to task
		int retval = offerRequestToDevice(r, currTime);
		//System.out.println("In processTaskStartEvent():" + retval);
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
		if(SimulationParameters.distributedSystemSim.serverMigrated(rq.softServName)){
			String newname = SimulationParameters.distributedSystemSim.softServerMap.get(rq.softServName).machines.get(0);
			if(newname.compareTo(rq.machineName) != 0){
				System.out.println("offerRequestToLink():servername:" + rq.softServName + " oldMachineName:" + rq.machineName + " machineName:" + newname  + " tasknodename:" + rq.taskName);
				rq.machineName = newname;
			}
		}
		destLanName = SimulationParameters.distributedSystemSim.machineMap.get(rq.machineName).lan;

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
		rq.nwDataSize = (int) rq.currentTaskNode.pktsize.getValue();
		rq.srcLanName = srcLanName;
		rq.destLanName = destLanName;
		lks.enqueue(rq, SimulationParameters.currTime);
		return true;
	}

	// request is offered to next node
	public void offerRequestToNextNode(Request rq) throws Exception {
		String srcLanName;
		srcLanName = SimulationParameters.distributedSystemSim.machineMap.get(rq.machineName).lan;

		rq.currentTaskNode = rq.nextTaskNode;
		rq.nextTaskNode = SimulationParameters.distributedSystemSim.findNextTaskNode(rq.currentTaskNode);
		//System.out.println("While handling software task starts event of request " + rq.toString() +" setting currentTaskNode " + rq.currentTaskNode.name + " to " + rq.nextTaskNode.name);
		rq.taskName = rq.currentTaskNode.name;
		rq.setSubTaskIdx(0, "SoftServerSim:offerRequestToNextNode");
		rq.devInstance = 0;
		rq.softResIdx = 0;
		rq.softResInstance = 0;
		rq.clearRequestFromFlags();
		rq.softServName = rq.currentTaskNode.servername;

		// Check whether the request is a reply for a previous sync request.
		if (rq.isSyncReply(rq.softServName)) {
			// this is sync reply

			// retrieve sync request state saved on the vector to
			// get information about the thread etc. to be used
			// and remove it from the vector
			rq.softServArrivalTime = rq.getServerArrivalTime(rq.softServName, rq.id);
			rq.softServStartTime = rq.getServerStartTime(rq.softServName, rq.id);

			// get the thread num where compound task had started execution
			rq.threadNum = rq.getThreadNum(rq.softServName, rq.id);

			// the host name on which a sync req ends should be same
			// as the host that has started the sync req. Akhila
			rq.setHost(rq.getHostName(rq.softServName, rq.id));

			// check if link exists between the nodes. if it exists
			// request is offered to link
			boolean isReqOfferedToLink = offerRequestToLink(srcLanName, rq);
			if (!isReqOfferedToLink) {
				rq.synReqVector.remove(rq.synReqVector.size() - 1);

				// This request is not added to queue, but processing begins
				// immediately
				PhysicalMachineSim machineObject = SimulationParameters.distributedSystemSim.machineMap.get(rq.machineName);
				machineObject.getServer(rq.softServName).createStartTaskEvent(rq, rq.threadNum, SimulationParameters.currTime);
			}

		} else {
			// If soft server is deployed on more than one host then get a random host name
			String newmname = SimulationParameters.distributedSystemSim.softServerMap.get(rq.softServName).machines.get(0);
			if(SimulationParameters.migrationHappend){
				
				if(newmname.compareTo(rq.machineName) != 0){
					rq.machineName = newmname;
					//System.out.println("AFTER:ReqObj.id:" + reqObj.id + " servername:" + reqObj.softServName + " machineName:" + reqObj.machineName);
				}
			}
			rq.machineName = SimulationParameters.distributedSystemSim.getServer(rq.softServName).getRandomHostObject().name;
			// get soft server
			// this is not sync reply, but an ordinary request
			rq.softServArrivalTime = SimulationParameters.currTime;

			// offer request to link
			boolean isReqOfferedToLink = offerRequestToLink(srcLanName, rq);
			if (!isReqOfferedToLink) {
				// link not present. add to software queue for processing
				PhysicalMachineSim machineObject = SimulationParameters.distributedSystemSim.machineMap.get(rq.machineName);
				machineObject.getServer(rq.softServName).enqueue(rq, SimulationParameters.currTime);
			}
		}
	}
	
	public void clearValuesButKeepConfIvals() {
		((QueueSim) resourceQueue).clearValuesButKeepConfIvals();
		avgRamUtilSim.clearValuesButKeepConfInts();
	}
	
	 public void computeConfIvalsAtEndOfRepl() {
		 ((QueueSim) resourceQueue).computeConfIvalsAtEndOfRepl();
		computeConfIvalForMetric(ramUtil, avgRamUtilSim);
	 }
}
