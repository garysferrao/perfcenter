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
package perfcenter.analytical;

import java.util.ArrayList;

import perfcenter.baseclass.Device;
import perfcenter.baseclass.DistributedSystem;
import perfcenter.baseclass.Host;
import perfcenter.baseclass.ModelParameters;
import perfcenter.baseclass.SoftServer;
import perfcenter.baseclass.Task;

//! Contains all the distributed system parameters as given in input file.
/*! Inherited class from DistributedSytem. It contains all the model parameters
 *  as contained in the DistributedSystem data structure. Essentially, its just a 
 *  replication of DistributedSystem data structure.
 */
public class DistributedSystemAna extends DistributedSystem {

	ArrayList<SoftServerAna> softServerAna;

	public DistributedSystemAna(DistributedSystem ds) {
		virRes = ds.virRes;
		devices = ds.devices;
		variables = ds.variables;
		tasks = ds.tasks;
		softServers = ds.softServers;
		scenarios = ds.scenarios;
		lans = ds.lans;
		links = ds.links;
		this.overallResponseTime.setValue((double) 0);
		this.overallThroughput.setValue((double) 0);
		this.overallArrivalRate.setValue((double) 0);
		softServerAna = new ArrayList<SoftServerAna>();
		for (Host h : ds.hosts) {
			for (Object s : h.softServers) {
				// Below is a temporary hack. When you deploy one software server on two machines, the software server
				// object in ModelParams.ds and the software server object inside the host are different. Don't know why
				// this is happening. To fix this, I have taken the object from the ModelParams.ds rather than from host object. --Mayur
				SoftServer s1 = (SoftServer) s;
				SoftServerAna sa = new SoftServerAna(ModelParameters.inputDistributedSystem.getServer(s1.getName()), h.name);
				softServerAna.add(sa);
			}

			for (Object d : h.devices) {
				((Device) d).initialize();
			}
			// h.softServers = softServerAna;
			h.arrivalRate = 0;
			hosts.add(h);
		}
		// this need not be done if its use in perfanalytic is fixed
		for (Object s : softServers) {
			for (Task t : ((SoftServer) s).simpleTasks) {
				t.initialize();
			}
		}
	}

	public SoftServerAna getSoftServerAna(String name, String hName) {
		for (SoftServerAna serv : softServerAna) {
			if (serv.name.compareToIgnoreCase(name) == 0 && serv.getHostName().compareTo(hName) == 0) {
				return serv;
			}
		}
		throw new Error(name + " is not Server");
	}

	public SoftServerAna getSoftServerAna(String name) {
		for (Object serv : softServerAna) {
			if (((SoftServerAna) serv).name.compareToIgnoreCase(name) == 0) {
				return ((SoftServerAna) serv);
			}
		}
		throw new Error(name + " is not Server");
	}
}
