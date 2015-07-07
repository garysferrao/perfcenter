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
package perfcenter.simulator.queue.SchedulingStratergy;

import perfcenter.simulator.queue.QueueServer;
import perfcenter.simulator.queue.QueueSim;
import perfcenter.simulator.request.Request;

/**
 * This class implements the scheduling policy of LCFS. It manages the enqueue and dequeue operations.
 *
 */
public class LCFS extends QueueSim {

	public LCFS(Integer buffSize, Integer numInstances, /* String resType, */QueueServer qs) {
		super(buffSize, numInstances,/* resType, */qs);
	}

	public void enqueue(Request req, double currTime) throws Exception {
		// chk if any instance of resource free
		int idleDeviceId = getIdleInstanceId();

		// mark time when request enters the system
		processRequestArrival(req, idleDeviceId, currTime);

		if (idleDeviceId == -1) {
			// no instance of device free
			// chk if buffer is not full
			if (!isBufferFull()) {
				// buffer not full so add to buffer
				addRequestToBuffer(req, currTime);

			} else {
				// discard request if all buffers full
				totalNumberOfRequestsBlocked.recordValue(req,1);
				qServer.dropRequest(req, currTime);

			}
		} else {
			// some instance of device(idleDev) is free
			// so now we schedule the request
			createStartTaskEvent(req, idleDeviceId, currTime);
			// update the average waiting time for this resource
			averageWaitingTimeSim.recordValue(req,qServerInstances.get(idleDeviceId).reqStartTime - qServerInstances.get(idleDeviceId).reqArrivalTime);
		}
	}

	public void dequeueAndProcess(int instanceId, double currTime) throws Exception {
		// get next last request(because it is lcfs) from buffer
		int curBufSize = queueBuffer.size();
		Request req = getRequestFromBuffer(curBufSize - 1, currTime);

		int idleDeviceId = getIdleInstanceId();
		processRequestArrival(req, idleDeviceId, currTime);
		
		req.qServerInstanceID = idleDeviceId;

		// send the request for service.
		createStartTaskEvent(req, idleDeviceId, currTime);

		// update the total waiting time for this resource
		// nadeesh commented because now totalWaitingTime is not used to find the averageWaitingTime
		//	totalWaitingTime.recordValue(req,qServerInstances.get(instanceId).reqStartTime - qServerInstances.get(instanceId).reqArrivalTime);
		// update the average waiting time for this resource
		averageWaitingTimeSim.recordValue(req,qServerInstances.get(idleDeviceId).reqStartTime - qServerInstances.get(idleDeviceId).reqArrivalTime);
	}
}
