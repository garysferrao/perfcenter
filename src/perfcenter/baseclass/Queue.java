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
/*
 * Represents a queue.
 */
package perfcenter.baseclass;

/**
 * Queue class is the primitive queue which resides in all the components of distributed system.
 * 
 * This class has all the parameters and performance metrics of the queue, which can be set by
 * the method solving it. It also has information about where it belongs in the system.
 */
public class Queue {

	//queue parameters
	public Metric averageQueueLength = new Metric();
	public Metric averageResponseTime = new Metric();
	public Metric averageUtilization = new Metric();
	public Metric averageThroughput = new Metric();
	public Metric averagePowerConsumed = new Metric();
	public Metric averageEnergyConsumptionPerRequest = new Metric();
	
	public Metric powerDelayProduct = new Metric();
	public Metric powerEfficiency = new Metric();

	public Metric averageWaitingTime = new Metric();
	public Metric averageArrivalRate = new Metric();

	public Metric averageServiceTime = new Metric();
	public Metric blockingProbability = new Metric();
	
	// device name
	public String deviceName; //RIGHTNOW: later convert this to reference
	public String serverName; //RIGHTNOW: later convert this to reference
	
	public String hostName;

	public Queue() {
		initialize();
	}

	public void initialize() {
		averageWaitingTime.initialize();
		averageUtilization.initialize();
		averageThroughput.initialize();
		averageResponseTime.initialize();
		averageQueueLength.initialize();

		averageArrivalRate.initialize();
		averageServiceTime.initialize();
		blockingProbability.initialize();
		serverName = null;
		deviceName = null;
		hostName = null;
	}

	public double getBlockingProbability() {
		return blockingProbability.getValue();
	}
}