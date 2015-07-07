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
 * Inherits from LanLink. Has more functions for simulation
 * @author  akhila
 */
package perfcenter.simulator;

import perfcenter.baseclass.Helper;
import perfcenter.baseclass.LanLink;
import perfcenter.simulator.queue.QueueServer;
import perfcenter.simulator.queue.QueueSim;
import perfcenter.simulator.request.Request;

/** Inherits from LanLink. Has some more functions for simulation. */
public class LanLinkSim extends LanLink implements QueueServer {

	// constructor
	public LanLinkSim(LanLink lk) {
		name = lk.getName();
		srclan = lk.getSrcLanName();
		destlan = lk.getDestLanName();
		mtu = lk.mtu;
		trans = lk.trans;
		prop = lk.prop;
		headerSize = lk.headerSize;
		mtuUnit = lk.mtuUnit;
		transUnit = lk.transUnit;
		propUnit = lk.propUnit;
		headerSizeUnit = lk.headerSizeUnit;
		buffer = lk.buffer;
		pol = lk.pol;
		// Dynamically load the scheduling policy class based on policy name
		// given
		// in input file
		linkQforward = QueueSim.loadSchedulingPolicyClass(pol.toString(), (int) buffer.getValue(), 1,/* "hwRes", */
				this);
		linkQreverse = QueueSim.loadSchedulingPolicyClass(pol.toString(), (int) buffer.getValue(), 1,/* "hwRes", */
				this);
	}

	// this generates network task starts event
	public void createStartTaskEvent(Request req, int idleInstanceId, double currTime) {
		// set the request device Instance
		req.nwInstance = idleInstanceId;
		Event ev = new Event(currTime, EventType.NETWORK_TASK_STARTS, req);
		SimulationParameters.offerEvent(ev);
	}

	public void enqueue(Request req, double time) throws Exception {
		if (isForwardLink(req.srcLanName, req.destLanName) == true) {
			((QueueSim) linkQforward).enqueue(req, time);
		} else {
			((QueueSim) linkQreverse).enqueue(req, time);
		}
	}

	public void dequeue() {

	}

	public void endService(Request request,String srclan, String destlan, int instanceId, double time) throws Exception {
		if (isForwardLink(srclan, destlan) == true) {
			((QueueSim) linkQforward).endService(request, instanceId, time);
		} else {
			((QueueSim) linkQreverse).endService(request, instanceId, time);
		}
	}

	// called at end of simulation. the queue performance parameters are updated
	void recordCISampleAtTheEndOfSimulation() {
		((QueueSim) linkQforward).recordCISampleAtTheEndOfSimulation();
		((QueueSim) linkQreverse).recordCISampleAtTheEndOfSimulation();
	}

	// this is processing of network task end event. It begins processing
	// of next software task
	public void processTaskEndEvent(Request rq, int instanceId, double currTime) throws Exception {
		try {
			// end service for the queue
			endService(rq,rq.srcLanName, rq.destLanName, instanceId, SimulationParameters.currentTime);

			// get the soft server
			SoftServerSim s = rq.hostObject.getServer(rq.softServerName);

			if (rq.isSyncReply(rq.softServerName)) {
				// if it is reply to sync call remove from the vector
				rq.synReqVector.remove(rq.synReqVector.size() - 1);

				// This request is not added to queue, but processing begins
				// immediately
				s.createStartTaskEvent(rq, rq.threadNum, SimulationParameters.currentTime);
			} else {
				// add request to the queue of soft server
				s.enqueue(rq, SimulationParameters.currentTime);
			}
		} catch (Exception e) {
			throw e;
		}

	}

	// Processes the lan link task end event:
	// when lan link task ends then network task end event is generated
	public void processTaskStartEvent(Request r, double currTime) throws Exception {
		LanLinkSim s = (LanLinkSim) SimulationParameters.distributedSystemSim.getLink(r.linkName);

		// find number of packets
		int packets = (int) r.nwDataSize / (int) s.mtu.getValue();
		if (r.nwDataSize % s.mtu.getValue() != 0)
			packets++;

		// add header to each packet, and find total data size
		int datasize = r.nwDataSize + packets * (int) s.headerSize.getValue();

		// data size in bits
		datasize = datasize * 8;

		// transmission delay in seconds
		double transdelay = datasize / Helper.convertTobps(s.trans.getValue(), s.transUnit);

		// propagation delay in seconds
		double propdelay = Helper.convertToSeconds(s.prop.getValue(), s.propUnit);

		// calculate end time. and add new event in the event list
		// double endTime = SimParams.currentTime + transdelay + propdelay;
		ExponentialDistribution exp = new ExponentialDistribution();
		double endTime = SimulationParameters.currentTime + exp.nextExp(transdelay) + propdelay;// modified by niranjan
		Event ev = new Event(endTime, EventType.NETWORK_TASK_ENDS, r);
		SimulationParameters.offerEvent(ev);

	}

	public void dropRequest(Request r, double currTime) throws Exception {
		// if the resource is a hardware resource like cpu or network resource
		// then while discarding the request
		// 1. We need to take care of the thread allocated to the request
		// 2. Take care of contributions made to performance measures of
		// current software server

		r.hostObject.getServer(r.softServerName).abortThread(r.threadNum, SimulationParameters.currentTime);
		r.drop();
	}
}
