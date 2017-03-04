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

import perfcenter.baseclass.enums.PowerManagementGovernor;
import perfcenter.baseclass.DeviceCategory;
import perfcenter.baseclass.enums.SchedulingPolicy;
import perfcenter.baseclass.exception.DeviceNotFoundException;
import perfcenter.simulator.SimulationParameters;
import perfcenter.simulator.queue.QServerInstance;
import perfcenter.simulator.queue.QueueSim;

/**
 * This class defines devices that would be deployed on Host.
 * 
 * This class is agnostic to the actual type of device, and does not differentiate
 * between CPU or disk or printer. Powermanagement feature of the device are built around
 * those of CPU.
 * 
 * This class holds the definition of a device instance, and its configuration parameters
 * like its count, buffer size, scheduling policy, power up speed, etc.
 * 
 * @author akhila
 * @author rakesh
 * @author bhavin
 */
public class Device extends QueuingResource {

	/** Name of the device. This identifies the device throughout the perfcenter. */
	public String name = "undef";
	public DeviceCategory category; 
	
	
	/** Number of device instances. This is used to spawn as many {@link perfcenter.simulator.queue.QServerInstance QServerInstance} objects */
	public Variable count = new Variable("local", 1);
	
	/** Size of queue buffer. Its unit is number of requests. */
	public Variable buffer = new Variable("local", 99999999);
	
	/** Scheduling policy object which would decide how to enqueue and dequeue something. 
	 */
	public SchedulingPolicy schedulingPolicy = SchedulingPolicy.FCFS;
	
	/** Basespeed of device. Its unit depends on input specification. It is modeler dependent.
	 *  If value is set to -1, then its basespeed is not set 
	 **/
	public Variable basespeed = new Variable("local", -1);
	
	/** Device speed up factor. It decides the <i>effective</i> service time of a job.
	 * 
	 *  e.g. speed up factor of "k" would require a job with service demand "s" to be served
	 *  in the time "s/k". 
	 */
	public Variable speedUpFactor = new Variable("local", 1);

	/** Holding time of a job by the device. This is used by analytical perfcenter. */
	public double holdingTime;
	
	/** think time of a request at this device. This is used by analytical perfcenter. */
	public double thinkTime;
	
	/** Number of users at the device. This is used by analytical perfcenter. */
	public double numberOfUsers;
	
	/** Available operating speed leves for a power managed device.
	 * 
	 * @author rakesh
	 */
	public double availableSpeedLevels[] = new double[35]; // speed_inedx limit=15; taken implicitly
	
	/**
	 * The dynamic power consumption component corresponding to each speed level, of a power managed device.
	 */
	public double powerConsumptionsLevels[] = new double[35];
	
	/** Idle power consumption of the power managed device, for each speed level. 
	 *
	 * 	@author bhavin
	 */
	public double[] idlePower = new double[35];
	
	/** Saves average device speed. This is used only by the bottleneck function in the Output. */
	public double avgDeviceSpeedup;
	
	/** Device probe interval. Device is probed after this interval periodically. */
	public double deviceProbeInterval = 99999;
	
	/** The upthreshold parameter of the frequency scaling governor. */
	public double upThreshold = 100;
	
	/** The downthreshold parameter of the frequency scaling governor. */
	public double downThreshold = 0;
	
	/** Governing policy of power-management governor. */
	public PowerManagementGovernor governor;
	
	/** Flag indicating whether this is a powerymanaged device or not. */
	public boolean isDevicePowerManaged = false;

	/** Used only by USERSPACE governor, gives the index of device speed selected for USERSPACE governor */
	public double userspaceSpeedIndex = 0;
	
	/** Total frequency levels available for the device. */
	public int totalFrequencyLevels = 0;
	
	/** Total power levels available for the device. */
	public int totalPowerLevels = 0;
	
	/** Total idle power levels available for the device. */
	public int totalIdlePowerLevels = 0;
	
	/** Average Frequency of the device. */
	public Metric averageFrequency = new Metric();

	public Device() {
		this.availableSpeedLevels[0] = 1;
	}

	public Device(String _name) {
		this();
		name = _name;
	}
	
	public Device(String _name, DeviceCategory _category) {
		this();
		name = _name;
		category = _category;
	}
	
	public Device(String _name, String _catname) {
		this();
		name = _name;
		category = ModelParameters.inputDistSys.getDeviceCategory(_catname);
	}
	
	public Device(String _name, DeviceCategory _category, double _baselineSpeed){
		this();
		name = _name;
		category = _category;
		basespeed.value = _baselineSpeed;
	}
	
	public Device(String _name, String _catname, double _baselineSpeed){
		this();
		name = _name;
		category = ModelParameters.inputDistSys.getDeviceCategory(_catname);
		basespeed.value = _baselineSpeed;
	}
/*
	public Device(Device anotherDevice){
		schedulingPolicy = anotherDevice.schedulingPolicy;
		name = anotherDevice.name;
		category = anotherDevice.category;
		if (anotherDevice.buffer.getName().compareToIgnoreCase("local") != 0) {
			buffer = anotherDevice.buffer;
		} else {
			buffer.value = anotherDevice.buffer.value;
		}
		if (anotherDevice.count.getName().compareToIgnoreCase("local") != 0) {
			count = anotherDevice.count;
		} else {
			count.value = anotherDevice.count.value;
		}
		if (anotherDevice.speedUpFactor.getName().compareToIgnoreCase("local") != 0) {
			speedUpFactor = anotherDevice.speedUpFactor;
		} else {
			speedUpFactor.value = anotherDevice.speedUpFactor.value;
		}
		idlePower = anotherDevice.idlePower;
		isDevicePowerManaged = anotherDevice.isDevicePowerManaged;
		System.arraycopy(anotherDevice.powerConsumptionsLevels, 0, powerConsumptionsLevels, 0, anotherDevice.powerConsumptionsLevels.length);
		System.arraycopy(anotherDevice.availableSpeedLevels, 0, availableSpeedLevels, 0, anotherDevice.availableSpeedLevels.length);
		avgDeviceSpeedup = anotherDevice.avgDeviceSpeedup;
		downThreshold = anotherDevice.downThreshold;
		upThreshold = anotherDevice.upThreshold;
		deviceProbeInterval = anotherDevice.deviceProbeInterval;
		userspaceSpeedIndex = anotherDevice.userspaceSpeedIndex;
		governor = anotherDevice.governor;
	} */
	
	public String toString() {
		StringBuilder builder = new StringBuilder("Device:").append(name).append(" Sched pol ").append(schedulingPolicy).append(")\n");
		builder.append("count(").append(count.getName()).append(":").append(count.getValue()).append(")\n");
		builder.append("buffer(").append(buffer.getName()).append(":").append(buffer.getValue()).append(")\n");
		builder.append("speedupfactor(").append(speedUpFactor.getName()).append(":").append(speedUpFactor.getValue()).append(")\n");
		return builder.toString();
	}

	public Device getCopy() {
		Device dcpy = new Device();

		dcpy.schedulingPolicy = schedulingPolicy;
		dcpy.name = name;
		dcpy.category = category;
		if (buffer.getName().compareToIgnoreCase("local") != 0) {
			dcpy.buffer = buffer;
		} else {
			dcpy.buffer.value = buffer.value;
		}
		if (count.getName().compareToIgnoreCase("local") != 0) {
			dcpy.count = count;
		} else {
			dcpy.count.value = count.value;
		}
		if (speedUpFactor.getName().compareToIgnoreCase("local") != 0) {
			dcpy.speedUpFactor = speedUpFactor;
		} else {
			dcpy.speedUpFactor.value = speedUpFactor.value;
		}
		dcpy.idlePower = idlePower;
		dcpy.isDevicePowerManaged = isDevicePowerManaged;
		System.arraycopy(powerConsumptionsLevels, 0, this.powerConsumptionsLevels, 0, powerConsumptionsLevels.length);
		System.arraycopy(availableSpeedLevels, 0, this.availableSpeedLevels, 0, availableSpeedLevels.length);
		dcpy.avgDeviceSpeedup = avgDeviceSpeedup;
		dcpy.downThreshold = downThreshold;
		dcpy.upThreshold = upThreshold;
		dcpy.deviceProbeInterval = deviceProbeInterval;
		dcpy.userspaceSpeedIndex = userspaceSpeedIndex;
		dcpy.governor = governor;
		return dcpy;
	}

	public void setHoldingTime(double htime) {
		holdingTime = htime;
	}

	public double getHoldingTime() {
		return holdingTime;
	}

	public String getDeviceName() {
		return name;
	}

	/** Used by analytical part */
	public void initialize() {
		holdingTime = 0;
		resourceQueue.initialize();
	}

	public void addSpeedLevels(String name, Variable v) throws DeviceNotFoundException {
		this.availableSpeedLevels[this.totalFrequencyLevels++] = v.value;
	}

	public void addPowerConsumedLevels(String name, Variable v) throws DeviceNotFoundException {
		this.powerConsumptionsLevels[this.totalPowerLevels++] = v.value;

	}

	public void addIdlePower(String name, Variable v) throws DeviceNotFoundException {
		this.idlePower[this.totalIdlePowerLevels++] = v.value;
	}

	/** Set a default probe-interval for all power-managed devices having same device-name */
	public void addProbeInterval(String name, Variable v) throws DeviceNotFoundException {
		this.deviceProbeInterval = v.value;
	}

	/** Set a default governor up-threshold for all power-managed devices having same device-name */
	public void addGovernorUpThreshold(String name, Variable v) throws DeviceNotFoundException {
		this.upThreshold = v.value;
	}

	/** Set a default governor down-threshold for all power-managed devices having same device-name */
	public void addGovernorDownThreshold(String name, Variable v) throws DeviceNotFoundException {
		this.downThreshold = v.value;
	}

	/*
	 * Calculating device instance speedup here. dev_util*speedup = sigma(qsi.uti * qsi.speedup)/ dev_util;
	 */
	public void calculateAndPrintAverageDeviceSpeedup(Machine host) {
		avgDeviceSpeedup = 0;
		double qsi_util = 0.0;
		int i = 0;
		for (QServerInstance qsi : ((QueueSim) resourceQueue).qServerInstances) // devQ has devices of type cpu
		{
			// sum(qsi.util * qsi.avg_speedup)
			avgDeviceSpeedup += (qsi.totalBusyTime.getTotalValue() / SimulationParameters.currTime) * qsi.avgSpeedup;
			qsi_util += qsi.totalBusyTime.getTotalValue() / SimulationParameters.currTime;
			i++;
		}
		avgDeviceSpeedup = avgDeviceSpeedup / qsi_util;		
	}
}