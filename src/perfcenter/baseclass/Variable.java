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

import org.apache.log4j.Logger;

/**
 * Stores the variable that is defined in the input file
 * 
 * @author akhila
 */
public class Variable {

	public String name;
	public double value = 0;
	boolean isUsed;
	Logger logger = Logger.getLogger("Variable");

	public Variable(String varname, double varval) {
		name = varname;
		value = varval;
	}

	public String getName() {
		return name;
	}

	public double getValue() {
		return value;
	}

	public void setValue(double val) {
		value = val;
	}

	public String toString() {
		return name + ":" + value;
	}

	public void updateUsedInfo() {
		isUsed = true;
	}

	public void validate() {
		if (isUsed == false) {
			logger.warn("Warning:Variable \"" + name + "\" is not used");
		}
	}
}
