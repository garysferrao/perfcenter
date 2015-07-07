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
	
	/** Number of device instances. This is used to spawn as many {@link perfcenter.simulator.queue.QServerInstance QServerInstance} objects */
	public Variable count = new Variable("local", 1);
	
	/** Size of queue buffer. Its unit is number of requests. */
	public Variable buffer = new Variable("local", 99999999);
	
	/** Scheduling policy object which would decide how to enqueue and dequeue something. 
	 */
	public SchedulingPolicy schedulingPolicy = SchedulingPolicy.FCFS;
	
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
	public double availabelSpeedLevels[] = new double[35]; // speed_inedx limit=15; taken implecitly
	
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
	
	/** Flag indicating whether this is a powermanaged device or not. */
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
		this.availabelSpeedLevels[0] = 1;
	}

	public Device(String deviceName) {
		this();
		name = deviceName;
	}

	/** Print device and its values */
	public void print() {
		System.out.println("----StartDevice:" + name + " Sched pol " + schedulingPolicy);
		System.out.println("count--" + count.getName() + " " + count.getValue());
		System.out.println("buffer--" + buffer.getName() + " " + buffer.getValue());
		System.out.println("speedupfactor--" + speedUpFactor.getName() + " " + speedUpFactor.getValue());
		System.out.println("----EndDevice:" + name);
	}

	public Device getCopy() {
		Device dcpy = new Device();

		dcpy.schedulingPolicy = schedulingPolicy;
		dcpy.name = name;
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
		System.arraycopy(availabelSpeedLevels, 0, this.availabelSpeedLevels, 0, availabelSpeedLevels.length);
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
		this.availabelSpeedLevels[this.totalFrequencyLevels++] = v.value;
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
	public void calculateAndPrintAverageDeviceSpeedup(Host host) {
		avgDeviceSpeedup = 0;
		double qsi_util = 0.0;
		int i = 0;
		System.out.println("\n============== Host Machine: " + host.name);
		for (QServerInstance qsi : ((QueueSim) resourceQueue).qServerInstances) // devQ has devices of type cpu
		{
			System.out.println("------>>   device name: " + name + "\t instance: " + i + "\t Device instance util:" + qsi.totalBusyTime.getTotalValue()
					/ SimulationParameters.currentTime + "\t Device instance speedup: " + qsi.avgSpeedup);

			// sum(qsi.util * qsi.avg_speedup)
			avgDeviceSpeedup += (qsi.totalBusyTime.getTotalValue() / SimulationParameters.currentTime) * qsi.avgSpeedup;
			qsi_util += qsi.totalBusyTime.getTotalValue() / SimulationParameters.currentTime;
			i++;
		}
		avgDeviceSpeedup = avgDeviceSpeedup / qsi_util;
		
		System.out.println(">>>>>>>> Device Name: " + name + "  Active governor: " + this.governor + "  Device Speedup: " + avgDeviceSpeedup);
		System.out.println(" probe interval: " + this.deviceProbeInterval + "   up_threshold: " + this.upThreshold);
	}
}