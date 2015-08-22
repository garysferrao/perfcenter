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

import org.apache.log4j.Logger;
import org.jgrapht.alg.EdmondsKarpMaximumFlow;
import org.jgrapht.graph.DefaultWeightedEdge;
import org.jgrapht.graph.SimpleDirectedWeightedGraph;

import perfcenter.analytical.CompoundTask.PairValue;
import perfcenter.analytical.CompoundTask.SubTask;
import perfcenter.baseclass.Device;
import perfcenter.baseclass.DeviceServiceTime;
import perfcenter.baseclass.Host;
import perfcenter.baseclass.Lan;
import perfcenter.baseclass.LanLink;
import perfcenter.baseclass.ModelParameters;
import perfcenter.baseclass.Node;
import perfcenter.baseclass.Scenario;
import perfcenter.baseclass.SoftServer;
import perfcenter.baseclass.Task;
import perfcenter.baseclass.Variable;

//! Contains the functions for the Performance analysis of a closed queuing network.
/*! This, along with PerfAnaOpen, PerfAnaClosedOpen are the most important 
 *  classes of the Analytical part of the tool. They contain all the equations
 *  explained in the paper. Some of the important things done here are the
 *  calculation of Arrival rates, think times and number of users. These are the
 *  initialization parameters needed for solving the model.
 */
public class PerfAnaClosed {

	DistributedSystemAna resultDistributedSystemAna;
	Logger logger = Logger.getLogger("PerfAnaClosed");
	boolean overload = false;
	boolean network = true;// keep it false if you are not performing Network level calculations

	ArrayList<HostDevice> hostDeviceList = new ArrayList<HostDevice>();

	public PerfAnaClosed(DistributedSystemAna ds1) {
		resultDistributedSystemAna = ds1;
		SoftServerAna.ds = ds1;// added by niranjan
	}

	public DistributedSystemAna performAnalysisClosed() throws Exception {
		double squaredResTime = 0.0;
		double responseTime = 0.0;
		double previousResponseTime = 0.0;
		double error = 0.00001;
		int i = 0;
		
		//initialize the structures that would help in solving (like list of servers for a compound task etc.
		recalculateNodeProbabilityInScenarioTree();
		
		createCompoundTaskObjects(); //RIGHTNOW: here is the problem
		initializePairTasks(); //RIGHTNOW: later: understand what the pairtasks are for. They are not required for webcal scenarios.
		initializeConditionalProb();
		initializeCallingServers();
		
		//start solving for initial values from here
		calculateInitialTaskHoldingValues();
		calculateScenarioMetrics();
		initializeNoOfUsersThinkTime();
		initializeNoOfInvocations(); //RIGHTNOW: later: not sure where this is used
		initializeLinkQueues();
		
		//XXX debug
		System.err.println();
		System.err.println("_______________________________");
		for (SoftServerAna sa : resultDistributedSystemAna.softServerAna) {
			System.err.println(">>>> compound tasks of SoftServerAna " + sa.getName() + ": prob: numinvok: ");
			for(CompoundTask ct : sa.compundTasks) {
				System.err.println(ct.getName() + ": " + ct.getProbability() + ": " + ct.numInvocations);
			}
			System.err.println("###################");
		}
		System.err.println("_______________________________");
		
		for (Host host : resultDistributedSystemAna.hosts) {
			calculateDeviceHoldingTime(host);
		}

		// Calculating number of users at each server and device
		calculateNumberOfUsers();

		for (i = 0; i < 2000; i++) {
			solveIteratively();
			previousResponseTime = responseTime;
			squaredResTime = 0.0;
			calculateScenarioMetrics();
			for (Scenario sc : resultDistributedSystemAna.scenarios)
				squaredResTime += sc.getAverageResponseTime() * sc.getAverageResponseTime();
			responseTime = Math.sqrt(squaredResTime);
			if (previousResponseTime != 0.0) {
				if (Math.abs(previousResponseTime - responseTime) <= error)
					break;
			}
		}
		return resultDistributedSystemAna;
	}

	void recalculateNodeProbabilityInScenarioTree() {
		for (Scenario sce : resultDistributedSystemAna.scenarios) {
			sce.initialize();
		}
	}

	/** compound task identification and creation from scenario graphs */
	void createCompoundTaskObjects() {
		for (Host h : resultDistributedSystemAna.hosts) {
			for (SoftServer s : h.softServers) {
				for (Scenario sce : resultDistributedSystemAna.scenarios) {
					SoftServerAna softServerAna = resultDistributedSystemAna.getSoftServerAna(s.getName(), h.getName());
					softServerAna.createCompoundTaskObjectsForGivenSoftServer(sce.rootNode, sce.getName());
				}
			}
		}

		// for (Scenario sce : ds.scenarios) {
		// generateJobClass(sce.getName());
		// }
	}

	void generateJobClass(String scename) {
		ArrayList<CompoundTask> ctasks1 = new ArrayList<CompoundTask>();

		/*
		 * This block is used to get CTs whose users are end-users, e.g: send_to_auth-1 and send_to_auth-2 for login scenario
		 */
		for (Host host : resultDistributedSystemAna.hosts) {
			for (SoftServer softserv : host.softServers) {
				SoftServerAna sa = resultDistributedSystemAna.getSoftServerAna(softserv.getName(), host.getName());
				for (CompoundTask ct : sa.compundTasks) {
					if (ct.getParentServer().equalsIgnoreCase("user") && ct.sceName.equalsIgnoreCase(scename)) {
						ctasks1.add(ct);
					}
				}
			}
		}

		ArrayList<CompoundTask> ctasksToBeAdded = new ArrayList<CompoundTask>();
		for (Host host : resultDistributedSystemAna.hosts) {
			for (SoftServer softserv : host.softServers) {
				SoftServerAna sa = resultDistributedSystemAna.getSoftServerAna(softserv.getName(), host.getName());
				for (CompoundTask ct : sa.compundTasks) {
					if (!ct.getParentServer().equalsIgnoreCase("user") && (ct.sceName.equalsIgnoreCase(scename))) // tbd: if this is sufficient to
																													// check if this case is
																													// sufficient
					{
						if (TaskPresenceInCTs(ct.subTasks.get(0).name, scename) > checkPresenceOfCTs(ct.subTasks.get(0).name, scename)) {
							CompoundTask ct1 = ct.deepCopy();
							ct1.name = ct.subTasks.get(0).name + "_" + (checkPresenceOfCTs(ct.subTasks.get(0).name, scename) + 1);
							ctasksToBeAdded.add(ct1);
						}
					}
				}
				sa.compundTasks.addAll(ctasksToBeAdded);
				ctasksToBeAdded.clear();
			}
		}

		for (Host host : resultDistributedSystemAna.hosts) {
			for (SoftServer softserv : host.softServers) {
				SoftServerAna sa = resultDistributedSystemAna.getSoftServerAna(softserv.getName(), host.getName());
				for (CompoundTask ct : sa.compundTasks) {
					if (!ct.getParentServer().equalsIgnoreCase("user") && (ct.sceName.equalsIgnoreCase(scename))) // tbd: if this is sufficient to
																													// check if this case is
																													// sufficient
					{
						while (TaskPresenceInCTs(ct.subTasks.get(0).name, scename) > checkPresenceOfCTs(ct.subTasks.get(0).name, scename))
							;
						{
							CompoundTask ct1 = ct.deepCopy();
						}
					}
				}
			}
		}

		for (CompoundTask rootct : ctasks1) {
			createClassName(rootct.ClassName, rootct);
		}
	}

	void createClassName(String jobclassname, CompoundTask parenttask) {
		CompoundTask childCT;
		for (SubTask task : parenttask.subTasks) {
			if (task.name.equalsIgnoreCase(parenttask.subTasks.get(0).name)) {
				continue;
			}
			childCT = isCT(task.name, jobclassname);
			if (childCT != null) {
				childCT.parentCTTask = parenttask.name;
				childCT.jobClassSet = true;
				childCT.ClassName = jobclassname;
				childCT.probability = parenttask.probability;
				childCT.parentServer = parenttask.servername;
				createClassName(jobclassname, childCT);
			}
		}

	}

	CompoundTask isCT(String taskname, String jobclassname) {
		for (Host host : resultDistributedSystemAna.hosts) {
			for (SoftServer softserv : host.softServers) {
				SoftServerAna sa = resultDistributedSystemAna.getSoftServerAna(softserv.getName(), host.getName());
				for (CompoundTask ct : sa.compundTasks) {

					if (ct.subTasks.get(0).name.equals(taskname) && ct.ClassName.equalsIgnoreCase(jobclassname) && ct.jobClassSet) {
						return null;
					}
				}
				for (CompoundTask ct : sa.compundTasks) {
					if (ct.subTasks.get(0).name.equals(taskname) && !ct.jobClassSet) {
						return ct;
					}

				}
			}
		}
		return null;

	}

	int TaskPresenceInCTs(String ctname, String scename) {
		int count = 0;
		for (Host host : resultDistributedSystemAna.hosts) {
			for (SoftServer softserv : host.softServers) {
				SoftServerAna sa = resultDistributedSystemAna.getSoftServerAna(softserv.getName(), host.getName());
				for (CompoundTask ct : sa.compundTasks) {
					if (ct.getParentServer().equalsIgnoreCase("user") && ct.sceName.equalsIgnoreCase(scename)) // tbd: if this is sufficient to check
																												// if this case is sufficient
					{
						for (CompoundTask.SubTask t : ct.subTasks) {
							if (t.name.compareToIgnoreCase(ctname) == 0) {
								count++;
							}
						}

					}
				}
			}
		}
		return count;
	}

	int checkPresenceOfCTs(String ctname, String scename) {
		int count = 0;
		for (Host host : resultDistributedSystemAna.hosts) {
			for (SoftServer softserv : host.softServers) {
				SoftServerAna sa = resultDistributedSystemAna.getSoftServerAna(softserv.getName(), host.getName());
				for (CompoundTask ct : sa.compundTasks) {
					if (ct.subTasks.get(0).name.equalsIgnoreCase(ctname) && ct.sceName.equalsIgnoreCase(scename)) // tbd: if this is sufficient to check
																												// if this case is sufficient
					{
						count++;
					}
				}
			}
		}
		return count;
	}

	void initializeNoOfUsersThinkTime() throws Exception {
		for (Host h : resultDistributedSystemAna.hosts) {
			for (SoftServer softserv : h.softServers) {
				for (String deploymentHostForSoftServer : softserv.hosts) {
					SoftServerAna sa = resultDistributedSystemAna.getSoftServerAna(softserv.getName(), deploymentHostForSoftServer);
					for (SoftServerAna.CallingServer callingServer : sa.callingServers) {
						if (callingServer.getServername().equals("user")) {
							callingServer.setUsers((int) ModelParameters.getNumberOfUsers());
							callingServer.setThinkTime(getUserServerThinkTime(sa.getName()));
						} else {
							callingServer.setUsers((int) resultDistributedSystemAna
											.getSoftServerAna(callingServer.getServername(), callingServer.getHostName())
											.thrdCount.getValue());
						}
					}
				}
			}
		}
	}

	double getUserServerThinkTime(String server) throws Exception {
		//eqn (25)
		double directCallProb = 0.0;
		double otherScenarioResponseTime = 0;
		Node currentNode;
		for (Scenario scenario : resultDistributedSystemAna.scenarios) {
			if (scenario.rootNode.name.compareToIgnoreCase("user") == 0)
				currentNode = scenario.rootNode.children.get(0);
			else
				currentNode = scenario.rootNode;

			if (currentNode.servername.compareToIgnoreCase(server) == 0)
				directCallProb += scenario.getProbability();
			else
				otherScenarioResponseTime += scenario.getAverageResponseTime();
		}

		SoftServer softServer = resultDistributedSystemAna.getServer(server);
		directCallProb = directCallProb / softServer.hosts.size();

		if (directCallProb == 0)
			throw new Exception("User does not call this server", null);
		else {
			return (1 / directCallProb - 1) * (ModelParameters.getThinkTime().getServiceTime() + otherScenarioResponseTime)
					+ ModelParameters.getThinkTime().getServiceTime();
		}
	}

	void initializeCallingServers() {
		for (Host host : resultDistributedSystemAna.hosts) { //for all hosts
			for (SoftServer softserv : host.softServers) { //for all softservers of that host
				boolean userFlag = true;
				SoftServerAna softServerAna = resultDistributedSystemAna.getSoftServerAna(softserv.getName(), host.getName());
				for (CompoundTask compundTask : softServerAna.compundTasks) { //for all compound task of given soft server
					if (compundTask.getParentServer().compareTo("user") != 0) { //if compund task's parent server is not "user"
						SoftServerAna parentSoftServerAna = resultDistributedSystemAna.getSoftServerAna(compundTask.getParentServer());
						boolean addCallingServer = true;
						
						//check if the CallingServer object is already there
						for (SoftServerAna.CallingServer callingServer : softServerAna.callingServers) {
							if (callingServer.getServername().compareTo(parentSoftServerAna.getName()) == 0) {
								addCallingServer = false;
								break;
							}
						}
						
						//add calling server only if already an object is not found
						if (addCallingServer) {
							
							//add a calling server object for each host on which current compound task's parentSoftServer is deployed
							for (String parentHost : parentSoftServerAna.hosts) {
								softServerAna.callingServers.add(
										softServerAna.new CallingServer(parentSoftServerAna.getName(), parentHost));
							}
						}
					} else {
						//for all compound tasks of current softserver, add the calling server of user only once
						if (userFlag) {
							softServerAna.callingServers.add(softServerAna.new CallingServer("user", "user"));
							userFlag = false;
						}
					}
				}
			}
		}
	}

	void initializeLinkQueues() {
		LanLink link;
		for (Lan lan1 : resultDistributedSystemAna.lans) {
			for (Lan lan2 : resultDistributedSystemAna.lans) {
				if (!lan1.getName().equalsIgnoreCase(lan2.getName())) {
					link = resultDistributedSystemAna.getLink(lan1.getName(), lan2.getName());
					link.initialize();
				}
			}
		}
		return;
	}

	/**
	 * Sets the number of invocations made by the calling server on the called server 
	 * within the same compound task. User calling a software server is not considered 
	 * an invocation here.<br>
	 * <p>
	 * Used for think time calculation.
	 */

	public void initializeNoOfInvocations() {
		for (Host host : resultDistributedSystemAna.hosts) {
			for (SoftServer softserv : host.softServers) {
				SoftServerAna softServerAna = resultDistributedSystemAna.getSoftServerAna(softserv.getName(), host.getName());
				for (CompoundTask compoundTask : softServerAna.compundTasks) {
					for (SubTask subTask : compoundTask.subTasks) {
						if (!subTask.servername.equals(softServerAna.getName()) && !subTask.servername.equals("user")) {
							int invocations = 0;
							for (SubTask eachSubTask : compoundTask.subTasks) {
								if (subTask.servername.equals(eachSubTask.servername)) {
									invocations++;
								}
								SoftServer softServer = resultDistributedSystemAna.getServer(subTask.servername);
								for (String hostname : softServer.hosts) {
									Host h = resultDistributedSystemAna.getHost(hostname);
									resultDistributedSystemAna.getSoftServerAna(subTask.servername, h.getName())
										.getCTask(subTask.name, compoundTask.getScenarioName())
										.numInvocations = invocations;
								}
							}
						}
					}
				}
			}
		}
	}

	/*
	 * This function can be used to solve the queues in appropriate order
	 */
	public void solveIteratively() throws Exception {
		for (Host host : resultDistributedSystemAna.hosts) {
			for (SoftServer softserv : host.softServers) {
				SoftServerAna sa = resultDistributedSystemAna.getSoftServerAna(softserv.getName(), host.getName());
				aggregateParameters(sa, host.getName());
				sa.solveAggrMVA();

				double modified_utilization = sa.getUtilization() * sa.thrdCount.getValue() / (resultDistributedSystemAna.getServer(sa.getName()).thrdCount.getValue());
				sa.setUtilization(modified_utilization);
				sa.setAvgWaitingTime(sa.getAvgResponseTime() - sa.getHoldingTime());
				softserv.setUtilization(modified_utilization);
				softserv.setThroughput(sa.getThroughput());
				softserv.setAverageResponseTime(sa.getAvgResponseTime());
				softserv.setAvgWaitingTime(sa.getAvgResponseTime() - sa.getHoldingTime());
				calculateTaskHoldingValues(sa, resultDistributedSystemAna.getHost(host.getName()));
			}

			for (Device d : host.devices) {
				d.thinkTime = 0;
			}

			calculateDeviceThinkTimes(host);
			if (host.softServers.size() > 0) {
				for (Device d : host.devices) {
					String stationNames[] = new String[2];
					stationNames[0] = d.name;
					stationNames[1] = "infinite-server";
					int stationTypes[] = new int[2];
					stationTypes[0] = 1;
					stationTypes[1] = 2;
					double[] visits = new double[2];
					double[][] serviceTimes = new double[2][1];

					serviceTimes[0][0] = d.holdingTime;
					serviceTimes[1][0] = d.thinkTime;

					visits[0] = 1;
					visits[1] = 1;

					Solver solver;
					logger.debug("device on machine: " + host.getName());
					logger.debug("input: service time: " + serviceTimes[0][0] + " Think time: " + serviceTimes[1][0] + " no of users: "
							+ d.numberOfUsers + " no of servers " + d.count.getValue());
					SolverSingleClosedMVA closedsolver = new SolverSingleClosedMVA((int) Math.round(d.numberOfUsers), 2);// d. no of users
					if (!closedsolver.input(stationNames, stationTypes, serviceTimes, visits, (int) d.count.getValue())) {
						throw new Exception("Error initializing MVASingleSolver", null);
					}
					solver = closedsolver;
					if (d.numberOfUsers != 0) {
						solver.solve();
						d.setThroughput(solver.getThroughput(0));
						d.setUtilization(solver.getUtilization(0) / (int) d.count.getValue());
						d.setAverageResponseTime(solver.getResTime(0));
						d.setAvgWaitingTime(d.getAvgResponseTime() - d.holdingTime);
					}
					logger.debug("output: Utilization:" + d.getUtilization() + " response time: " + solver.getResTime(0) + " Throughput:"
							+ solver.getThroughput(0));
				}
			}
		}
		for (Host host : resultDistributedSystemAna.hosts) {
			for (SoftServer softserv : host.softServers) {
				for (String h : softserv.hosts) {
					SoftServerAna sa = resultDistributedSystemAna.getSoftServerAna(softserv.getName(), h);
					calculateCompoundTaskResponseTimes(sa);
				}
			}
		}
		this.linkPerformanceParams();
	}

	void calculateCompoundTaskResponseTimes(SoftServerAna sa) throws Exception {
		double tempResponseTime = 0.0;
		for (CompoundTask ct : sa.compundTasks) {
			tempResponseTime = 0.0;
			CompoundTask.SubTask prevTask = null;
			for (CompoundTask.SubTask t : ct.subTasks) {
				if (t.servername.equals(sa.getName())) {
					double temp = 0.0;
					for (DeviceServiceTime dst : sa.getSimpleTask(t.getName()).deviceServiceTimes) {
						temp += dst.getDistribution().getServiceTime()
								+ ((Device) resultDistributedSystemAna.getHost(sa.getHostName()).getDevice(dst.getDeviceName())).getAvgWaitingTime();
					}
					tempResponseTime += temp;
				} else {
					double ct_htime = 0;
					for (String hostname : resultDistributedSystemAna.getServer(t.getServerName()).hosts) {
						SoftServer s = (SoftServer) resultDistributedSystemAna.getHost(hostname).getServer(t.getServerName());
						SoftServerAna sa1 = resultDistributedSystemAna.getSoftServerAna(s.getName(), hostname);
						ct_htime += messageDelay(sa.getHostName(), hostname, sizeOfMsg(prevTask.name, t.getName()))
								+ recursiveCompoundTaskResponseTime(sa1.getCTask(t.getName(), ct.getScenarioName()), sa1)
								+ sa1.getAvgWaitingTime()
								+ messageDelay(
										hostname,
										sa.getHostName(),
										sizeOfMsg(getTaskFromReply(ct, prevTask.name, t.getName()), getTaskAfterReply(ct, prevTask.name, t.getName())));
					}
					ct_htime /= resultDistributedSystemAna.getServer(t.getServerName()).hosts.size();
					tempResponseTime += ct_htime;
				}
				prevTask = t;
			}
			ct.setResponseTime(tempResponseTime);
		}
	}

	double recursiveCompoundTaskResponseTime(CompoundTask ct, SoftServerAna sa) throws Exception {
		double resTime = 0.0;
		for (CompoundTask ctask : sa.compundTasks) {
			SubTask prevTask = null;
			if (ctask.equals(ct)) {
				for (SubTask t : ct.subTasks) {
					if (t.servername.equals(sa.name)) {
						double temp = 0.0;
						for (DeviceServiceTime dst : sa.getSimpleTask(t.getName()).deviceServiceTimes) {
							temp += dst.getDistribution().getServiceTime()
									+ ((Device) resultDistributedSystemAna.getHost(sa.getHostName()).getDevice(dst.getDeviceName())).getAvgWaitingTime();
						}
						resTime += temp;
					} else {
						double ct_htime = 0.0;
						for (String hostname : resultDistributedSystemAna.getServer(t.getServerName()).hosts) {
							SoftServer s = (SoftServer) resultDistributedSystemAna.getHost(hostname).getServer(t.getServerName());
							SoftServerAna sa1 = resultDistributedSystemAna.getSoftServerAna(s.getName(), hostname);
							ct_htime += messageDelay(sa.getHostName(), hostname, sizeOfMsg(prevTask.name, t.getName()))
									+ recursiveCompoundTaskResponseTime(sa1.getCTask(t.getName(), ct.getScenarioName()), sa1)
									+ messageDelay(
											hostname,
											sa.getHostName(),
											sizeOfMsg(getTaskFromReply(ct, prevTask.name, t.getName()),
													getTaskAfterReply(ct, prevTask.name, t.getName())));
							;
						}
						ct_htime /= resultDistributedSystemAna.getServer(t.getServerName()).hosts.size();
						resTime += ct_htime;
					}
					prevTask = t;
				}
			}
		}
		return resTime;
	}

	/**
	 * Computes softserver's holding time and think time.
	 */
	void aggregateParameters(SoftServerAna sa, String host) throws Exception {
		double rateSum = 0.0;
		sa.holdingTime = 0.0;
		sa.thinkTime = 0.0;
		
		/* Aggregating Holding Time */
		double totProb = 0.0;
		//eqn (6) with both numerator and denominator divided by arrival rate to the system
		for (CompoundTask ct : sa.compundTasks) {
			sa.holdingTime += ct.probability * ct.responseTime;
			totProb += ct.probability;
		}
		sa.holdingTime /= totProb;

		/* Aggregating Think Time */
		for (SoftServerAna.CallingServer cs : sa.callingServers) {
			if (cs.getServername().compareToIgnoreCase("user") == 0)
				rateSum += ModelParameters.getNumberOfUsers() / (getUserServerThinkTime(sa.getName()) + sa.getHoldingTime() + sa.getAvgWaitingTime());
			else {
				SoftServerAna parentServer = resultDistributedSystemAna.getSoftServerAna(cs.getServername(), cs.getHostName());
				rateSum += parentServer.thrdCount.getValue()
						/ (getSoftServerThinkTime(sa.getName(), sa.getHostName(), cs.getServername(), cs.getHostName()) + sa.getHoldingTime() + sa
								.getAvgWaitingTime());
			}
		}

		sa.thinkTime = (sa.noOfUsers / rateSum) - (sa.getHoldingTime() + sa.getAvgWaitingTime());
	}

	/**
	 * Calculating number of users at each server and device.<P>
	 * <p>
	 * We need to solve the vertex max flow problem in order to calculate the effective number of users at each server and device.<p>
	 * <p>
	 * 1st Step : Generation of the graph from the scenarios.<p>
	 * 
	 * 2nd Step : Calculating Maximum flow considering each node as a sink in the network, and thus 
	 * finding the maximum number of users at any server or device.<p>
	 */
	@SuppressWarnings("unchecked")
	void calculateNumberOfUsers() {
//		 We need to solve the vertex max flow problem in order to calculate the effective number of users at each server and device.
		
//		1st Step : Generation of the graph from the scenarios.
		SimpleDirectedWeightedGraph<GraphNode, DefaultWeightedEdge> network = createNetwork();

		// 2nd Step : Calculating Maximum flow considering each node as a sink in the network, and thus finding the maximum number of users at any
		// server or device.
		 
		@SuppressWarnings({ "rawtypes" })
		EdmondsKarpMaximumFlow maxFlow = new EdmondsKarpMaximumFlow(network);
		GraphNode userVertex = null;

		for (GraphNode gn : network.vertexSet())
			if (gn.type == NodeType.USER)
				userVertex = gn;

		for (GraphNode gn : network.vertexSet()) {
			if (gn.type == NodeType.SERVER) {
				if (gn.getSa().callingServers.get(0).servername.equals("user")) {
					gn.getSa().noOfUsers = ModelParameters.getNumberOfUsers();
					Variable v = new Variable("local", ModelParameters.getNumberOfUsers());
					if (ModelParameters.getNumberOfUsers() < gn.getSa().thrdCount.getValue())
						gn.getSa().setThreadCount(v);
				} else {
					if (gn.getName().equals(gn.getSa().getName() + "_1")) { //FIXME: bad hack of _1??
						maxFlow.calculateMaximumFlow(userVertex, gn);
						gn.getSa().noOfUsers = maxFlow.getMaximumFlowValue();
						Variable v = new Variable("local", maxFlow.getMaximumFlowValue());
						if (maxFlow.getMaximumFlowValue() < gn.getSa().thrdCount.getValue())
							gn.getSa().setThreadCount(v);
					}
				}
			} else if (gn.type == NodeType.DEVICE) {
				maxFlow.calculateMaximumFlow(userVertex, gn);
				for (Device d : gn.hd.h.devices) {
					if (d.getDeviceName().equals(gn.hd.deviceName))
						d.numberOfUsers = maxFlow.getMaximumFlowValue();
				}
			}
		}
	}

	public SimpleDirectedWeightedGraph<GraphNode, DefaultWeightedEdge> createNetwork() {
		SimpleDirectedWeightedGraph<GraphNode, DefaultWeightedEdge> g = new SimpleDirectedWeightedGraph<GraphNode, DefaultWeightedEdge>(
				DefaultWeightedEdge.class);
		/*
		 * Add user node
		 */
		GraphNode user = new GraphNode(NodeType.USER);
		user.setHD(null);
		user.setSa(null);
		g.addVertex(user);

		GraphNode n1 = null, n2 = null, n3 = null;
		DefaultWeightedEdge e1, e2;

		for (Scenario sc : resultDistributedSystemAna.scenarios) {
			Node node = sc.rootNode;
			if (node.name.equals("user")) {
				node = sc.rootNode.children.get(0);
			}
			SoftServer s = resultDistributedSystemAna.getServer(node.servername);
			for (String host : s.hosts) {
				SoftServerAna sa = resultDistributedSystemAna.getSoftServerAna(s.getName(), host);
				if (!sa.nodeAdded) {
					n1 = new GraphNode(NodeType.SERVER);
					n1.setSa(sa);
					n1.setHD(null);
					n1.setName(sa.getName() + "_1");

					n2 = new GraphNode(NodeType.SERVER);
					n2.setSa(sa);
					n2.setHD(null);
					n2.setName(sa.getName() + "_2");

					g.addVertex(n1);
					g.addVertex(n2);

					e1 = g.addEdge(user, n1);
					g.setEdgeWeight(e1, ModelParameters.getNumberOfUsers());

					e2 = g.addEdge(n1, n2);
					g.setEdgeWeight(e2, sa.thrdCount.getValue());

					Host h = resultDistributedSystemAna.getHost(sa.hostName);
					Task t = sa.getSimpleTask(node.name);

					for (DeviceServiceTime dst : t.deviceServiceTimes) {
						if (!addedToGraph(h, dst.getDeviceName())) {
							/*
							 * If the device is not yet added to the graph
							 */
							HostDevice hd = new HostDevice(h, dst.getDeviceName());
							hostDeviceList.add(hd);

							n3 = new GraphNode(NodeType.DEVICE);
							n3.setHD(hd);
							n3.setSa(null);
							n3.setName(h.name + " " + dst.getDeviceName());

							g.addVertex(n3);

							e1 = g.addEdge(n2, n3);
							g.setEdgeWeight(e1, sa.thrdCount.getValue());
						} else {
							for (GraphNode gn : g.vertexSet()) {
								if (gn.type == NodeType.DEVICE && gn.hd.deviceName.equals(dst.getDeviceName()) && gn.hd.h.equals(h)
										&& gn.name.equals(h.name + " " + dst.getDeviceName())) {
									e1 = g.addEdge(n2, gn);
									g.setEdgeWeight(e1, sa.thrdCount.getValue());
									break;
								}
							}
						}
					}
					sa.nodeAdded = true;
				} else {
					for (GraphNode gn : g.vertexSet()) {
						if (gn.getType() == NodeType.SERVER) {
							if (gn.getSa().equals(sa) && gn.getName().equals(sa.getName() + "_2")) {
								n2 = gn;
								break;
							}
						}
					}

					Host h = resultDistributedSystemAna.getHost(sa.hostName);
					Task t = sa.getSimpleTask(node.name);

					for (DeviceServiceTime dst : t.deviceServiceTimes) {
						if (!addedToGraph(h, dst.getDeviceName())) {
							/*
							 * If the device is not yet added to the graph
							 */
							HostDevice hd = new HostDevice(h, dst.getDeviceName());
							hostDeviceList.add(hd);

							n3 = new GraphNode(NodeType.DEVICE);
							n3.setHD(hd);
							n3.setSa(null);
							n3.setName(h.name + " " + dst.getDeviceName());

							g.addVertex(n3);

							e1 = g.addEdge(n2, n3);
							g.setEdgeWeight(e1, sa.thrdCount.getValue());
						} else {
							for (GraphNode gn : g.vertexSet()) {
								if (gn.type == NodeType.DEVICE && gn.hd.deviceName.equals(dst.getDeviceName()) && gn.hd.h.equals(h)
										&& gn.name.equals(h.name + " " + dst.getDeviceName())) {
									if (!g.containsEdge(n2, gn)) {
										e1 = g.addEdge(n2, gn);
										g.setEdgeWeight(e1, sa.thrdCount.getValue());
									}
									break;
								}
							}
						}
					}
				}
			}

			for (Node n : sc.rootNode.children) {
				addRecursiveGraph(n, g, n2);
			}
		}
		return g;
	}

	/*
     * 
     */
	void addRecursiveGraph(Node node, SimpleDirectedWeightedGraph<GraphNode, DefaultWeightedEdge> g, GraphNode parentNode) {
		GraphNode n1 = null, n2 = null, n3 = null;
		SoftServer s = resultDistributedSystemAna.getServer(node.servername);
		for (String host : s.hosts) {
			SoftServerAna sa = resultDistributedSystemAna.getSoftServerAna(s.getName(), host);
			if (!sa.nodeAdded) {
				/*
				 * Software server is not yet added to the graph
				 */
				n1 = new GraphNode(NodeType.SERVER);
				n1.setSa(sa);
				n1.setHD(null);
				n1.setName(sa.getName() + "_1");

				n2 = new GraphNode(NodeType.SERVER);
				n2.setSa(sa);
				n2.setHD(null);
				n2.setName(sa.getName() + "_2");

				g.addVertex(n1);
				g.addVertex(n2);

				DefaultWeightedEdge e1 = g.addEdge(parentNode, n1);
				g.setEdgeWeight(e1, parentNode.getSa().thrdCount.getValue());

				DefaultWeightedEdge e2 = g.addEdge(n1, n2);
				g.setEdgeWeight(e2, sa.thrdCount.getValue());

				Host h = resultDistributedSystemAna.getHost(sa.hostName);
				Task t = sa.getSimpleTask(node.name);

				for (DeviceServiceTime dst : t.deviceServiceTimes) {
					if (!addedToGraph(h, dst.getDeviceName())) {
						/*
						 * If the device is not yet added to the graph
						 */
						HostDevice hd = new HostDevice(h, dst.getDeviceName());
						hostDeviceList.add(hd);

						n3 = new GraphNode(NodeType.DEVICE);
						n3.setHD(hd);
						n3.setSa(null);
						n3.setName(h.name + " " + dst.getDeviceName());

						g.addVertex(n3);

						e1 = g.addEdge(n2, n3);
						g.setEdgeWeight(e1, sa.thrdCount.getValue());
					} else {
						for (GraphNode gn : g.vertexSet()) {
							if (gn.type == NodeType.DEVICE && gn.hd.deviceName.equals(dst.getDeviceName()) && gn.hd.h.equals(h)
									&& gn.name.equals(h.name + " " + dst.getDeviceName())) {
								e1 = g.addEdge(n2, gn);
								g.setEdgeWeight(e1, sa.thrdCount.getValue());
								break;
							}
						}
					}
				}
				sa.nodeAdded = true;
			} else {
				for (GraphNode gn : g.vertexSet()) {
					if (gn.type == NodeType.SERVER) {
						if (gn.getSa().equals(sa) && gn.getName().equals(sa.getName() + "_2")) {
							n2 = gn;
							break;
						}
					}
				}
				Host h = resultDistributedSystemAna.getHost(sa.hostName);
				Task t = sa.getSimpleTask(node.name);

				for (DeviceServiceTime dst : t.deviceServiceTimes) {
					if (!addedToGraph(h, dst.getDeviceName())) {
						HostDevice hd = new HostDevice(h, dst.getDeviceName());
						hostDeviceList.add(hd);

						n3 = new GraphNode(NodeType.DEVICE);
						n3.setHD(hd);
						n3.setSa(null);
						n3.setName(h.name + " " + dst.getDeviceName());

						g.addVertex(n3);

						DefaultWeightedEdge e1 = g.addEdge(n2, n3);
						g.setEdgeWeight(e1, sa.thrdCount.getValue());
					} else {
						for (GraphNode gn : g.vertexSet()) {
							if (gn.type == NodeType.DEVICE && gn.hd.deviceName.equals(dst.getDeviceName()) && gn.hd.h.equals(h)
									&& gn.name.equals(h.name + " " + dst.getDeviceName())) {
								if (!g.containsEdge(n2, gn)) {
									DefaultWeightedEdge e1 = g.addEdge(n2, gn);
									g.setEdgeWeight(e1, sa.thrdCount.getValue());
								}
								break;
							}
						}
					}
				}
			}
		}
		for (Node n : node.children) {
			addRecursiveGraph(n, g, n2);
		}
	}

	/*
	 * Checks whether device has been added to the Graph or not
	 */
	boolean addedToGraph(Host h, String deviceName) {
		for (HostDevice hd : hostDeviceList) {
			if (hd.h.equals(h) && hd.deviceName.equals(deviceName))
				return true;
		}
		return false;
	}

	/**
	 * Calculate Think Time for a Software Queuing System
	 */

	double getSoftServerThinkTime(String serverName, String hostName, String parentServerName, String parentHostName) throws Exception {

		Host parentHost = resultDistributedSystemAna.getHost(parentHostName);
		SoftServerAna parentServerAna = resultDistributedSystemAna.getSoftServerAna(parentServerName, parentHostName);
		SoftServerAna softserv = resultDistributedSystemAna.getSoftServerAna(serverName, hostName);

		/* Idle Time Calculation */
		/* Need to take care of the machine too */
		//eqn (20)
		double idleThinkTime = parentServerAna.thrdCount.getValue() / parentServerAna.getThroughput()
									- parentServerAna.getHoldingTime();

		/* Time spent by parent server calling other Compound Tasks */
		double parentServTotProb = 0, servTasksProb = 0, parentTasksProb = 0;
		for (CompoundTask c : parentServerAna.compundTasks) {
			parentServTotProb += c.getProbability();
		}
		for (CompoundTask c : softserv.compundTasks) {
			if (c.getParentServer().equals(parentServerAna.getName())) {
				servTasksProb += c.getProbability();// / c.numInvocations;
			}
		}
		//eqn (22)
		parentTasksProb = servTasksProb / (parentServTotProb * softserv.hosts.size());
		//eqn (21)
		double parentOtherCompoundTasks = (1 / parentTasksProb - 1) * (parentServerAna.thrdCount.getValue() / parentServerAna.getThroughput());

		/*
		 * Time spent by the compound task in other simple tasks Calculating the overall think time along with this
		 */
		double tempThinkTime = 0.0;
		int childInvocations = 0;
		int tempInv = 0;
		double firstTermTotProb = 0.0;
		double secondTermTotProb = 0.0;
		double firstTerm = 0.0;
		double secondTerm = 0.0;

		for (CompoundTask ct1 : parentServerAna.compundTasks) {
			if (isServerBeingCalled(ct1, serverName)) {
				for (CompoundTask ct2 : parentServerAna.compundTasks) {
					if (isServerBeingCalled(ct2, serverName))
						secondTermTotProb += ct1.getProbability() * ct2.getProbability();
				}
			}
		}

		for (CompoundTask ct1 : parentServerAna.compundTasks) {
			if (isServerBeingCalled(ct1, serverName)) {
				for (CompoundTask ct2 : parentServerAna.compundTasks) {
					if (isServerBeingCalled(ct2, serverName)) {
						secondTerm += ct1.getProbability() * ct2.getProbability() * (parentOtherCompoundTasks + idleThinkTime);
						childInvocations = getNumberOfInvocations(ct1, parentServerName, serverName);
						tempInv = childInvocations;
						SubTask prevtask = null;
						for (SubTask t : ct1.subTasks) {
							if (t.servername.compareToIgnoreCase(serverName) != 0) {
								if (tempInv == childInvocations)
									continue;
								else if (tempInv == 0) {
									if (t.servername.compareToIgnoreCase(parentServerName) == 0) {
										double temp = 0.0;
										for (DeviceServiceTime dst : (parentServerAna).getSimpleTask(t.getName()).deviceServiceTimes) {
											temp += dst.getDistribution().getServiceTime()
													+ ((Device) parentHost.getDevice(dst.getDeviceName())).getAvgWaitingTime();
										}
										secondTerm += temp * ct1.getProbability() * ct2.getProbability();
									} else {
										double ct_htime = 0;
										for (String hostname : resultDistributedSystemAna.getServer(t.getServerName()).hosts) {
											SoftServer s = (SoftServer) resultDistributedSystemAna.getHost(hostname).getServer(t.getServerName());
											SoftServerAna sa = resultDistributedSystemAna.getSoftServerAna(s.getName(), hostname);
											ct_htime += messageDelay(parentHostName, hostname, sizeOfMsg(prevtask.name, t.getName()))
													+ sa.getCTask(t.getName(), ct1.getScenarioName()).getResponseTime()
													+ sa.getAvgWaitingTime()
													+ messageDelay(
															hostname,
															parentHostName,
															sizeOfMsg(getTaskFromReply(ct1, prevtask.name, t.getName()),
																	getTaskAfterReply(ct1, prevtask.name, t.getName())));
										}
										ct_htime /= resultDistributedSystemAna.getServer(t.getServerName()).hosts.size();
										secondTerm += ct_htime * ct1.getProbability() * ct2.getProbability();
									}
								} else {
									if (t.servername.compareToIgnoreCase(parentServerName) == 0) {
										double temp = 0.0;
										for (DeviceServiceTime dst : (parentServerAna).getSimpleTask(t.getName()).deviceServiceTimes) {
											temp += dst.getDistribution().getServiceTime()
													+ ((Device) parentHost.getDevice(dst.getDeviceName())).getAvgWaitingTime();
										}
										firstTerm += temp * ct1.getProbability();
										firstTermTotProb += ct1.getProbability();
									} else {
										double ct_htime = 0;
										for (String hostname : resultDistributedSystemAna.getServer(t.getServerName()).hosts) {
											SoftServer s = (SoftServer) resultDistributedSystemAna.getHost(hostname).getServer(t.getServerName());
											SoftServerAna sa = resultDistributedSystemAna.getSoftServerAna(s.getName(), hostname);
											ct_htime += messageDelay(parentHostName, hostname, sizeOfMsg(prevtask.name, t.getName()))
													+ sa.getCTask(t.getName(), ct1.getScenarioName()).getResponseTime()
													+ sa.getAvgWaitingTime()
													+ messageDelay(
															hostname,
															parentHostName,
															sizeOfMsg(getTaskFromReply(ct1, prevtask.name, t.getName()),
																	getTaskAfterReply(ct1, prevtask.name, t.getName())));
										}
										ct_htime /= resultDistributedSystemAna.getServer(t.getServerName()).hosts.size();
										firstTerm += ct_htime * ct1.getProbability();
										firstTermTotProb += ct1.getProbability();
									}
								}
							} else {
								tempInv--;
								continue;
							}
							prevtask = t;
						}

						for (SubTask t : ct2.subTasks) {
							if (t.servername.compareToIgnoreCase(serverName) != 0) {
								if (t.servername.compareToIgnoreCase(parentServerName) == 0) {
									double temp = 0.0;
									for (DeviceServiceTime dst : (parentServerAna).getSimpleTask(t.getName()).deviceServiceTimes) {
										temp += dst.getDistribution().getServiceTime()
												+ ((Device) parentHost.getDevice(dst.getDeviceName())).getAvgWaitingTime();
									}
									secondTerm += temp * ct1.getProbability() * ct2.getProbability();
								} else {
									double ct_htime = 0;
									for (String hostname : resultDistributedSystemAna.getServer(t.getServerName()).hosts) {
										SoftServer s = (SoftServer) resultDistributedSystemAna.getHost(hostname).getServer(t.getServerName());
										SoftServerAna sa = resultDistributedSystemAna.getSoftServerAna(s.getName(), hostname);
										ct_htime += messageDelay(parentHostName, hostname, sizeOfMsg(prevtask.name, t.getName()))
												+ sa.getCTask(t.getName(), ct1.getScenarioName()).getResponseTime()
												+ messageDelay(
														hostname,
														parentHostName,
														sizeOfMsg(getTaskFromReply(ct1, prevtask.name, t.getName()),
																getTaskAfterReply(ct1, prevtask.name, t.getName())));
									}
									ct_htime /= resultDistributedSystemAna.getServer(t.getServerName()).hosts.size();
									secondTerm += ct_htime * ct1.getProbability() * ct2.getProbability();
								}
							} else
								break;
						}
					}
					tempThinkTime += secondTerm;
					secondTerm = 0;
				}
			}
			tempThinkTime += firstTerm;
			firstTerm = 0;
		}
		return ((tempThinkTime / (firstTermTotProb + secondTermTotProb)));
	}

	int getNumberOfInvocations(CompoundTask ct, String parentServerName, String serverName) {
		int invocations = 0;
		for (SubTask t : ct.subTasks) {
			if (t.servername.compareToIgnoreCase(serverName) == 0)
				invocations++;
		}
		return invocations;
	}

	boolean isServerBeingCalled(CompoundTask ct, String server) {
		for (SubTask t : ct.subTasks)
			if (t.servername.compareToIgnoreCase(server) == 0)
				return true;
		return false;
	}

	/**
	 * For every server, for every CT, this function creates a list of all the CTs of its server that are invoked by the same parent
	 */
	public void initializePairTasks() {
		for (Host host : resultDistributedSystemAna.hosts) {
			for (SoftServer softserv : host.softServers) {
				SoftServerAna sa = resultDistributedSystemAna.getSoftServerAna(softserv.getName(), host.getName());
				for (CompoundTask ct : sa.compundTasks) {
					double sumprob = 0;
					for (CompoundTask pairct : sa.compundTasks) {
						if (ct.getParentServer() == pairct.getParentServer()) {
							PairValue p = ct.new PairValue(pairct);
							ct.pairTasks.add(p);
//							sumprob += sa.getCTask(p.jobClassName, p.Scename).getprobability();
							sumprob += pairct.getProbability();
						}
					}
					for (PairValue pair : ct.pairTasks) {
						pair.changeProb = sa.getCTask(pair.jobClassName, pair.Scename).getProbability() / sumprob;
					}
				}
			}
		}
	}

	/**
	 * Initializes conditional probability of a compound task given the software server.
	 */
	public void initializeConditionalProb() {
		for (Host host : resultDistributedSystemAna.hosts) {
			for (SoftServer softserv : host.softServers) {
				SoftServerAna sa = resultDistributedSystemAna.getSoftServerAna(softserv.getName(), host.getName());
				double sumprob = 0;
				for (CompoundTask ct : sa.compundTasks) {
					sumprob += ct.probability;
				}
				for (CompoundTask ct : sa.compundTasks) {
					ct.conditionalProbability = ct.probability / sumprob;
				}
			}
		}
	}

	/**
	 * Initializing number of users and think time for end user compound tasks (assuming all scenarios start with the same server, therefore think
	 * time initialized to end user think time. These values do not change throughout the solution. So this function is invoked only once.
	 */
	private void dfs(Node n, double threadcount) throws Exception {
		if (n.isSync()) {
			threadcount = setServerThreads(n.servername, threadcount);
		}
		for (Node child : n.children) {
			dfs(child, threadcount);
		}

	}

	double setServerThreads(String servname, double threadcount) {
		double setvalue = 0;
		for (Host h : resultDistributedSystemAna.hosts) {
			for (Object s : h.softServers) {
				if (((SoftServerAna) s).name.equals(servname)) {
					setvalue = resultDistributedSystemAna.getServer(servname).thrdCount.getValue();
					if (((int) setvalue) >= ((int) threadcount)) {
						((SoftServerAna) s).thrdCount.setValue(threadcount);
						setvalue = threadcount;
					}
				}
			}
		}
		if (servname.equalsIgnoreCase("user")) {
			return threadcount;
		}
		return setvalue;

	}

	String getTaskAfterReply(CompoundTask ct, String task1, String task2) {
		String returning_servname = "junkvalue";
		for (SubTask t : ct.subTasks) {

			if (t.servername.equalsIgnoreCase(returning_servname)) {
				return t.name;
			}
			if (t.name.equalsIgnoreCase(task1)) {
				returning_servname = t.servername;
			}

		}
		return "did not found";
	}

	String getTaskFromReply(CompoundTask ct, String task1, String task2) {
		String returning_servname = null;
		SubTask prevtask = ct.subTasks.get(0);
		for (SubTask t : ct.subTasks) {
			if (t.servername.equalsIgnoreCase(returning_servname)) {
				return prevtask.name;
			}
			if (t.name.equalsIgnoreCase(task1)) {
				returning_servname = t.servername;
			}
			prevtask = t;
		}
		return "did not found";
	}

	/**
	 * Calculates holding times of devices for a given host.
	 * 
	 * TODO: find out which eqn this belongs to.
	 * @param host
	 */
	void calculateDeviceHoldingTime(Host host) {
		double totProb = 0.0;
		for (Device device : host.devices) {
			for (SoftServer s : host.softServers) {
				SoftServerAna server = resultDistributedSystemAna.getSoftServerAna(s.getName(), host.getName());
				for (CompoundTask compoundTask : server.compundTasks) {
					for (SubTask subTask : compoundTask.subTasks) {
						if (subTask.servername.equalsIgnoreCase(server.getName())) {
							if (isDeviceBeingCalled(subTask, device, server)) {
								Task simpleTask = server.getSimpleTask(subTask.getName());
								for (DeviceServiceTime dst : simpleTask.deviceServiceTimes) {
									if (dst.getDeviceName().compareToIgnoreCase(device.name) == 0) {
										device.holdingTime += compoundTask.getProbability() * simpleTask.getServiceTime(device.name).getServiceTime();
										totProb += compoundTask.getProbability();
									}
								}
							}
						}
					}
				}
			}
			if (totProb != 0) {
				device.holdingTime /= totProb;
				totProb = 0.0;
			}
			device.setAverageResponseTime(device.holdingTime);
		}
	}

	/*
	 * Modified by Mayur
	 */
	void calculateDeviceThinkTimes(Host host) throws Exception {
		double rateSum = 0;
		for (Device d : host.devices) {
			for (SoftServer server : host.softServers) {
				SoftServerAna parentServer = resultDistributedSystemAna.getSoftServerAna(server.getName(), host.getName());
				if (isDeviceBeingCalled(d, parentServer, host.getName())) {
					rateSum += parentServer.thrdCount.getValue() / (getServerDeviceThinkTime(d, parentServer, host.getName()) + d.getAvgResponseTime());
				}
			}
			d.thinkTime = (d.numberOfUsers / rateSum) - d.getAvgResponseTime();
			rateSum = 0;
		}
	}

	double getServerDeviceThinkTime(Device d, SoftServerAna parentServer, String hostname) throws Exception {
		/*
		 * Calculating parent Ilde time
		 */

		double parentIdleTime = (parentServer.thrdCount.getValue() / parentServer.getThroughput()) - parentServer.getHoldingTime();

		/*
		 * Calculating Other Compound tasks think time
		 */

		Host host = resultDistributedSystemAna.getHost(hostname);

		double parentOtherTasks = 0.0;
		double parentServTotProb = 0, servDeviceProb = 0, parentServDeviceProb = 0;

		for (CompoundTask c : parentServer.compundTasks) {
			parentServTotProb += c.getProbability();
		}

		for (CompoundTask ct : parentServer.compundTasks) {
			if (isDeviceBeingCalled(d, parentServer, ct))
				servDeviceProb += ct.getProbability();
		}

		parentServDeviceProb = servDeviceProb / parentServTotProb;

		parentOtherTasks = (1 / parentServDeviceProb - 1) * (parentServer.thrdCount.getValue() / parentServer.getThroughput());

		/*
		 * Calculating other simple tasks and total think time
		 */

		double tempThinkTime = 0.0;
		int deviceInvocations = 0;
		int tempInv;

		double firstTermTotProb = 0.0;
		double secondTermTotProb = 0.0;
		double firstTerm = 0.0;
		double secondTerm = 0.0;

		for (CompoundTask ct1 : parentServer.compundTasks) {
			if (isDeviceBeingCalled(d, parentServer, ct1)) {
				for (CompoundTask ct2 : parentServer.compundTasks) {
					if (isDeviceBeingCalled(d, parentServer, ct2))
						secondTermTotProb += ct1.getProbability() * ct2.getProbability();
				}
			}
		}

		for (CompoundTask ct1 : parentServer.compundTasks) {
			if (isDeviceBeingCalled(d, parentServer, ct1)) {
				for (CompoundTask ct2 : parentServer.compundTasks) {
					if (isDeviceBeingCalled(d, parentServer, ct2)) {
						secondTerm += ct1.getProbability() * ct2.getProbability() * (parentOtherTasks + parentIdleTime);
						deviceInvocations = getDeviceInvocations(ct1, d, parentServer);
						tempInv = deviceInvocations;
						SubTask prevTask = null;
						for (SubTask t : ct1.subTasks) {
							if (t.servername.compareToIgnoreCase(parentServer.getName()) == 0) {
								if (isDeviceBeingCalled(t, d, parentServer)) {
									if (tempInv == deviceInvocations) { // Device is not called yet
										tempInv--;
										if (tempInv == 0) {
											Task st = parentServer.getSimpleTask(t.getName());
											boolean deviceFlag = false;
											for (DeviceServiceTime dst : st.deviceServiceTimes) {
												if (deviceFlag) {
													Device dev = (Device) host.getDevice(dst.getDeviceName());
													secondTerm += ct1.getProbability() * ct2.getProbability() * dev.getAvgResponseTime();

												}
												if (dst.getDeviceName().compareToIgnoreCase(d.name) == 0) {
													deviceFlag = true;
												}
											}
										} else {
											Task st = parentServer.getSimpleTask(t.getName());
											boolean deviceFlag = false;
											for (DeviceServiceTime dst : st.deviceServiceTimes) {
												if (deviceFlag) {
													Device dev = (Device) host.getDevice(dst.getDeviceName());
													firstTerm += ct1.getProbability() * dev.getAvgResponseTime();
													// firstTermTotProb += ct1.getprobability();
												}
												if (dst.getDeviceName().compareToIgnoreCase(d.name) == 0)
													deviceFlag = true;
											}
										}
									} else if (tempInv == 0) { // Device not going to be called anymore in this CT
										/*
										 * Add other simple Task's response time multiplied by ct1.getprobability*ct2.getprobability
										 */
										Task st = parentServer.getSimpleTask(t.getName());
										for (DeviceServiceTime dst : st.deviceServiceTimes) {
											Device dev = (Device) host.getDevice(dst.getDeviceName());
											secondTerm += ct1.getProbability() * ct2.getProbability() * dev.getAvgResponseTime();
										}
									} else { // Between two calls to the device
										/*
										 * Add the simple Task's response time multiplied by ct1.getprobability
										 */
										tempInv--;
										Task st = parentServer.getSimpleTask(t.getName());
										boolean deviceFlag = false;
										if (tempInv == 0) {
											for (DeviceServiceTime dst : st.deviceServiceTimes) {
												if (dst.getDeviceName().compareToIgnoreCase(d.name) == 0)
													deviceFlag = true;
												else {
													if (deviceFlag) {
														Device dev = (Device) host.getDevice(dst.getDeviceName());
														secondTerm += ct1.getProbability() * ct2.getProbability() * dev.getAvgResponseTime();
													} else {
														Device dev = (Device) host.getDevice(dst.getDeviceName());
														firstTerm += ct1.getProbability() * dev.getAvgResponseTime();
													}
												}

											}
										} else {
											for (DeviceServiceTime dst : st.deviceServiceTimes) {
												if (dst.getDeviceName().compareToIgnoreCase(d.name) != 0) {
													Device dev = (Device) host.getDevice(dst.getDeviceName());
													firstTerm += ct1.getProbability() * dev.getAvgResponseTime();
												}
											}
										}
									}
								} else {
									if (tempInv == deviceInvocations)
										continue;
									else if (tempInv == 0) {
										Task st = parentServer.getSimpleTask(t.getName());
										for (DeviceServiceTime dst : st.deviceServiceTimes) {
											Device dev = (Device) host.getDevice(dst.getDeviceName());
											secondTerm += ct1.getProbability() * ct2.getProbability() * dev.getAvgResponseTime();
										}
									} else {
										Task st = parentServer.getSimpleTask(t.getName());
										for (DeviceServiceTime dst : st.deviceServiceTimes) {
											Device dev = (Device) host.getDevice(dst.getDeviceName());
											firstTerm += ct1.getProbability() * dev.getAvgResponseTime();
											firstTermTotProb += ct1.getProbability();
										}
									}
								}
							} else {
								if (tempInv == deviceInvocations) // Device not yet called
									continue;
								else if (tempInv == 0) { // Device not going to be called anymore in this CT
									/*
									 * Add simple Task's response time multiplied by ct1.getprobability*ct2.getprobability
									 */
									double ct_htime = 0;
									for (String hostName : resultDistributedSystemAna.getServer(t.getServerName()).hosts) {
										// message Delay was added for the purpose of Network Layer Calculation
										SoftServer s = (SoftServer) resultDistributedSystemAna.getHost(hostName).getServer(t.getServerName());
										SoftServerAna sa = resultDistributedSystemAna.getSoftServerAna(s.getName(), hostName);
										ct_htime += messageDelay(parentServer.getHostName(), hostName, sizeOfMsg(prevTask.name, t.getName()))
												+ sa.getCTask(t.getName(), ct1.getScenarioName()).getResponseTime()
												+ sa.getAvgWaitingTime()
												+ messageDelay(
														hostName,
														parentServer.getHostName(),
														sizeOfMsg(getTaskFromReply(ct1, prevTask.name, t.getName()),
																getTaskAfterReply(ct1, prevTask.name, t.getName())));
									}
									ct_htime /= resultDistributedSystemAna.getServer(t.getServerName()).hosts.size();
									secondTerm += ct_htime * ct1.getProbability() * ct2.getProbability();
								} else { // Between two calls to the device
									/*
									 * Add simple Task's response time mulitiplied by ct1.getprobability
									 */
									double ct_htime = 0;
									for (String hostName : resultDistributedSystemAna.getServer(t.getServerName()).hosts) {
										SoftServer s = (SoftServer) resultDistributedSystemAna.getHost(hostName).getServer(t.getServerName());
										SoftServerAna sa = resultDistributedSystemAna.getSoftServerAna(s.getName(), hostName);
										ct_htime += messageDelay(parentServer.getHostName(), hostName, sizeOfMsg(prevTask.name, t.getName()))
												+ sa.getCTask(t.getName(), ct1.getScenarioName()).getResponseTime()
												+ sa.getAvgWaitingTime()
												+ messageDelay(
														hostName,
														parentServer.getHostName(),
														sizeOfMsg(getTaskFromReply(ct1, prevTask.name, t.getName()),
																getTaskAfterReply(ct1, prevTask.name, t.getName())));
									}
									ct_htime /= resultDistributedSystemAna.getServer(t.getServerName()).hosts.size();
									firstTerm += ct_htime * ct1.getProbability();
									firstTermTotProb += ct1.getProbability();
								}
							}
							prevTask = t;
						}
						prevTask = null;
						for (SubTask t : ct2.subTasks) {
							if (t.servername.compareToIgnoreCase(parentServer.getName()) == 0) {
								if (isDeviceBeingCalled(t, d, parentServer)) {
									Task st = parentServer.getSimpleTask(t.getName());
									for (DeviceServiceTime dst : st.deviceServiceTimes) {
										if (dst.getDeviceName().compareToIgnoreCase(d.name) == 0)
											break;
										else {
											Device dev = (Device) host.getDevice(dst.getDeviceName());
											secondTerm += ct1.getProbability() * ct2.getProbability() * dev.getAvgResponseTime();
										}
									}
									break;
								} else {
									Task st = parentServer.getSimpleTask(t.getName());
									for (DeviceServiceTime dst : st.deviceServiceTimes) {
										Device dev = (Device) host.getDevice(dst.getDeviceName());
										secondTerm += ct1.getProbability() * ct2.getProbability() * dev.getAvgResponseTime();
									}
								}
							} else {
								double ct_htime = 0;
								for (String hostName : resultDistributedSystemAna.getServer(t.getServerName()).hosts) {
									SoftServer s = (SoftServer) resultDistributedSystemAna.getHost(hostName).getServer(t.getServerName());
									SoftServerAna sa = resultDistributedSystemAna.getSoftServerAna(s.getName(), hostName);
									ct_htime += messageDelay(parentServer.getHostName(), hostName, sizeOfMsg(prevTask.name, t.getName()))
											+ sa.getCTask(t.getName(), ct2.getScenarioName()).getResponseTime()
											+ sa.getAvgWaitingTime()
											+ messageDelay(
													hostName,
													parentServer.getHostName(),
													sizeOfMsg(getTaskFromReply(ct2, prevTask.name, t.getName()),
															getTaskAfterReply(ct2, prevTask.name, t.getName())));
								}
								ct_htime /= resultDistributedSystemAna.getServer(t.getServerName()).hosts.size();
								secondTerm += ct_htime * ct1.getProbability() * ct2.getProbability();
							}
							prevTask = t;
						}
					}
					tempThinkTime += secondTerm;
					secondTerm = 0;
				}
			}
			tempThinkTime += firstTerm;
			firstTerm = 0;
		}

		return (tempThinkTime / (firstTermTotProb + secondTermTotProb));
	}

	int getDeviceInvocations(CompoundTask ct, Device d, SoftServerAna sa) {
		int inv = 0;
		for (SubTask t : ct.subTasks) {
			if (t.servername.compareToIgnoreCase(sa.getName()) == 0) {
				Task st = sa.getSimpleTask(t.getName());
				for (DeviceServiceTime dst : st.deviceServiceTimes) {
					if (dst.getDeviceName().compareToIgnoreCase(d.name) == 0)
						inv++;
				}
			}
		}
		return inv;
	}

	boolean isDeviceBeingCalled(SubTask subTask, Device device, SoftServerAna server) {
		boolean flag = false;
		for (Task task : server.simpleTasks) {
			if (subTask.getName().equalsIgnoreCase(task.name))
				flag = true;
		}
		if (!flag)
			return false;

		Task simpleTask = server.getSimpleTask(subTask.getName());
		for (DeviceServiceTime dst : simpleTask.deviceServiceTimes) {
			if (dst.getDeviceName().equalsIgnoreCase(device.getDeviceName()))
				return true;
		}
		return false;
	}

	boolean isDeviceBeingCalled(Device d, SoftServerAna sa, CompoundTask ct) {
		for (SubTask t : ct.subTasks) {
			if (t.servername.equalsIgnoreCase(sa.getName())) {
				Task st = sa.getSimpleTask(t.getName());
				for (DeviceServiceTime dst : st.deviceServiceTimes) {
					if (dst.getDeviceName().equalsIgnoreCase(d.getDeviceName()))
						return true;
				}
			}
		}
		return false;
	}

	boolean isDeviceBeingCalled(Device d, SoftServerAna sa, String hostname) {
		for (CompoundTask ct : sa.compundTasks) {
			for (SubTask t : ct.subTasks) {
				if (t.servername.compareToIgnoreCase(sa.getName()) == 0) {
					Task st = sa.getSimpleTask(t.getName());
					for (DeviceServiceTime dst : st.deviceServiceTimes) {
						if (dst.getDeviceName().compareToIgnoreCase(d.getDeviceName()) == 0)
							return true;
					}
				}
			}
		}
		return false;
	}

	/** Holding time initialization for all compound tasks */
	void calculateInitialTaskHoldingValues() throws Exception {
		for (Host currentHost : resultDistributedSystemAna.hosts) {
			for (SoftServer softserv : currentHost.softServers) {
				SoftServerAna softServerAna = resultDistributedSystemAna.getSoftServerAna(softserv.getName(), currentHost.getName());
				for (CompoundTask compoundTask : softServerAna.compundTasks) {
					double compundTaskHoldingTime = 0;
					SubTask prevtask = null;
					for (SubTask subTask : compoundTask.subTasks) {
						if (subTask.getServerName().compareTo(softServerAna.getName()) == 0) {
							//eqn (3)
							//If it is a simple task on the same machine
							for (DeviceServiceTime dst : resultDistributedSystemAna.getTask(subTask.getName()).deviceServiceTimes) {
								//DOUBT: should the distribution check be made here for exp?
								compundTaskHoldingTime += dst.getDistribution().getServiceTime();
							}
						} else { //else get the compound task response time add network delay
							double temp = 0;
							for (String hostName : resultDistributedSystemAna.getServer(subTask.getServerName()).hosts) {
								if (network = true) {
									//eqn (4)
									temp += messageDelay(currentHost.name, hostName, sizeOfMsg(prevtask.name, subTask.getName()))
											+ getInitialIndividualTaskHoldingTime(hostName, subTask.getServerName(), subTask.getName(), compoundTask.getScenarioName(),
													compoundTask.ClassName)
											+ messageDelay(hostName, currentHost.name,
													sizeOfMsg(getTaskFromReply(compoundTask, prevtask.name, subTask.getName()),
															getTaskAfterReply(compoundTask, prevtask.name, subTask.getName())));
								} else {
									//eqn (5) numerator
									temp += getInitialIndividualTaskHoldingTime(hostName, subTask.getServerName(), subTask.getName(), compoundTask.getScenarioName(),
											compoundTask.ClassName);
								}
							}
							//eqn (5) denominator
							temp /= resultDistributedSystemAna.getServer(subTask.getServerName()).hosts.size();
							compundTaskHoldingTime += temp;
						}
						prevtask = subTask;
					}
					compoundTask.setHoldingTime(compundTaskHoldingTime);
					compoundTask.setResponseTime(compundTaskHoldingTime);
					logger.debug("host " + (currentHost).getName() + " compnd task  " + compoundTask.getName() + "compound task holding time = " + compoundTask.getHoldingTime());
				}
			}
		}
	}

	public double getInitialIndividualTaskHoldingTime(String hostname, String servername, String taskname, String Scename, String classname)
			throws Exception {
		double ct_htime;
		Host h = resultDistributedSystemAna.getHost(hostname);
		SoftServerAna sa = resultDistributedSystemAna.getSoftServerAna(servername, h.getName());
		CompoundTask ct = sa.getCTask(taskname, Scename);

		ct_htime = 0;
		SubTask prevtask = null;
		for (SubTask t : ct.subTasks) {
			if (t.getServerName().compareTo(servername) == 0) {
				for (DeviceServiceTime dst : resultDistributedSystemAna.getTask(t.getName()).deviceServiceTimes) {
					ct_htime += dst.getDistribution().getServiceTime();
				}
			} else {
				double temp = 0;
				for (String hname : resultDistributedSystemAna.getServer(t.getServerName()).hosts) {
					temp += messageDelay(hostname, hname, sizeOfMsg(prevtask.name, t.getName()))
							+ getInitialIndividualTaskHoldingTime(hname, t.getServerName(), t.getName(), ct.getScenarioName(), ct.ClassName)
							+ messageDelay(hname, hostname,
									sizeOfMsg(getTaskFromReply(ct, prevtask.name, t.getName()), getTaskAfterReply(ct, prevtask.name, t.getName())));
				}
				temp /= resultDistributedSystemAna.getServer(t.getServerName()).hosts.size();
				ct_htime += temp;
			}
			prevtask = t;
		}
		ct.setHoldingTime(ct_htime);
		ct.setResponseTime(ct_htime);
		logger.debug(" In Individual  host " + hostname + "CT " + ct.getName() + " compound task holding time = " + ct.getHoldingTime());

		return ct_htime;
	}

	/*
	 * Iteratively calculating taks holding times using the response time values of the tasks synchronously invoked
	 */
	void calculateTaskHoldingValues(SoftServerAna sa, Host host) throws Exception {
		double ct_htime;
		for (CompoundTask ct : sa.compundTasks) {
			ct_htime = 0;
			SubTask prevtask = null;
			for (SubTask t : ct.subTasks) {
				if (t.getServerName().compareTo(sa.getName()) == 0) {
					for (DeviceServiceTime dst : sa.getSimpleTask(t.getName()).deviceServiceTimes) {
						ct_htime += dst.getDistribution().getServiceTime() + ((Device) host.getDevice(dst.getDeviceName())).getAvgWaitingTime();
					}
				} else {
					double temp = 0;
					for (String hostname : resultDistributedSystemAna.getServer(t.getServerName()).hosts) {
						SoftServer softTemp = (SoftServer) resultDistributedSystemAna.getHost(hostname).getServer(t.getServerName());
						SoftServerAna stemp = resultDistributedSystemAna.getSoftServerAna(softTemp.getName(), hostname);
						temp += messageDelay(host.name, hostname, sizeOfMsg(prevtask.name, t.getName()))
								+ stemp.getCTask(t.getName(), ct.getScenarioName()).getResponseTime()
								+ stemp.getAvgWaitingTime()
								+ messageDelay(
										hostname,
										host.name,
										sizeOfMsg(getTaskFromReply(ct, prevtask.name, t.getName()), getTaskAfterReply(ct, prevtask.name, t.getName())));

					}
					temp /= resultDistributedSystemAna.getServer(t.getServerName()).hosts.size();
					ct_htime += temp;
				}
				prevtask = t;
			}
			ct.setHoldingTime(ct_htime);

			logger.debug("compnd task  " + ct.getName() + "compound task holding time = " + ct.getHoldingTime());
		}
	}

	/**
	 * This method computes the scenario and system level throughput and response times.
	 * This method is called after the whole solution finishes.
	 */
	void calculateScenarioMetrics() {
		for (Scenario scenario : resultDistributedSystemAna.scenarios) {
			Node nonUserRootNode = scenario.rootNode;
			if (scenario.rootNode.name.compareToIgnoreCase("user") == 0) {
				nonUserRootNode = scenario.rootNode.children.get(0);
			}
			double responseTimeIntoProbabilitySum = 0;
			double probabilitySum = 0;
			for (Host host : resultDistributedSystemAna.hosts) {
				for (SoftServer softserv : host.softServers) {
					SoftServerAna sa = resultDistributedSystemAna.getSoftServerAna(softserv.getName(), host.getName());
					for (CompoundTask compoundTask : sa.compundTasks) {
						//find the compound task that basically represents the scenario
						//then simply take its response time as scenario response time!
						if (compoundTask.subTasks.get(0).name.compareTo(nonUserRootNode.name) == 0 &&  
								compoundTask.getScenarioName().compareTo(scenario.getName()) == 0) {
							responseTimeIntoProbabilitySum += compoundTask.getResponseTime() * compoundTask.getProbability();
							probabilitySum += compoundTask.getProbability();
						}
					}
				}
			}
			Node sensibleRootNode;
			if (scenario.rootNode.servername.equals("user"))
				sensibleRootNode = scenario.rootNode.children.get(0);
			else
				sensibleRootNode = scenario.rootNode;

			//eqn (18)
			//scenario response time is average waiting time for server plus response time of the first compound task
			SoftServerAna rootServer = resultDistributedSystemAna.getSoftServerAna(sensibleRootNode.servername);
			scenario.setAverageResponseTime(responseTimeIntoProbabilitySum / probabilitySum + rootServer.getAvgWaitingTime());
			
			//scenario throughput: throughput of the softserver of scenario, fractioned by conditional probability of scenario
			//given the softserver is called
			CompoundTask scenarioCompoundTask = null;
			SoftServerAna scenarioSoftServerAna = null;
			for (Host host : resultDistributedSystemAna.hosts) {
				for (SoftServer softserv : host.softServers) {
					SoftServerAna sa = resultDistributedSystemAna.getSoftServerAna(softserv.getName(), host.getName());
					for (CompoundTask compoundTask : sa.compundTasks) {
						if (compoundTask.subTasks.get(0).name.compareTo(nonUserRootNode.name) == 0 &&  
								compoundTask.getScenarioName().compareTo(scenario.getName()) == 0) {
							scenarioCompoundTask = compoundTask;
							scenarioSoftServerAna = sa;
						}
					}
				}
			}
			
			double conditionalProbability = 0;
			for (Host host : resultDistributedSystemAna.hosts) {
				for (SoftServer softserv : host.softServers) {
					SoftServerAna sa = resultDistributedSystemAna.getSoftServerAna(softserv.getName(), host.getName());
					for (CompoundTask compoundTask : sa.compundTasks) {
						if (compoundTask.servername.equals(scenarioCompoundTask.servername)) {
							conditionalProbability += compoundTask.probability;
						}
					}
				}
			}
			
			scenario.avgThroughput.setValue(scenarioSoftServerAna.getThroughput() * scenarioCompoundTask.getProbability() / conditionalProbability);
		}
		double overallresp = 0;
		for (Scenario sce : resultDistributedSystemAna.scenarios) {
			overallresp += sce.getAverageResponseTime() * sce.getProbability();
		}
		resultDistributedSystemAna.overallRespTime.setValue(overallresp);
		
		double overallThroughput = 0;
		for (Scenario sce : resultDistributedSystemAna.scenarios) {
			overallThroughput += sce.avgThroughput.getValue();
		}
		resultDistributedSystemAna.overallThroughput.setValue(overallThroughput);
	}

	/* All the code shown below is written by niranjan */
	/*
	 * For Network Layer, Input to perfcenter should be the characteristics of Link should be defined link linkname lanname1 lanname2 trans
	 * (variablename|NUMBER) bps|Kbps|Mbps|Gbps mtu (variablename|NUMBER) bytes prop (variablename|NUMBER) ms|us|ns|ps headersize
	 * (variablename|NUMBER) bytes end for e.g: link lk1 lan1 lan2 trans 100 Mbps mtu 100 bytes prop 3 us headersize 100 bytes end For the analytical
	 * code purpose everything is converted to bits like mtu and headersize is converted to bits and trans is converted to bits/sec, time is converted
	 * to seconds,
	 */
	double sizeOfMsg(String task1_name, String task2_name) {
		double length_of_msg;
		for (Scenario sc : resultDistributedSystemAna.scenarios) {
			length_of_msg = sizeOfMsg(sc.rootNode, task1_name, task2_name);
			if (length_of_msg > 0) {
				return length_of_msg;
			}
			/*
			 * Since size of message during syncronous call is specified in Bytes, to convert to bits, length of message is multiplied by 8
			 */
		}
		return 0;
	}

	double sizeOfMsg(Node node, String task1_name, String task2_name) {

		double length_of_msg = 0;

		for (Node child : node.children) {
			if (node.name.equals(task1_name) && child.name.equals(task2_name)) {
				return child.pktsize.getValue() * 8;
				/*
				 * Since size of message during syncro8s specified in Bytes, to convert to bits, length of message is multiplied by 8
				 */

			}
			length_of_msg = sizeOfMsg(child, task1_name, task2_name);
			if (length_of_msg > 0) {
				return length_of_msg;
			}
		}

		return 0;
	}

	double getMtuBetweenLans(String lan1, String lan2) {
		for (LanLink link : resultDistributedSystemAna.links) {
			if (link.srclan.equals(lan1) && link.destlan.equals(lan2) || link.srclan.equals(lan2) && link.destlan.equals(lan1)) {
				return link.mtu.getValue() * 8;
				/*
				 * Since MTU of link is specified in Bytes, to convert to bits, mtu of message is multiplied by 8
				 */
			}
		}
		return 0;
	}

	double getPropBetweenLans(String lan1, String lan2) {
		for (LanLink link : resultDistributedSystemAna.links) {
			if (link.srclan.equals(lan1) && link.destlan.equals(lan2) || link.srclan.equals(lan2) && link.destlan.equals(lan1)) {
				if (link.propUnit.equalsIgnoreCase("ms")) {
					return link.prop.getValue() / 1000;
				} else if (link.propUnit.equalsIgnoreCase("us")) {
					return link.prop.getValue() / 1000000;
				} else if (link.propUnit.equalsIgnoreCase("ns")) {
					return link.prop.getValue() / 1000000000;
				} else if (link.propUnit.equalsIgnoreCase("ps")) {
					return link.prop.getValue() / 1000000000000.0;
				}
				/*
				 * Since MTU of link is specified in Bytes, to convert to bits, mtu of message is multiplied by 8
				 */
			}
		}
		return 0;
	}

	double getNumberOfPackets(String lan1, String lan2, double size) {
		double noofpkts = size / getMtuBetweenLans(lan1, lan2);
		LanLink lanlink = resultDistributedSystemAna.getLink(lan1, lan2);
		noofpkts = (int) (size / (getMtuBetweenLans(lan1, lan2) - lanlink.headerSize.getValue() * 8));
		if (size % (getMtuBetweenLans(lan1, lan2) - lanlink.headerSize.getValue() * 8) >= 0.0) {
			noofpkts = noofpkts + 1;
		}
		return noofpkts;
	}

	double getTransmissionRate(String lan1, String lan2) {
		for (LanLink link : resultDistributedSystemAna.links) {
			if (link.srclan.equals(lan1) && link.destlan.equals(lan2) || link.srclan.equals(lan2) && link.destlan.equals(lan1)) {
				if (link.transUnit.equalsIgnoreCase("bps")) {
					return link.trans.getValue();
				} else if (link.transUnit.equalsIgnoreCase("Kbps")) {
					return link.trans.getValue() * 1000;
				} else if (link.transUnit.equalsIgnoreCase("Mbps")) {
					return link.trans.getValue() * 1000000;
				} else if (link.transUnit.equalsIgnoreCase("Gbps")) {
					return link.trans.getValue() * 1000000000;
				}

			}
		}
		return 0;
	}

	double messageDelay(String host1, String host2, double length) {
		LanLink lanlink;
		Lan lan1, lan2;
		if (resultDistributedSystemAna.links.size() == 0) {
			return 0;
		} else if (resultDistributedSystemAna.getHost(host1).lan == null || resultDistributedSystemAna.getHost(host2).lan == null) {
			return 0;
		}
		lan1 = resultDistributedSystemAna.getLan(resultDistributedSystemAna.getHost(host1).lan);
		lan2 = resultDistributedSystemAna.getLan(resultDistributedSystemAna.getHost(host2).lan);
		int noofpkts;
		if (lan1.getName().equals(lan2.getName())) {
			return 0;
		} else {
			lanlink = resultDistributedSystemAna.getLink(lan1.getName(), lan2.getName());
			noofpkts = (int) (length / (getMtuBetweenLans(lan1.getName(), lan2.getName()) - lanlink.headerSize.getValue() * 8));
			if (length % (getMtuBetweenLans(lan1.getName(), lan2.getName()) - lanlink.headerSize.getValue() * 8) >= 0.0) {
				noofpkts = noofpkts + 1;
			}
			return (lanlink.getAvgWaitingTime(lan1.getName(), lan2.getName()) + getPropBetweenLans(lan1.getName(), lan2.getName()) + (length + (noofpkts
					* lanlink.headerSize.getValue() * 8))
					/ getTransmissionRate(lan1.getName(), lan2.getName()));
		}

	}

	boolean synTest(Node node, String task1_name, String task2_name) {
		for (Node child : node.children) {
			if (node.name.equals(task1_name) && child.name.equals(task2_name)) {
				return true;
			}
			return synTest(child, task1_name, task2_name);

		}
		return false;
	}

	void linkPerformanceParams() throws Exception {
		int inv;
		Host host1, host2;
		LanLink link;
		double aggrHoldingTime = 0;
		double aggrThinkTime = 0;
		double noUsers = 0;
		double noofpkts = 0;
		double totPktSize;
		double linkservicetime, linkthinktime;
		for (Lan lan1 : resultDistributedSystemAna.lans) {
			for (Lan lan2 : resultDistributedSystemAna.lans) {
				if (!lan1.getName().equalsIgnoreCase(lan2.getName())) {
					link = resultDistributedSystemAna.getLink(lan1.getName(), lan2.getName());
					;
					aggrHoldingTime = 0;
					aggrThinkTime = 0;
					noUsers = 0;
					for (String host1str : lan1.hosts) {
						host1 = resultDistributedSystemAna.getHost(host1str);
						for (Object softserv : host1.softServers) {
							for (CompoundTask ct : ((SoftServerAna) softserv).compundTasks) {
								inv = 0;
								totPktSize = 0;
								noofpkts = 0;

								for (SubTask task1 : ct.subTasks) {
									for (String host2str : lan2.hosts) {
										host2 = resultDistributedSystemAna.getHost(host2str);
										if (!host1.name.equalsIgnoreCase(host2.name)) {
											for (Object softserv2 : host2.softServers) {
												for (Task task2 : ((SoftServerAna) softserv2).simpleTasks) {
													if (!task1.name.equalsIgnoreCase(task2.name)) {
														if (synTest(resultDistributedSystemAna.getScenario(ct.getScenarioName()).rootNode, task1.name, task2.name)) {
															inv++;
															noofpkts = getNumberOfPackets(lan1.getName(), lan2.getName(),
																	sizeOfMsg(task1.getName(), task2.name));
															totPktSize += this.sizeOfMsg(resultDistributedSystemAna.getScenario(ct.getScenarioName()).rootNode,
																	task1.getName(), task2.name) + (noofpkts * link.headerSize.getValue() * 8);

														}
													}
												}

											}
										}
									}

								}
								linkservicetime = totPktSize / (inv * getTransmissionRate(lan1.getName(), lan2.getName()));

								linkthinktime = ((SoftServerAna) softserv).thrdCount.getValue()
										/ ((SoftServerAna) softserv).getThroughput()
										- totPktSize
										/ (getTransmissionRate(lan1.getName(), lan2.getName()))
										- inv
										* (link.getAvgWaitingTime(lan1.getName(), lan2.getName()) + getPropBetweenLans(lan1.getName(), lan2.getName()));
								linkthinktime = linkthinktime / inv;
								aggrHoldingTime += linkservicetime * ct.conditionalProbability * ((SoftServerAna) softserv).thrdCount.getValue();
								aggrThinkTime += linkthinktime * ct.conditionalProbability * ((SoftServerAna) softserv).thrdCount.getValue();
								noUsers += ct.conditionalProbability * ((SoftServerAna) softserv).thrdCount.getValue();
							}
						}

						String stationNames[] = new String[2];
						stationNames[0] = link.getName();
						stationNames[1] = "infinite-server";
						int stationTypes[] = new int[2];
						stationTypes[0] = 1;
						stationTypes[1] = 2;
						double[] visits = new double[2];
						double[][] serviceTimes = new double[2][1];

						serviceTimes[0][0] = aggrHoldingTime / noUsers;
						serviceTimes[1][0] = aggrThinkTime / noUsers;
						noUsers = Math.round(noUsers);

						visits[0] = 1;
						visits[1] = 1;
						Solver solver;
						SolverSingleClosedMVA closedsolver = new SolverSingleClosedMVA((int) noUsers, 2);
						logger.debug("link on LAN from " + lan1.getName() + " " + lan2.getName());
						logger.debug("input: service time: " + serviceTimes[0][0] + " Think time: " + serviceTimes[1][0] + " no of users: " + noUsers
								+ " no of servers: 1 ");
						if (!closedsolver.input(stationNames, stationTypes, serviceTimes, visits, (int) 1)) {
							throw new Exception("Error initializing MVASingleSolver", null);
						}
						solver = closedsolver;
						solver.solve();
						link.setThroughput(lan1.getName(), lan2.getName(), solver.getThroughput(0));
						link.setUtilization(lan1.getName(), lan2.getName(), solver.getUtilization(0));
						link.setAvgWaitingTime(lan1.getName(), lan2.getName(), solver.getResTime(0) - aggrHoldingTime / noUsers);
						logger.debug("output: Utilization:" + solver.getUtilization(0) + " response time: " + solver.getResTime(0) + " Throughput:"
								+ solver.getThroughput(0));
					}
				}
			}
		}
	}

	double getSumOfCompoundTaskThinktimeandResponseTime(String ctname) {
		for (Host host : resultDistributedSystemAna.hosts) {
			for (Object softserv : host.softServers) {
				for (CompoundTask ct : ((SoftServerAna) softserv).compundTasks) {
					if (ct.name.equalsIgnoreCase(ctname)) {
						return ct.responseTime + ct.thinkTime;
					}

				}
			}
		}
		return 0;
	}
}