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
 * Contains simulation book-keepings for the VirtualResSim object.
 * @author  akhila
 */
import perfcenter.baseclass.Variable;
import perfcenter.baseclass.SoftResource;
import perfcenter.baseclass.enums.SchedulingPolicy;
import perfcenter.simulator.queue.QueueServer;
import perfcenter.simulator.queue.QueueSim;
import perfcenter.simulator.request.Request;
import perfcenter.simulator.request.SoftResVector;

public class SoftResSim extends SoftResource implements QueueServer {
	public SoftResSim(String srname, Variable srcount, Variable srbuffer, SchedulingPolicy srpol) {
		this.name = srname;
		count = srcount;
		buffer = srbuffer;
		pol = srpol;
		// dynamically load the scheduling policy class
		resourceQueue = QueueSim.loadSchedulingPolicyClass(pol.toString(), (int) buffer.getValue(), (int) count.getValue(),
		/* "swRes", */this);

	}

	// generates virtual res task starts event. This method is called by queue
	public void createStartTaskEvent(Request req, int idleInstanceId, double currTime) {
		// set the request device Instance
		req.softResInstance = idleInstanceId;
		Event ev = new Event(currTime, EventType.SOFTRES_TASK_STARTS, req);

		SimulationParameters.offerEvent(ev);
	}

	public void enqueue(Request req, double time) throws Exception {
		((QueueSim) resourceQueue).enqueue(req, time);
	}

	public void dequeue() {

	}

	// even when the request is dropped it contributes to busy time and
	// thus utilization.
	public void abort(int instanceid, double time) {
		((QueueSim) resourceQueue).discard(instanceid, time);
	}

	// process virtual resource task end
	public void processTaskEndEvent(Request rq, int instanceId, double currTime) throws Exception {
		SoftResVector vr1 = rq.virtResStack.pop();
		if (vr1.softResName_.compareToIgnoreCase(rq.softResName) == 0) {
			// queue book keeping structures are updated
			endService(rq,instanceId, currTime);
		} else {// ARCHITECTURE: handle this well
			throw new Exception("wrong stack");
		}

		// the stack was not empty. pop the virtual resource and process it
		boolean nextSoftResFound2 = false;
		SoftResVector vr = rq.virtResStack.pop();
		rq.softResName = vr.softResName_;
		rq.softResInstance = vr.instanceNo_;
		rq.softResIdx = vr.virtResIndex_;
		vr.virtResIndex_++;
		rq.virtResStack.push(vr);

		rq.softResIdx = rq.softResIdx + 1;

		// Offer request to next virtual resource
		nextSoftResFound2 = rq.machineObject.offerRequestToSoftRes(rq, SimulationParameters.currTime);
		if (nextSoftResFound2 == true) {
			return;
		}

		// there is no next virtual res
		if (rq.virtResStack.size() == 1) {
			Event ev2 = new Event(SimulationParameters.currTime, EventType.SOFTWARE_TASK_ENDS, rq);
			SimulationParameters.offerEvent(ev2);
			return;
		} else {
			Event ev = new Event(SimulationParameters.currTime, EventType.SOFTRES_TASK_ENDS, rq);
			SimulationParameters.offerEvent(ev);
			return;
		}

	}

	// process virtual resource task start
	public void processTaskStartEvent(Request r, double currTime) throws Exception {
		// offer the request to virtual resource device. before offering
		// put the virtual res into the stack
		MachineSim hs = r.machineObject;
		r.setRequestFromVirtRes();
		r.setSubTaskIdx(0, "VirtualResSim:processTaskStartEvent");
		SoftResVector vr = new SoftResVector(r.softResInstance, r.softResIdx, r.softResName, hs);
		r.virtResStack.push(vr);
		hs.offerReqToNextDevice(r, currTime);

	}

	// this is called by the queue and also from deviceSim
	public void dropRequest(Request r, double currTime) throws Exception {
		r.machineObject.getServer(r.softServName).abortThread(r.threadNum, SimulationParameters.currTime);
		r.drop();
	}

	public void endService(Request request,int instanceId, double time) throws Exception {
		((QueueSim) resourceQueue).endService(request, instanceId, time);
	}
}
