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
package perfcenter.output;

import org.apache.log4j.Logger;

import perfcenter.baseclass.Device;
import perfcenter.baseclass.Machine;

/**
 * This is implemented to sort resources based on utilization. Used in output.java to find bottleneck resource.
 * 
 * @author akhila
 * 
 */
public class BottleNeck implements Comparable<BottleNeck> {
	String resname;
	Double util;
	Logger logger = Logger.getLogger("BottleNeck");
	Machine host = null;
	Device device = null;

	public BottleNeck(Double u, String name) {
		util = u;
		resname = name;
	}

	BottleNeck(double u, Machine h, Device d) {
		util = u;
		resname = h.name + ":" + ((Device) d).getDeviceName();
		this.host = h;
		this.device = d;
	}

	/* Comparison is based on utilization */
	public int compareTo(BottleNeck anotherRes) throws ClassCastException {
		if (!(anotherRes instanceof BottleNeck))
			throw new ClassCastException("Bottleneck object expected.");
		double anotherUtil = ((BottleNeck) anotherRes).util;
		if (this.util > anotherUtil)
			return -1;
		else if (this.util < anotherUtil)
			return 1;
		else
			return 0;
	}

	void print() {
		System.out.print(resname + " = " + util);
	}

	// returns utilization (of a device, s/w server, virt-res etc) added by rakesh
	public double getUtil() {
		return this.util;
	}

}
