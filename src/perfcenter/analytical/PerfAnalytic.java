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

import perfcenter.baseclass.DeviceServiceTime;
import perfcenter.baseclass.DistributedSystem;
import perfcenter.baseclass.Host;
import perfcenter.baseclass.ModelParameters;
import perfcenter.baseclass.Node;
import perfcenter.baseclass.Scenario;
import perfcenter.baseclass.SoftServer;
import perfcenter.baseclass.Task;
import perfcenter.baseclass.Variable;
import perfcenter.baseclass.VirtualResource;
import perfcenter.baseclass.enums.SysType;

//! To be filled in after understanding
/*! 
 */
public class PerfAnalytic {

	DistributedSystemAna resultDistributedSystemAna;
	DistributedSystem inputDistributedSystem;

	public PerfAnalytic(DistributedSystem perfc) throws Exception {
		inputDistributedSystem = perfc;
		generateSoftServers();
		// modifyScenario();
		resultDistributedSystemAna = new DistributedSystemAna(perfc);
	}

	public DistributedSystemAna performAnalysis() throws Exception {

		if (ModelParameters.getSystemType() == SysType.OPEN) {
			PerfAnaClosedOpen pco;
			pco = new PerfAnaClosedOpen(resultDistributedSystemAna);
			// resultDistributedSystem = pco.performAnalysisOpen();
			PerfAnaOpen po;
			po = new PerfAnaOpen(resultDistributedSystemAna);
			resultDistributedSystemAna = po.performAnalysisOpen();
		} else if (ModelParameters.getSystemType() == SysType.CLOSED) {
			PerfAnaClosed pc;
			pc = new PerfAnaClosed(resultDistributedSystemAna);
			resultDistributedSystemAna = pc.performAnalysisClosed();
		} else {
			PerfAnaClosed pco;
			pco = new PerfAnaClosed(resultDistributedSystemAna);
			resultDistributedSystemAna = pco.performAnalysisClosed();
		}
		return resultDistributedSystemAna;
	}

	/**
	 * This function generates software server named with virresource_server on all Hosts(virtHosts) and count indicates count of virtresource or
	 * default one task is the task present on that server, deploy software servers on hosts
	 * 
	 */
	void generateSoftServers() {
		SoftServer s;
		SoftServerAna sa;
		Task t;
		boolean conf_modified = false;

		for (Host host : inputDistributedSystem.hosts) {

			for (VirtualResource virres : host.virResources) {
				if (!inputDistributedSystem.isServer(virres.name + "_server")) {
					conf_modified = true;
					s = new SoftServer(virres.name + "_server");
					s.thrdCount.setValue(virres.count.getValue());

					if (virres.virtRes.size() == 0) {/* add only one task */
						t = new Task(virres.name + "_task", 0);
						for (DeviceServiceTime dst : virres.deviceSTimes) {
							t.addDeviceServiceTime(dst.getCopy());
						}
						t.addServer(s.name);
						inputDistributedSystem.tasks.add(t);
						s.addTask(t);

					} else {/* add two tasks */
						t = new Task(virres.name + "_task", 0);
						for (DeviceServiceTime dst : virres.deviceSTimes) {
							DeviceServiceTime dst1 = dst.getCopy();
							dst1.dist.setServiceTime(dst1.dist.getServiceTime() / 2);
							t.addDeviceServiceTime(dst1);
						}
						t.addServer(s.name);
						inputDistributedSystem.tasks.add(t);
						s.addTask(t);
						t = new Task(virres.name + "_task1", 0);
						for (DeviceServiceTime dst : virres.deviceSTimes) {
							DeviceServiceTime dst1 = dst.getCopy();
							dst1.dist.setServiceTime(dst1.dist.getServiceTime() / 2);
							t.addDeviceServiceTime(dst1);
						}
						t.addServer(s.name);
						inputDistributedSystem.tasks.add(t);
						s.addTask(t);

					}

					s.addHost(host.name);
					inputDistributedSystem.softServers.add(s);
					// sa= new SoftServerAna((SoftServer) a);
					// host.addServer((SoftServer)sa);
					host.softServers.add(s);
				} else if (!host.isServerDeployed(virres.name + "_server")) {
					conf_modified = true;
					s = inputDistributedSystem.getServer(virres.name + "_server");
					s.addHost(host.name);
					// sa= new SoftServerAna((SoftServer) s);
					host.addServer((SoftServer) s);
				}

			}
		}
		if (conf_modified) {
			modifyScenario();
		}
	}

	void modifyScenario() {
		Task t;
		ArrayList<Task> simpletasks = new ArrayList<Task>();
		for (Task simpletask : inputDistributedSystem.tasks) {
			simpletasks.add(simpletask);

		}
		for (Task simpletask : simpletasks) {

			if (simpletask.virRes.size() != 0) {/* create two tasks */

				for (DeviceServiceTime dst : simpletask.deviceServiceTimes) {
					dst.dist.setServiceTime(dst.dist.getServiceTime() / 2);
				}
				t = new Task(simpletask.name + "_replication", 6);
				for (DeviceServiceTime dst : simpletask.deviceServiceTimes) {
					DeviceServiceTime dst1 = dst.getCopy();
					t.addDeviceServiceTime(dst1);
				}
				t.addServer(simpletask.getServerName());
				inputDistributedSystem.tasks.add(t);

				inputDistributedSystem.getServer(simpletask.getServerName()).addTask(t);
				for (Host h : inputDistributedSystem.hosts) {
					for (SoftServer s1 : h.softServers) {
						if (s1.name.equalsIgnoreCase(simpletask.getServerName())) {
							s1.addTask(t);
						}
						for (Task t2 : s1.simpleTasks) {
							if (t2.name.equalsIgnoreCase(simpletask.name)) {
								for (DeviceServiceTime dst : t2.deviceServiceTimes) {
									dst.dist.setServiceTime(dst.dist.getServiceTime() / 2);
								}
							}

						}

					}
				}

				for (Scenario sce : inputDistributedSystem.scenarios) {
					Node n = dfs(sce.rootNode, simpletask.name);

					if (n != null) {
						Node n1 = n.getCopy();// change n1.name to
						n1.name = simpletask.name + "_replication";
						// System.out.println(""+n.name);
						constructScenarioTree(n, n1, simpletask.virRes.get(0));

					}

				}
				for (Scenario sce : inputDistributedSystem.scenarios) {
					this.dfsprint(sce.rootNode);
				}

			}
			/* modify scenario diagram accordingly */

		}
	}

	void dfsprint(Node ns) {
		System.out.println("node :" + ns.name);
		for (Node child : ns.children) {
			dfsprint(child);
		}
		return;
	}

	void constructScenarioTree(Node start, Node end, String virres) {
		if (inputDistributedSystem.getVirtualRes(virres).virtRes.size() == 0) {
			VirtualResource virtres = inputDistributedSystem.getVirtualRes(virres);
			Variable size = new Variable("size", 0);// to do check variable name
			Node n = new Node(virtres.name + "_task", virtres.name + "_server", size, true);
			n.prob = start.prob;
			n.parent = start;
			n.children.add(end);
			end.parent = n;
			for (Node child : start.children) {
				child.parent = end;
			}
			start.children = new ArrayList<Node>();

			start.children.add(n);
		} else {
		}
	}

	Node dfs(Node n, String taskname) {
		if (n.name.equals(taskname)) {
			return n;
		}
		for (Node child : n.children) {
			if (child.name.equals(taskname)) {
				return child;
			}
			return dfs(child, taskname);

		}
		return null;
	}
}
