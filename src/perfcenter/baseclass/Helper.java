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

/**
 * This class has static helper functions called by perfcenter.
 * @author  akhila
 */
public class Helper {

	/**
	 *  Converts ms, us, ns, ps to seconds.
	 */
	public static double convertToSeconds(double val, String units) {

		if (units.compareTo("ms") == 0)
			return val * Math.pow(10, -3);
		else if (units.compareTo("us") == 0)
			return val * Math.pow(10, -6);
		else if (units.compareTo("ns") == 0)
			return val * Math.pow(10, -9);
		else if (units.compareTo("ps") == 0)
			return val * Math.pow(10, -12);

		throw new Error("\"" + units + "\" is not valid unit");

	}

	/**
	 * converts Mbps, Kbps and Gbps to bps
	 */
	public static double convertTobps(double val, String units) {
		if (units.compareTo("bps") == 0)
			return val;
		else if (units.compareTo("Mbps") == 0)
			return (val) * Math.pow(10, 6);
		else if (units.compareTo("Kbps") == 0)
			return (val) * Math.pow(10, 3);
		else if (units.compareTo("Gbps") == 0)
			return (val) * Math.pow(10, 9);

		throw new Error("\"" + units + "\" is not valid unit");
	}
}
