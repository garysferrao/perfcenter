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
 import org.apache.log4j.Logger;

import perfcenter.baseclass.enums.DeviceType;
/**
 * Defines service times. This is used by task and virtual resource, and by analytical perfcenter.
 * 
 * Service time can be defined as a distribution in the class.
 * 
 * @author akhila
 */
public class ServiceTime {
	// Device category
	public DeviceCategory devCategory;
	// device distribution
	public Distribution dist;
	public double basespeed;
	
	private Logger logger = Logger.getLogger("ServiceTime");

	// used by analytical part
	private double responseTime;

	double holdingTime = 0;
	double thinkTime = 0;
	public double probabilty = 0;
	public double noOfUsers = 0;

	public ServiceTime(DeviceCategory _devCategory) {
		devCategory = _devCategory;
	}

	public ServiceTime(DeviceCategory _devCategory, Distribution _dist) {
		devCategory = _devCategory;
		dist = _dist;
	}
	
	public ServiceTime(DeviceCategory _devCategory, Distribution _dist, double _basespeed) {
		devCategory = _devCategory;
		dist = _dist;
		basespeed = _basespeed;
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

	public DeviceCategory getDeviceCategory() {
		return devCategory;
	}

	public Distribution getDistribution() {
		return dist;
	}

	public String toString() {
		return devCategory.name + " servt " + dist.name_ + "(" + dist.value1_.getName() + ":" + dist.value1_.getValue() + ") at " + basespeed + "\n";
	}

	// makes a copy of self
	public ServiceTime getCopy() {// made public by niranjan
		ServiceTime stcpy = new ServiceTime(devCategory, dist, basespeed);
		stcpy.devCategory = devCategory.getCopy();
		stcpy.dist = dist.getCopy();
		stcpy.responseTime = this.responseTime;
		return stcpy;
	}

	// used by analytical part
	public void initialize() {
		responseTime = 0;
	}
}
