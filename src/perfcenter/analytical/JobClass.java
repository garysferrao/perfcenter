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

public class JobClass {
	String name;
	double thinkTime = 0;
	double NoOfUsers = 0;
	double holdingTime = 0;
	double responseTime = 0;
	double probability = 0;
	String parentServer;
	String parentClass;

	class PairValue {
		double interTime = 0;
		double changeProb = 0;
		String jobClassName;
	}

	public ArrayList<PairValue> pairTasks = new ArrayList<PairValue>();
	public ArrayList<String> FollowingTasks = new ArrayList<String>();
	public ArrayList<String> ComposingTasks = new ArrayList<String>();

	public void setHoldingTime(double htime) {
		holdingTime = htime;
	}

	public double getHoldingTime() {
		return holdingTime;
	}

	public void setResponseTime(double rtime) {
		responseTime = rtime;
	}

	public double getResponseTime() {
		return responseTime;
	}

	public void setNoOfUsers(double nou) {
		NoOfUsers = nou;
	}

	public double getNoOfUsers() {
		return NoOfUsers;
	}

	public void setThinkTime(double ttime) {
		thinkTime = ttime;
	}

	public double getThinkTime() {
		return thinkTime;
	}

	public void setprobability(double p) {
		probability = p;
	}

	public double getprobability() {
		return probability;
	}

	public void setname(String nm) {
		name = nm;
	}

	public String getname() {
		return name;
	}

	public void setParentServer(String ps) {
		parentServer = ps;
	}

	public String getParentServer() {
		return parentServer;
	}

	public void setParentClass(String pc) {
		parentClass = pc;
	}

	public String getParentClass() {
		return parentClass;
	}
}