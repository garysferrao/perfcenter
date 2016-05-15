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

import java.util.ArrayList;
import java.util.Random;

import org.apache.log4j.Logger;

import perfcenter.baseclass.enums.PowerManagementGovernor;
import perfcenter.baseclass.enums.SchedulingPolicy;
import perfcenter.baseclass.exception.DeviceNotFoundException;
import perfcenter.baseclass.DeviceCategory;

public class PhysicalMachine extends Machine {
	public boolean virtualizationEnabled = false; 
	public ArrayList<VirtualMachine> vms = new ArrayList<VirtualMachine>();
	
	public PhysicalMachine() {
		super();
	}

	public PhysicalMachine(String pmname, int num) {
		super(pmname, num);
	}
	
	public void addVM(VirtualMachine vm) {
		vms.add(vm);
	}
	
	public PhysicalMachine getCopy(String pmname, int num) throws DeviceNotFoundException {
		PhysicalMachine pmcpy = new PhysicalMachine(pmname, num);
		for (Device d : devices.values()) {
			Device dcpy = d.getCopy();
			pmcpy.addDevice(dcpy);
		}
		for (SoftResource sr : softResources.values()) {
			SoftResource srcpy = sr.getCopy();
			pmcpy.addSoftRes(srcpy);
		}
		for (SoftServer ss : softServers.values()) {
			SoftServer sscpy = ss.getCopy();
			pmcpy.addSoftServer(sscpy);
		}
		pmcpy.addLan(lan);
		pmcpy.virtualizationEnabled = virtualizationEnabled;
		return pmcpy;
	}
	
	public void addDeviceCount(String pdevname, Variable count) throws DeviceNotFoundException {
		if (isDeviceDeployed(pdevname) == false) {
			DeviceCategory devcat = ModelParameters.inputDistSys.getPDevice(pdevname).category;
			PhysicalDevice pdev = new PhysicalDevice(pdevname, devcat);
			devices.put(pdev.name, pdev);
		}
		getDevice(pdevname).count = count;
	}

	public void addDeviceBuffer(String pdevname, Variable buffersize) throws DeviceNotFoundException {
		if (isDeviceDeployed(pdevname) == false) {
			//System.out.println(name + " Device Created in addDeviceBuffer");
			DeviceCategory devcat = ModelParameters.inputDistSys.getPDevice(pdevname).category;
			PhysicalDevice pdev = new PhysicalDevice(pdevname, devcat);
			devices.put(pdev.name, pdev);
		}
		getDevice(pdevname).buffer = buffersize;
		return;
	}

	public void addDeviceSpeedUp(String pdevname, Variable pspeed) throws DeviceNotFoundException {
		if (isDeviceDeployed(pdevname) == false) {
			//System.out.println(name + " Device Created in addDeviceSpeedUp");
			DeviceCategory devcat = ModelParameters.inputDistSys.getPDevice(pdevname).category;
			PhysicalDevice pdev = new PhysicalDevice(pdevname, devcat);
			devices.put(pdev.name, pdev);
		}
		getDevice(pdevname).speedUpFactor = pspeed;
	}

	public void addDeviceSchedPol(String pdevname, SchedulingPolicy pol) throws DeviceNotFoundException {
		if (isDeviceDeployed(pdevname) == false) {
			//System.out.println(name + " Device Created in addDeviceSchedPol");
			DeviceCategory devcat = ModelParameters.inputDistSys.getPDevice(pdevname).category;
			PhysicalDevice pdev = new PhysicalDevice(pdevname, devcat);
			devices.put(pdev.name, pdev);
		}
		getDevice(pdevname).schedulingPolicy = pol;
	}
	
	public void addDeviceBaseSpeed(String pdevname, Variable basespeed) throws DeviceNotFoundException {
		if (isDeviceDeployed(pdevname) == false) {
			//System.out.println(name + " Device Created in addDeviceSpeedUp");
			DeviceCategory devcat = ModelParameters.inputDistSys.getPDevice(pdevname).category;
			PhysicalDevice pdev = new PhysicalDevice(pdevname, devcat, basespeed.value);
			devices.put(pdev.name, pdev);
		}
		getDevice(pdevname).basespeed.value = basespeed.value;
	}
}
