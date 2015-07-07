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
import java.util.Collections;

import org.apache.log4j.Logger;

import perfcenter.baseclass.ModelParameters;
import perfcenter.baseclass.Node;
import perfcenter.baseclass.Scenario;
import perfcenter.baseclass.SoftServer;
import perfcenter.baseclass.Task;
import perfcenter.baseclass.enums.SystemType;

/**
 * SoftServer Analytical is inherited from softserver. SoftServerAna has compound tasks.
 * 
 * @author akhila
 */
public class SoftServerAna extends SoftServer {

	static DistributedSystemAna ds;
	String hostName;
	double holdingTime;
	double thinkTime;
	double noOfUsers;
	boolean nodeAdded = false;
	Logger logger = Logger.getLogger("SoftServerAna");
	public ArrayList<CompoundTask> compundTasks = new ArrayList<CompoundTask>();
	public ArrayList<CallingServer> callingServers = new ArrayList<CallingServer>();

	public SoftServerAna(SoftServer s) {
		name = s.name;
		thrdCount = s.thrdCount;
		thrdBuffer = s.thrdBuffer;
		schedp = s.schedp;
		simpleTasks = s.simpleTasks;
		resourceQueue.initialize();
		hosts = s.hosts;
		for (Task t : simpleTasks) {
			t.initialize();
		}
	}

	// Added by Mayur
	// To know which instance of Software Server is deployed on which Host
	public SoftServerAna(SoftServer s, String hName) {
		name = s.name;
		thrdCount = s.thrdCount;
		thrdBuffer = s.thrdBuffer;
		schedp = s.schedp;
		simpleTasks = s.simpleTasks;
		resourceQueue.initialize();
		hosts = s.hosts;
		for (Task t : simpleTasks) {
			t.initialize();
		}

		hostName = hName; // Mayur
	}

	public class CallingServer {
		String servername;
		int users;
		double thinkTime;

		public double getThinkTime() {
			return thinkTime;
		}

		public void setThinkTime(double thinkTime) {
			this.thinkTime = thinkTime;
		}

		public void setUsers(int users) {
			this.users = users;
		}

		public String getHostName() {
			return hostName;
		}

		public String getServername() {
			return servername;
		}

		String hostName;
		double callProb;

		public void setCallProb(double callProb) {
			this.callProb = callProb;
		}

		public CallingServer(String name, String hName, double prob) {
			servername = name;
			hostName = hName;
			callProb = prob;
		}

		public CallingServer(String name, String hName) {
			servername = name;
			hostName = hName;
		}

	}

	public double getCallingServerProb(String name) {
		for (CallingServer cs : callingServers) {
			if (name == cs.servername) {
				return cs.callProb;
			}
		}
		return 0;
	}

	public SoftServerAna(String name) {
		super(name);
	}

	public void setHoldingTime(double htime) {
		holdingTime = htime;
	}

	public double getHoldingTime() {
		return holdingTime;
	}

	public String getHostName() {
		return hostName;
	}

	public CompoundTask getCTask(String name, String Scename, String classname) {
		for (CompoundTask task : compundTasks) {
			if ((task.subTasks.get(0).name.compareToIgnoreCase(name) == 0) && (task.sceName.compareTo(Scename) == 0)
					&& (task.ClassName.compareTo(classname) == 0)) {
				return task;
			}
			if ((task.name.compareToIgnoreCase(name) == 0) && (task.sceName.compareTo(Scename) == 0) && (task.ClassName.compareTo(classname) == 0)) {
				return task;
			}
		}
		throw new Error(name + " is not Compound Task of Scenario" + Scename);
	}

	public CompoundTask getCTask(String taskName, String scenarioName) {
		for (CompoundTask compoundTask : compundTasks) {
//			if (compoundTask.name.compareTo(taskName) == 0 && compoundTask.sceName.compareTo(scenarioName) == 0) {
			//messes up numInvocations calculation somehow. understand how.
			if (compoundTask.name.startsWith(taskName) && compoundTask.sceName.compareTo(scenarioName) == 0) { //RIGHTNOW: XXX: WORKS: changed here, final verification pending
				return compoundTask;
			}
		}
		for (CompoundTask task : compundTasks) {
			System.err.println(task.name);
		}
		
		throw new Error(taskName + " is not Compound Task of Scenario" + scenarioName);
	}
	
	/*public CompoundTask getCTask(String name, String Scename) {
		for (CompoundTask task : compundTasks) {
			if (task.name.compareTo(name) == 0 && task.sceName.compareTo(Scename) == 0) {
				return task;
			}
		}
		for (CompoundTask task : compundTasks) {
			System.err.println(task.name);
		}
		
		throw new Error(name + " is not Compound Task of Scenario" + Scename);
	}*/

	/**
	 * This starts the dfs traversal of the tree, For the first run sync is set to false and startnode is set to null
	 * 
	 * @param rootNode
	 *            is the root of the tree
	 * @param scenarioName
	 *            scenarioname
	 */
	public void createCompoundTaskObjectsForGivenSoftServer(Node rootNode, String scenarioName) {
		dfs(rootNode, name, false, null, scenarioName);
	}

	/**
	 * This function does dfs traversal of tree. In the process finds the compound tasks.
	 * 
	 * @param node
	 *            start node of the tree
	 * @param servername
	 *            server name for which compound task is being found
	 * @param sync
	 *            is set to true if dfs traveral is to find subsequent subtasks of CT
	 * @param startNode
	 *            is set to the node, for which compoundtask is being found
	 * @param scename
	 *            scenarioname of the tree
	 */
	int checkCTPresence(String ctname) {
		int count = 0;
		for (CompoundTask ct : compundTasks) {
			if (ct.subTasks.get(0).name.equals(ctname)) {
				count++;
			}
		}
		/*
		 * if(count==0) System.out.println("compound task "+ctname+" not present"); else
		 * System.out.println("already "+count+" compound tasks of name "+ctname+" is present");
		 */
		return count;
	}

	int TaskPresenceInCTs(String ctname) {
		int count = 0;
		for (CompoundTask ct : compundTasks) {
			if (ct.getParentServer().equalsIgnoreCase("user")) // tbd: if this is sufficient to check if this case is sufficient
			{
				for (CompoundTask.SubTask t : ct.subTasks) {
					if (t.name.compareToIgnoreCase(ctname) == 0) {
						count++;
					}
				}
			}

		}
		// System.out.println("compound task "+ctname+" not present");
		return count;
	}

	/* modified by niranjan to add different names for Compund task belonging to different compound task */
	void dfs(Node node, String serverName, boolean sync, Node startNode, String scenarioName) {
		// when encountered leaf node
		if (node.children == null || node.children.isEmpty() == true) {
			if (node.servername.compareToIgnoreCase(serverName) == 0) {
				if (sync == true) {
					compundTasks.add(makeCompoundTask(node, startNode, scenarioName, serverName));
					node.isCT = true;
				} else if (node.isCT == false) {
					// this is the case where compound task has single subtask
					compundTasks.add(makeCompoundTask(node, node, scenarioName, serverName));
					if (hosts.size() == 1) {
						node.isCT = true;
					} else {
						if (hostName.equals(hosts.get(hosts.size() - 1))) {
							node.isCT = true;
						}
					}

				}
			}
		}
		for (Node child : node.children) {

			if (node.servername.compareToIgnoreCase(serverName) == 0) {
				if (child.issync == true) {
					sync = true;

					// this the the first node of the compound task
					if (startNode == null) {
						startNode = node;
					}

					// name of the compound task this node is part of
					node.belongsToCT = startNode.name;
				} else if (sync == true) {
					// this is the case we have encountered the end of sync
					// call.
					compundTasks.add(makeCompoundTask(node, startNode, scenarioName, serverName));
				} else if (node.isCT == false) {
					// this is the case where compound task has single subtask
					compundTasks.add(makeCompoundTask(node, node, scenarioName, serverName));
					if (hosts.size() == 1) {
						node.isCT = true;
					} else {
						if (hostName.equals(hosts.get(hosts.size() - 1))) {
							node.isCT = true;
						}
					}
				}
			}
			// continue the traversal
			dfs(child, serverName, sync, startNode, scenarioName);
		}
	}

	/**
	 * This function calls makeCompoundTaskRec(compound task is created). Reverses the list of compound task, sets the compountask name and arrival
	 * rate and other params
	 * 
	 * @param endNode
	 * @param startNode
	 * @param scename
	 * @return created compoundtask
	 */
	CompoundTask makeCompoundTask(Node endNode, Node startNode, String scename, String serverName) {
		if (ModelParameters.getSystemType() == SystemType.CLOSED) {
			Scenario sce = ModelParameters.inputDistributedSystem.getScenario(scename);
			CompoundTask ct = new CompoundTask();
			// compound task is created.
			makeCompoundTaskRec(endNode, startNode, ct, serverName);
			Collections.reverse(ct.subTasks);

			// compound task paramters are set
			// added by niranjan
			// System.out.println("count of presence of task "+ct.tasks.get(0).name+" is "+TaskPresenceInCTs(ct.tasks.get(0).name));
			// ct.name=ct.tasks.get(0).name;//modified this to
			// added by niranjan
			if (startNode.parent == null) {
				ct.parentCTTask = "user";
				ct.parentServer = "user";
			} else {
				ct.parentServer = startNode.parent.servername;
				ct.parentCTTask = startNode.parent.belongsToCT;
				// ct.probability=getCTask(ct.parentCTTask,scename).probability;
			}
			ct.name = ct.subTasks.get(0).name + "_" + (checkCTPresence(ct.subTasks.get(0).name) + 1); // CT name is name of first task
			ct.probability = ct.subTasks.get(ct.subTasks.size() - 1).probability;
			ct.probability *= sce.getProbability();
			ct.ClassName = ct.name;

			// niranjan
			ct.sceName = scename;

			// arr rate is equal to the arr rate of last task
			ct.arrivalRate = ct.subTasks.get(ct.subTasks.size() - 1).arrivalRate;

			ct.jobClassSet = false;
			ct.servername = getName();

			// prob is equal to the prob of last task

			// to set parentTask and parentserver
			/*
			 * if (startNode.parent == null) { ct.parentCTTask = "user"; ct.parentServer = "user"; } else { ct.parentServer =
			 * startNode.parent.servername; ct.parentCTTask = startNode.parent.belongsToCT; }
			 */
			return ct;

		} else {
			Scenario sce = ModelParameters.inputDistributedSystem.getScenario(scename);
			CompoundTask ct = new CompoundTask();
			// compound task is created.
			makeCompoundTaskRec(endNode, startNode, ct, serverName);
			Collections.reverse(ct.subTasks);

			// compound task paramters are set
			ct.name = ct.subTasks.get(0).name; // CT name is name of first task
			ct.sceName = scename;

			// arr rate is equal to the arr rate of last task
			ct.arrivalRate = ct.subTasks.get(ct.subTasks.size() - 1).arrivalRate;

			// prob is equal to the prob of last task
			ct.probability = ct.subTasks.get(ct.subTasks.size() - 1).probability;
			ct.probability *= sce.getProbability();

			// to set parentTask and parentserver
			if (startNode.parent == null) {
				ct.parentCTTask = "user";
				ct.parentServer = "user";
			} else {
				ct.parentServer = startNode.parent.servername;
				ct.parentCTTask = startNode.parent.belongsToCT;
			}
			return ct;
		}

	}

	/**
	 * This function does reverse traversal of tree from endnode up to startnode and creates the compound task
	 * 
	 * @param endNode
	 * @param startNode
	 * @param ct
	 *            has the resultant compound task
	 */
	void makeCompoundTaskRec(Node endNode, Node startNode, CompoundTask ct, String serverName) {
		if (endNode.servername.compareToIgnoreCase(serverName) == 0 || endNode.parent.servername.compareToIgnoreCase(serverName) == 0) {
			ct.addSubTask(endNode);
		}
		if ((endNode.parent == null) || (endNode == startNode)) {
			return;
		} else {
			makeCompoundTaskRec(endNode.parent, startNode, ct, serverName);
		}
	}

	@Override
	public void print() {
		super.print();
		for (CompoundTask ct : compundTasks) {
			ct.print();
		}
	}

	void solveAggrMVA() throws Exception {
		if (compundTasks.size() == 0) {
			return;
		}
		String stationNames[] = new String[2];
		stationNames[0] = name;
		stationNames[1] = "infinite-server";

		int stationTypes[] = new int[2];
		stationTypes[0] = 1;
		stationTypes[1] = 2;
		double[] visits = new double[2];
		double[][] serviceTimes = new double[2][1];
		double aggrHoldingTime = 0;
		double aggrThinkTime = 0;
		double noUsers;

		aggrThinkTime = thinkTime;
		noUsers = noOfUsers;
		aggrHoldingTime = getHoldingTime();

		serviceTimes[0][0] = aggrHoldingTime;
		serviceTimes[1][0] = aggrThinkTime;

		noUsers = Math.round(noUsers);

		visits[0] = 1;
		visits[1] = 1;
		Solver solver;
		logger.debug("soft server:" + getName());
		logger.debug("input: service time: " + serviceTimes[0][0] + " Think time: " + serviceTimes[1][0] + " no of users: " + noUsers
				+ " no of servers " + thrdCount.getValue());

		SolverSingleClosedMVA closedsolver = new SolverSingleClosedMVA((int) noUsers, 2);
		if (!closedsolver.input(stationNames, stationTypes, serviceTimes, visits, (int) thrdCount.getValue())) {
			throw new Exception("Error initializing MVASingleSolver", null);
		}

		solver = closedsolver;
		solver.solve();

		/*
		 * Below line is causing a problem in compound tasks response time setting. The values are not as expected.
		 */
		/*
		 * for (CompoundTask ct : ctasks) { ct.setResponseTime(solver.getResTime(0) - aggrHoldingTime / noUsers + ct.getHoldingTime()); }
		 */

		setThroughput(solver.getThroughput(0));
		setUtilization(solver.getUtilization(0) / thrdCount.getValue());
		setAverageResponseTime(solver.getResTime(0));
		setAvgServiceTime(aggrHoldingTime / noUsers);
		/*
		 * System.out.println(" "+solver.getResTime(0)); System.out.println("Utilization: server:" + getName() + "  " + getUtilization()+
		 * "Throughput:" + solver.getThroughput(0));
		 */
		logger.debug("output: utilization " + getUtilization() + " response time: " + solver.getResTime(0) + " Throughput:" + solver.getThroughput(0));
	}

	/* Supriya: function implements a multi-class MVA solver */
	public void solveMultiMVA() throws Exception {
		String stationNames[] = new String[2];
		stationNames[0] = name;
		stationNames[1] = "infinite-server";

		int stationTypes[] = new int[2];
		stationTypes[0] = 1;
		stationTypes[1] = 2;

		if (compundTasks.size() > 1) {

			double[][] visits = new double[2][compundTasks.size()];
			double[][][] serviceTimes = new double[2][compundTasks.size()][1];
			/* 3D array for load dependent server */
			int[] classPop = new int[compundTasks.size()];
			/* array to store number of users for every class */

			for (int i = 0; i < 2; i++) {
				for (int j = 0; j < compundTasks.size(); j++) {
					classPop[j] = compundTasks.get(j).getNoOfUsers();
					if (i == 0) {
						serviceTimes[i][j][0] = compundTasks.get(j).getHoldingTime();
						visits[i][j] = 0.5;
					} else {
						serviceTimes[i][j][0] = compundTasks.get(j).getThinkTime();
						visits[i][j] = 1;
					}
					// tbd initialize visits

				}
			}
			// visits[0][0]=0.9;
			// visits[0][1]=0.1;
			int[] servercopies = new int[2];
			servercopies[0] = (int) thrdCount.getValue();
			servercopies[1] = 1;
			SolverMulti solver;
			SolverMultiClosedMVA closedsolver = new SolverMultiClosedMVA(compundTasks.size(), 2);
			if (!closedsolver.input(stationNames, stationTypes, serviceTimes, visits, classPop, servercopies)) {
				throw new Exception("Error initializing MVAMultiSolver", null);
			}
			solver = closedsolver;
			solver.solve();
			for (int j = 0; j < compundTasks.size(); j++) {
				compundTasks.get(j).setResponseTime(solver.getResTime(0, j) / 0.5);
				compundTasks.get(j).setThroughput(solver.getClsThroughput(j));
				/*
				 * System.out.println("Resp Time : server:" + getName() + "ctask " + ctasks.get(j).getName() + ": " + solver.getResTime(0, j)/0.5);
				 */
			}
			// System.out.println("Resp Time : ");
			setThroughput(solver.getAggrThroughput(0));
			setAvgQueueLength(solver.getAggrQueueLen(0));
			setAverageResponseTime(solver.getAggrResTime(0));
			setUtilization(solver.getAggrUtilization(0));
			/*
			 * System.out.println("Utilization: server:" + getName() + "  " + solver.getAggrUtilization(0)); System.out.println("Throughput: server:"
			 * + getName() + "  " + solver.getAggrThroughput(0));
			 */
		} else {
			double[] visits = new double[2];
			double[][] serviceTimes = new double[2][1];
			serviceTimes[0][0] = compundTasks.get(0).getHoldingTime();
			serviceTimes[1][0] = compundTasks.get(0).getThinkTime();

			visits[0] = 1;
			visits[1] = 1;
			Solver solver;
			SolverSingleClosedMVA closedsolver = new SolverSingleClosedMVA(compundTasks.get(0).getNoOfUsers(), 2);
			if (!closedsolver.input(stationNames, stationTypes, serviceTimes, visits)) {
				throw new Exception("Error initializing MVASingleSolver", null);
			}
			solver = closedsolver;
			solver.solve();

			compundTasks.get(0).setResponseTime(solver.getResTime(0));
			setThroughput(solver.getTotThroughput());

		}

	}
}