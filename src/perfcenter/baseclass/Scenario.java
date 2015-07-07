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

	protected String scenarioName;

	public Variable scenarioProbability;

	/** Pointer to root node of scenario */
	public Node rootNodeOfScenario;

	//DOUBT: add power here?
	public Metric averageResponseTime = new Metric();
	public Metric averageThroughput = new Metric();
	public Metric averageBadput = new Metric(); //rate at which requests timeout
	public Metric averageGoodput = new Metric();
	public Metric buffTimeout = new Metric();
	public Metric dropRate = new Metric();
	public Metric blockingProb = new Metric();

	public Metric arateToScenario = new Metric();

	/** Arrival rate during simulation is different than what is specified */
	public Metric arateToScenarioDuringSimulation = new Metric();

	public Scenario() {}

	public Scenario(String scename) {
		scenarioName = scename;
	}

	public void setAverageResponseTime(double rtime) {
		averageResponseTime.setValue(rtime);
	}

	public double getAverageResponseTime() {
		return averageResponseTime.getValue();
	}

	public String getName() {
		return scenarioName;
	}

	public void setArateToScenario(int slot, double value) {
		arateToScenario.setValue(slot, value);
	}

	public double getArateToScenario() {
		return arateToScenario.getValue(SimulationParameters.getIntervalSlotCounter());
	}

	public double getProbability() {
		return scenarioProbability.value;
	}

	public void setProbability(Variable val) {
		scenarioProbability = val;
	}

	/** prints the tree present in scenario */
	public void printTree(Node node) {
		node.print();
		for (Node n : node.children) {
			printTree(n);
		}
	}

	public void modifyProbability(double var1) {
		if (scenarioProbability.name.compareToIgnoreCase("local") == 0) {
			scenarioProbability.value = var1;
			return;
		}
		throw new Error("Attempt to modify the arrival rate of scenario " + scenarioName + " instead variable " + scenarioProbability.name
				+ " should be modified");
	}

	/** finds arrival rate and probability for each node in the scenario */
	public void initialize() {
		// reset tree parameter
		initializeTree(rootNodeOfScenario);

		// set the arrival root of the root node
		//HACK: following field is used only for the analytical part, hence assigning arate of slot zero to it
		rootNodeOfScenario.arrate = arateToScenario.getValue(0);

		// find arrival rate to each of the nodes
		findArrivalRateTree(rootNodeOfScenario, rootNodeOfScenario.arrate, 1.0);
	}

	/** 
	 * isCT is reset for all the nodes in the tree (used for finding compound task).
	 */
	void initializeTree(Node node) {
		node.isCT = false;
		for (Node n : node.children) {
			initializeTree(n);
		}
	}

	/**
	 * Initializes the arrival rate and absolute probability of each node in the tree.<br/>
	 * <p>
	 * The values of arrival rate and probability flow down the tree in
	 * recursive manner. This function is used by analytical part.
	 */
	void findArrivalRateTree(Node node, double arate, double parentprob) {
		if (node.isRoot == true) {
			node.arrate = arate * node.prob.getValue();
			node.prob.value = node.prob.value * parentprob;
			arate = node.arrate;
			parentprob = node.prob.getValue();
		} else {
			node.arrate = arate;
			node.prob.value = parentprob;
		}
		for (Node n : node.children) {
			findArrivalRateTree(n, arate, parentprob);
		}
	}
}
