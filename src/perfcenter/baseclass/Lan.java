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

import java.util.ArrayList;

import org.apache.log4j.Logger;

/**
 * Defines LAN. Lan can have hosts deployed on to them. Lan can also have connections to other lans
 * 
 * @author akhila
 */
public class Lan {
	String name;
	private Logger logger = Logger.getLogger("Lan");
	
	/** list of hosts deployed on this lan */
	public ArrayList<String> hosts = new ArrayList<String>();
	
	/** list of lans that are connected to this lan */
	private ArrayList<String> lans = new ArrayList<String>();

	public Lan(String lanname) {
		name = lanname;
	}

	public String getName() {
		return name;
	}

	public void addConnectedLan(String lan) {
		lans.add(lan);
	}

	public void addMachine(String name) {
		hosts.add(name);
	}

	public void removeMachine(String name) {
		hosts.remove(name);
	}

	// Prints the lan parameters
	public void print() {
		System.out.println("Lan:" + name);
		for (String host : hosts) {
			System.out.println(" Host:" + host);
		}
		for (String l : lans) {
			System.out.println(" Connected to:" + l);
		}

	}

	/** validate lan parameters */
	public void validate() {
		if (hosts.isEmpty())
			logger.warn("Warning:Lan \"" + name + "\" Does not have any hosts ");
		if (lans.isEmpty())
			logger.warn("Warning:Lan \"" + name + "\" is not connected to other lans ");
	}
	
	public Lan getCopy(){
		Lan lcpy = new Lan(name);
		lcpy.hosts = new ArrayList<String>(this.hosts);
		lcpy.lans = new ArrayList<String>(this.lans);
		return lcpy;
	}
	
}
