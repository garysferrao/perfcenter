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

//this is not being used now.
public class VirResVector {
	public int instanceNo_;

	public int virtResIndex_;

	public String virtResName_;

	private HostSim hostObject;

	public VirResVector(int instanceNo, int virtResIndex, String virtResName, HostSim ho) {
		instanceNo_ = instanceNo;
		virtResIndex_ = virtResIndex;
		virtResName_ = virtResName;
		hostObject = ho;
	}

	public HostSim getHostObject() {
		return hostObject;
	}

	public void setHostObject(HostSim hostObject) {
		this.hostObject = hostObject;
	}
}
