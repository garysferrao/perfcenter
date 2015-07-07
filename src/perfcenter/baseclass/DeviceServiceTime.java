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
 */package perfcenter.baseclass;

/**
 * Defines service times. This is used by task and virtual resource, and by analytical perfcenter.
 * 
 * Service time can be defined as a distribution in the class.
 * 
 * @author akhila
 */
public class DeviceServiceTime {
	// device name
	public String devName;

	// device distribution
	public Distribution dist;

	// used by analytical part
	private double responseTime;

	double holdingTime = 0;
	double thinkTime = 0;
	public double probabilty = 0;
	public double noOfUsers = 0;

	public DeviceServiceTime(String name) {
		devName = name;
	}

	public DeviceServiceTime(String name, Distribution dis) {
		devName = name;
		dist = dis;
	}

	// getter and setter methods
	public void setResponseTime(double rtime) {
		responseTime = rtime;
	}

	public double getResponseTime() {
		return responseTime;
	}

	public void setHoldingTime(double rtime) {
		holdingTime = rtime;
	}

	public double getHoldingTime() {
		return holdingTime;

	}

	public void setThinkTime(double rtime) {
		thinkTime = rtime;
	}

	public double getThinkTime() {
		return thinkTime;
	}

	public String getDeviceName() {
		return devName;
	}

	public Distribution getDistribution() {
		return dist;
	}

	public void print() {
		System.out.println(" Device " + devName + " ");
		System.out.println(" Distribution " + dist.name_ + "(" + dist.value1_.getName() + ":" + dist.value1_.getValue() + ")");
	}

	// makes a copy of self
	public DeviceServiceTime getCopy() {// made public by niranjan
		DeviceServiceTime dstcpy = new DeviceServiceTime(devName);
		dstcpy.dist = dist.getCopy();
		dstcpy.responseTime = this.responseTime;
		return dstcpy;
	}

	// used by analytical part
	public void initialize() {
		responseTime = 0;
	}
}
