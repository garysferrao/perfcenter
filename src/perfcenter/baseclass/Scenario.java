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

/**
 * This class holds a Scenario, as defined in the input file.
 * 
 * It also holds some basic performance metrics of the scenario, which can be filled
 * in after the system is solved.
 * 
 * @author akhila
 */
public class Scenario {

	protected String name;

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

	public Scenario() {}

	public Scenario(String scename) {
		name = scename;
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

	/** prints the tree present in scenario */
	public void printTree(TaskNode node) {
		node.print();
		for (TaskNode n : node.children) {
			printTree(n);
		}
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
		for (TaskNode n : node.children) {
			findArrivalRateTree(n, arate, parentprob);
		}
	}
}
