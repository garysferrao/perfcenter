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
package perfcenter.baseclass;

import perfcenter.simulator.SimulationParameters;

import java.util.ArrayList;

import java.util.HashMap;

import org.apache.log4j.Logger;

/**
 * This class holds a Scenario, as defined in the input file.
 * 
 * It also holds some basic performance metrics of the scenario, which can be filled
 * in after the system is solved.
 * 
 * @author akhila
 */
public class Scenario {

	public String name;

	public Variable scenarioProb;

	/** Pointer to root node of scenario */
	public TaskNode rootNode;

	//DOUBT: add power here?
	public Metric avgRespTime = new Metric();
	public Metric avgThroughput = new Metric();
	public Metric avgBadput = new Metric(); //rate at which requests timeout
	public Metric avgGoodput = new Metric();
	public Metric buffTimeout = new Metric();
	public Metric dropRate = new Metric();
	public Metric blockingProb = new Metric();

	public Metric arateToScenario = new Metric();

	/** Arrival rate during simulation is different than what is specified */
	public Metric arateToScenarioDuringSimulation = new Metric();
	
	private Logger logger = Logger.getLogger("Scenario");
	
	public boolean isValidated = false;
	/*Following variable will be filled by validate() method. 
	 *It stores index of tasknodes which are synchronous request-response. 
	 *Key is request and element of value is response from a different branch */
	HashMap<Integer, ArrayList<Integer> > syncpairs = new HashMap<Integer, ArrayList<Integer> >();

	public Scenario() {}

	public Scenario(String scename) {
		name = scename;
	}
	
	public void printFullInfo(){
       	logger.debug("Scenario Name:" + name);
       	logger.debug("Scenario Prob:" + getProbability());
        logger.debug("Tree");
        rootNode.printFullInfo();
	}
	
	public String toString(){
		StringBuilder builder = new StringBuilder("");
		builder.append("Scenario Name:" + name + "\n");
       	builder.append("Scenario Prob:" + getProbability() + "\n");
        builder.append("Tree" + "\n");
        builder.append(rootNode.toString() + "\n");
        return builder.toString();
	}

	public void setAverageResponseTime(double rtime) {
		avgRespTime.setValue(rtime);
	}

	public double getAverageResponseTime() {
		return avgRespTime.getValue();
	}

	public String getName() {
		return name;
	}

	public void setArateToScenario(int slot, double value) {
		arateToScenario.setValue(slot, value);
	}

	public double getArateToScenario() {
		return arateToScenario.getValue(SimulationParameters.getIntervalSlotCounter());
	}

	public double getProbability() {
		return scenarioProb.value;
	}

	public void setProbability(Variable val) {
		scenarioProb = val;
	}

	public void modifyProbability(double var1) {
		if (scenarioProb.name.compareToIgnoreCase("local") == 0) {
			scenarioProb.value = var1;
			return;
		}
		throw new Error("Attempt to modify the arrival rate of scenario " + name + " instead variable " + scenarioProb.name
				+ " should be modified");
	}

	/** finds arrival rate and probability for each node in the scenario */
	public void initialize() {
		// reset tree parameter
		initializeTree(rootNode);

		// set the arrival root of the root node
		//HACK: following field is used only for the analytical part, hence assigning arate of slot zero to it
		rootNode.arrate = arateToScenario.getValue(0);

		// find arrival rate to each of the nodes
		findArrivalRateTree(rootNode, rootNode.arrate, 1.0);
	}

	/** 
	 * isCT is reset for all the nodes in the tree (used for finding compound task).
	 */
	void initializeTree(TaskNode node) {
		node.isCT = false;
		for (TaskNode n : node.children) {
			initializeTree(n);
		}
	}
	
	
	/* This method mainly does synchronous call validation in scenario
	 * It feels up syncpairs member variable with pairs of tasknodes' indices carrying synchronous responses 
	 */
	public void validate() {
		/*First of all give index to each task node in scenario tree */
		ArrayList<TaskNode> level = new ArrayList<TaskNode>();
		int idx = 0;
		TaskNode root = rootNode;
		root.index = idx++;
		level.add(root);
		/* Synchronous Request Server Pair: To -> From */
		HashMap<String, String> syncReqServerPair = new HashMap<String, String>();
		HashMap<String, ArrayList<String> >  list = new HashMap<String, ArrayList<String> >();
		while(!level.isEmpty()){
			ArrayList<TaskNode> nextlevel = new ArrayList<TaskNode>();
			/* Traversing current level */
			for(int i=0;i<level.size();i++){
				TaskNode elem = level.get(i);
				logger.debug("Tasknode Name:" + elem.name + " server:" + elem.servername + " Idx:" + elem.index);
				if(elem.issync){
					syncReqServerPair.put(elem.servername, elem.parent.servername);
				}
				/* Processing children of each element in current level and adding them to next level */
				for(int j=0;j<elem.children.size();j++){
					TaskNode tempnode = elem.children.get(j);
					tempnode.index = idx++;
					nextlevel.add(tempnode);
					logger.debug(syncReqServerPair.get(elem.getServerName()) + " tempnode.servername:" + tempnode.servername);
					if(syncReqServerPair.containsKey(elem.getServerName()) && syncReqServerPair.get(elem.getServerName()) == tempnode.servername){
						if(list.containsKey(elem.servername)){
							list.get(elem.servername).add(tempnode.servername);
							syncpairs.get(elem.index).add(tempnode.index);
						}else{
							ArrayList<String> tmplist = new ArrayList<String>();
							tmplist.add(tempnode.servername);
							list.put(elem.servername, tmplist); 
							
							ArrayList<Integer> itmplist = new ArrayList<Integer>();
							itmplist.add(tempnode.index);
							syncpairs.put(elem.index, itmplist);
						}
					}
					
				}
			}
			level = nextlevel;
		}
		
		for(String toServerName : syncReqServerPair.keySet()){
			if(!list.containsKey(toServerName)){
				logger.warn("Warning:Scenario: There is no corresponding response to server \"" + toServerName + "\" from server \"" + syncReqServerPair.get(toServerName) + "\"");
			}
		}
	}

	/**
	 * Initializes the arrival rate and absolute probability of each node in the tree.<br/>
	 * <p>
	 * The values of arrival rate and probability flow down the tree in
	 * recursive manner. This function is used by analytical part.
	 */
	void findArrivalRateTree(TaskNode node, double arate, double parentprob) {
		if (node.isRoot == true) {
			node.arrate = arate * node.prob.getValue();
			node.prob.value = node.prob.value * parentprob;
			arate = node.arrate;
			parentprob = node.prob.getValue();
		} else {
			node.arrate = arate;
			node.prob.value = parentprob;
		}
		for (TaskNode childNode : node.children) {
			findArrivalRateTree(childNode, arate, parentprob);
		}
	}
}
