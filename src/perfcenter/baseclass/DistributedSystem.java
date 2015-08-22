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
/*
 * This is the main class that has complete representation of input file
 * It has functions to print,validate,modify  the resources defined.
 */
package perfcenter.baseclass;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.FileAppender;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;

/**
 * This is the complete representation of the input file. Parser fills in this structure.
 * 
 * @author akhila
 */
public class DistributedSystem {

	// List of soft resources. used for reference only. they are deployed on
	// hosts
	public ArrayList<VirtualResource> virRes; // list of virtual resources
	public List<String> devices;
	public ArrayList<Variable> variables;
	public ArrayList<Task> tasks;
	/** List of softServers. used for reference only. They are deployed on Hosts */
	public ArrayList<SoftServer> softServers;
	/** All the Hosts. For simulation, this would contain objects of HostSim. */
	public ArrayList<Host> hosts;
	/** All the Scenarios. For simulation, this would contain objects of ScenarioSim. */
	public ArrayList<Scenario> scenarios;
	public ArrayList<Lan> lans;
	/** All the LanLinks. For simulation, this would contain objects of LanLinkSim. */ 
	public ArrayList<LanLink> links;
	
	// parameters across all scenarios
	public Metric overallRespTime = new Metric();
	public Metric overallThroughput = new Metric();
	public Metric overallBadput = new Metric();
	public Metric overallGoodput = new Metric();
	public Metric overallBuffTimeout = new Metric();
	public Metric overallDroprate = new Metric();
	public Metric overallArrivalRate = new Metric();
	public Metric overallBlockProb = new Metric();

	// this list contains different device types and used to copy attributes
	// from this device to other devices of same type: rakesh
	public List<Device> powerManagedDevicePrototypes;
	// list of power managed devices
	public List<Device> powerManagedDevices;

	public DistributedSystem() {
		hosts = new ArrayList<Host>();
		virRes = new ArrayList<VirtualResource>();
		devices = new ArrayList<String>();

		/***************************************************************************/
		powerManagedDevicePrototypes = new ArrayList<Device>(); // added by Rakesh
		powerManagedDevices = new ArrayList<Device>(); // added by Rakesh
		/***************************************************************************/
		variables = new ArrayList<Variable>();
		tasks = new ArrayList<Task>();
		softServers = new ArrayList<SoftServer>();
		scenarios = new ArrayList<Scenario>();
		lans = new ArrayList<Lan>();
		links = new ArrayList<LanLink>();

		// This is done to have a dummy task "user" at the start and end of a scenario
		Task t = new Task("user", 0);

		SoftServer sdummy = new SoftServer("user");
		sdummy.addTask(t);
//		sdummy.addHost("dummy");
		softServers.add(sdummy);
		
		t.softServerName = "user";
		tasks.add(t);
		
		// why these things are here? they are local variables which are being used nowhere else
//		Host hdummy = new Host("dummy", 1);
//		hdummy.addServer(sdummy);
		// End dummy task
	}

	public void printConfiguration() {
		System.out.println("---printcfg start----");
		for (Host host : hosts) {
			System.out.println("---HostDef----");
			host.print();
		}
		System.out.println("---printcfg end----");
	}

	public Task getTask(String name) {
		for (Task task : tasks) {
			if (task.name.compareToIgnoreCase(name) == 0) {
				return task;
			}
		}
		throw new Error(name + " is not Task");
	}

	public LanLink getLink(String name1, String name2) {
		for (LanLink link : links) {
			if (link.srclan.compareToIgnoreCase(name1) == 0) {
				if (link.destlan.compareToIgnoreCase(name2) == 0) {
					return link;
				}
			}
			if (link.srclan.compareToIgnoreCase(name2) == 0) {
				if (link.destlan.compareToIgnoreCase(name1) == 0) {
					return link;
				}
			}
		}
		throw new Error(name1 + " " + name2 + "is not Link");
	}

	public LanLink getLink(String name1) {
		for (LanLink link : links) {
			if (link.name.compareToIgnoreCase(name1) == 0) {
				return link;
			}
		}
		throw new Error(name1 + "is not defined as Link");
	}

	public boolean isLink(String name1) {
		for (LanLink link : links) {
			if (link.name.compareToIgnoreCase(name1) == 0) {
				return true;
			}
		}
		return false;
	}

	public boolean isLink(String name1, String name2) {
		for (LanLink link : links) {
			if (link.srclan.compareToIgnoreCase(name1) == 0) {
				if (link.destlan.compareToIgnoreCase(name2) == 0) {
					return true;
				}
			}
			if (link.srclan.compareToIgnoreCase(name2) == 0) {
				if (link.destlan.compareToIgnoreCase(name1) == 0) {
					return true;
				}
			}
		}
		return false;
	}

	public boolean isTask(String name) {
		for (Task task : tasks) {
			if (task.name.compareToIgnoreCase(name) == 0) {
				return true;
			}
		}
		return false;
	}

	public SoftServer getServer(String name) {
		for (SoftServer serv : softServers) {
			if (serv.name.compareToIgnoreCase(name) == 0) {
				return serv;
			}
		}
		throw new Error(name + " is not Server");
	}

	public boolean isServer(String name) {
		for (SoftServer serv : softServers) {
			if (serv.name.compareToIgnoreCase(name) == 0) {
				return true;
			}
		}
		return false;
	}

	public Host getHost(String name) {
		for (Object host : hosts) {
			if (((Host) host).name.compareToIgnoreCase(name) == 0) {
				return (Host) host;
			}
		}
		throw new Error(name + " is not Host");
	}

	public boolean isHost(String name) {
		for (Object host : hosts) {
			if (((Host) host).name.compareToIgnoreCase(name) == 0) {
				return true;
			}
		}
		return false;
	}

	public String getDevice(String name) {
		for (String dev : devices) {
			if (dev.compareToIgnoreCase(name) == 0) {
				return dev;
			}
		}
		throw new Error(name + " is not Device");
	}

	public boolean isDevice(String name) {
		for (String dev : devices) {
			if (dev.compareToIgnoreCase(name) == 0) {
				return true;
			}
		}
		return false;
	}

	public VirtualResource getVirtualRes(String name) {
		for (VirtualResource sr : virRes) {
			if (sr.name.compareToIgnoreCase(name) == 0) {
				return sr;
			}
		}
		throw new Error(name + " is not Virtual res");
	}

	public boolean isVirtualRes(String name) {
		for (VirtualResource sr : virRes) {
			if (sr.name.compareToIgnoreCase(name) == 0) {
				return true;
			}
		}
		return false;
	}

	public Scenario getScenario(String name) {
		for (Scenario sce : scenarios) {
			if (sce.name.compareToIgnoreCase(name) == 0) {
				return sce;
			}
		}
		throw new Error(name + " is not Scenario");
	}

	public boolean isScenario(String name) {
		for (Scenario sce : scenarios) {
			if (sce.name.compareToIgnoreCase(name) == 0) {
				return true;
			}
		}
		return false;
	}

	public Variable getVariable(String name) {
		for (Variable var : variables) {
			if (var.name.compareToIgnoreCase(name) == 0) {
				return var;
			}
		}
		throw new Error(name + " is not Variable");
	}

	public boolean isVariable(String name) {
		for (Variable var : variables) {
			if (var.name.compareToIgnoreCase(name) == 0) {
				return true;
			}
		}
		return false;
	}

	public Lan getLan(String name) {
		for (Lan lan : lans) {
			if (lan.name.compareToIgnoreCase(name) == 0) {
				return lan;
			}
		}
		throw new Error(name + " is not Lan");
	}

	public boolean isLan(String name) {
		for (Lan lan : lans) {
			if (lan.name.compareToIgnoreCase(name) == 0) {
				return true;
			}
		}
		return false;
	}
	
	//CHECK : This method is not used anywhere
	// deploying server on host or host on lan
	public void deploy(String name1, String name2) {
		SoftServer srv;
		Host host;
		// deploying server on host
		if (isServer(name1)) {
			srv = getServer(name1);
			host = getHost(name2);
			srv.addHost(name2);
			host.addServer(srv);
			return;
		} else {
			// deploying host on lan
			host = getHost(name1);
			Lan ln = getLan(name2);
			host.addLan(name2);
			ln.addHost(name1);
		}
	}

	public void validate() {
		for (Task task : tasks) {
			task.validate();
		}
		for (SoftServer serv : softServers) {
			serv.validate();
		}
		for (Host host : hosts) {
			host.validate();
		}
		for (Lan lan : lans) {
			lan.validate();
		}
		for (Variable var : variables) {
			var.validate();
		}
	}

	// check that sum of all scenario prob is one
	public void checkParameters() throws IOException {
		double total_prob = 0;
		for (Scenario sc : ModelParameters.inputDistSys.scenarios) {
			total_prob += sc.getProbability();
		}
		if (total_prob != 1) {
			throw new Error("Sum of scenario probability is not equal to 1");
		}

		if (ModelParameters.outputFileStr.length() > 0) {
			FileAppender fa = new FileAppender(new PatternLayout(), ModelParameters.outputFileStr, false);
			Logger l = Logger.getLogger("DistributedSystem");
			l.addAppender(fa);
		}
	}

	// scenario arrival rate is based on scenario probability
	public void setScenarioArrivalRate() {
		for (int slot = 0; slot < ModelParameters.intervalSlotCount; slot++) {
			for (Scenario sc : ModelParameters.inputDistSys.scenarios) {
				sc.setArateToScenario(slot, ModelParameters.getArrivalRate(slot).getValue() * sc.getProbability());
			}
		}
	}

	public ArrayList<Host> getHostList() {
		return (hosts);
	}

	public ArrayList<SoftServer> getSoftserverList() {
		return (softServers);
	}

	/********************************************************************/
	// Add power-managed device into the list
	public void addPowerManagedDevices(Device device) {
		powerManagedDevices.add(device);
	}

	/********************************************************************/
}
