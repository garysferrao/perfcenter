/*
 * Copyright (C) 2015-  by Varsha Apte - <varsha@cse.iitb.ac.in>, et al.
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

import java.util.HashMap;

import org.apache.log4j.Logger;

import perfcenter.baseclass.enums.SchedulingPolicy;
import perfcenter.baseclass.exception.DeviceNotFoundException;
import perfcenter.baseclass.DeviceCategory;

/**
 * This represents the virtual machine definition in the input file.
 * 
 * A virtual machine can have software servers deployed onto them. Virtual Machine has set of virtual devices and an machine is
 * deployed on to a single LAN. 
 * 
 */
public class VirtualMachine extends Machine{
	public Machine host = null;
	public boolean virtualizationEnabled ; 
	public HashMap<String, VirtualMachine> vms;
	public VirtualMachine() {
		super();
		virtualizationEnabled = false; 
		vms = new HashMap<String, VirtualMachine>();
	}

	public VirtualMachine(String hname, int num) {
		super(hname, num);
		virtualizationEnabled = false; 
		vms = new HashMap<String, VirtualMachine>();
	}
	
	public void addVM(VirtualMachine vm) {
		vms.put(vm.name, vm);
	}
	
	public void removeVM(VirtualMachine vm) {
		vms.remove(vm.name);
	}
	
	public void addDeviceCount(String vdevname, Variable count) throws DeviceNotFoundException {
		if (isDeviceDeployed(vdevname) == false) {
			DeviceCategory devcat = ModelParameters.inputDistSys.getVDevice(vdevname).category;
			VirtualDevice vdev = new VirtualDevice(vdevname, devcat);
			devices.put(vdev.name, vdev);
		}
		getDevice(vdevname).count = count;
	}

	public void addDeviceBuffer(String vdevname, Variable buffersize) throws DeviceNotFoundException {
		if (isDeviceDeployed(vdevname) == false) {
			//System.out.println(name + " Device Created in addDeviceBuffer");
			DeviceCategory devcat = ModelParameters.inputDistSys.getVDevice(vdevname).category;
			VirtualDevice vdev = new VirtualDevice(vdevname, devcat);
			devices.put(vdev.name, vdev);
		}
		getDevice(vdevname).buffer = buffersize;
		return;
	}

	public void addDeviceSpeedUp(String vdevname, Variable pspeed) throws DeviceNotFoundException {
		if (isDeviceDeployed(vdevname) == false) {
			//System.out.println(name + " Device Created in addDeviceSpeedUp");
			DeviceCategory devcat = ModelParameters.inputDistSys.getVDevice(vdevname).category;
			VirtualDevice vdev = new VirtualDevice(vdevname, devcat);
			devices.put(vdev.name, vdev);
		}
		getDevice(vdevname).speedUpFactor = pspeed;
	}

	public void addDeviceSchedPol(String vdevname, SchedulingPolicy pol) throws DeviceNotFoundException {
		if (isDeviceDeployed(vdevname) == false) {
			//System.out.println(name + " Device Created in addDeviceSchedPol");
			DeviceCategory devcat = ModelParameters.inputDistSys.getVDevice(vdevname).category;
			VirtualDevice vdev = new VirtualDevice(vdevname, devcat);
			devices.put(vdev.name, vdev);
		}
		getDevice(vdevname).schedulingPolicy = pol;
	}
	
	public void addDeviceBaseSpeed(String vdevname, Variable basespeed) throws DeviceNotFoundException {
		if (isDeviceDeployed(vdevname) == false) {
			//System.out.println(name + " Device Created in addDeviceSpeedUp");
			DeviceCategory devcat = ModelParameters.inputDistSys.getVDevice(vdevname).category;
			VirtualDevice vdev = new VirtualDevice(vdevname, devcat, basespeed.value);
			devices.put(vdev.name, vdev);
		}
		getDevice(vdevname).basespeed.value = basespeed.value;
	}
	
	public VirtualMachine getCopy(String vmname, int num) throws DeviceNotFoundException {
		VirtualMachine vmcpy = new VirtualMachine(vmname, num);
		for (Device d : devices.values()) {
			Device dcpy = d.getCopy();
			vmcpy.addDevice(dcpy);
		}
		for (SoftResource sr : softResources.values()) {
			SoftResource srcpy = sr.getCopy();
			vmcpy.addSoftRes(srcpy);
		}
		for (SoftServer ss : softServers.values()) {
			vmcpy.addServer(ss);
		}
		vmcpy.addLan(lan);
		vmcpy.host = host;
		vmcpy.virtualizationEnabled = virtualizationEnabled;
		return vmcpy;
	}
	
	/* This function is used for creating deep copy of an physical machine object
	 * Note that, it doesn't create deepcopy of member array variable 'vms'.
	 */
	public VirtualMachine getCopy() throws DeviceNotFoundException {
		VirtualMachine vmcpy = new VirtualMachine();
		vmcpy.name = name;
		for (Device d : devices.values()) {
			Device dcpy = d.getCopy();
			vmcpy.addDevice(dcpy);
		}
		for (SoftResource sr : softResources.values()) {
			SoftResource srcpy = sr.getCopy();
			vmcpy.addSoftRes(srcpy);
		}
		for (SoftServer ss : softServers.values()) {
			vmcpy.addServer(ss);
		}
		vmcpy.addLan(lan);
		vmcpy.host = host;
		return vmcpy;
	}
	
}
