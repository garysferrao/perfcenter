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

import perfcenter.simulator.HostSim;

/** Synchronous request between two servers */
public class SyncRequest {

	int reqID;

	String softServerName;

	String taskName;

	private HostSim hostObject;

	int threadNum;

	double swServerArrivalTime;

	double swServerStartTime;

	public SyncRequest(HostSim h, String swID, String tskID, int thrdNum, int rqID, double servArrTime, double servStartTime) {
		softServerName = swID;
		taskName = tskID;
		threadNum = thrdNum;
		reqID = rqID;
		swServerArrivalTime = servArrTime;
		swServerStartTime = servStartTime;
		setHostObject(h);
	}

	HostSim getHostObject() {
		return hostObject;
	}

	void setHostObject(HostSim hostObject) {
		this.hostObject = hostObject;
	}

}