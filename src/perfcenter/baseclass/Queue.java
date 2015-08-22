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
	public Metric avgQueueLen = new Metric();
	public Metric avgRespTime = new Metric();
	public Metric avgUtil = new Metric();
	public Metric avgThroughput = new Metric();
	public Metric avgPowerConsumed = new Metric();
	public Metric avgEnergyConsumptionPerReq = new Metric();
	
	public Metric powerDelayProduct = new Metric();
	public Metric powerEfficiency = new Metric();

	public Metric avgWaitingTime = new Metric();
	public Metric avgArrivalRate = new Metric();

	public Metric avgServiceTime = new Metric();
	public Metric blockProb = new Metric();
	
	// CHECK : Introuduce a new member called queueAt : HOST, SERVER, DEVICE // VIRTUALIZATION: Add vm, dom0 to the list
	// CHECK : Combine below three variables into one
	public String devName; //RIGHTNOW: later convert this to reference
	public String serverName; //RIGHTNOW: later convert this to reference
	
	public String hostName;

	public Queue() {
		initialize();
	}

	public void initialize() {
		avgWaitingTime.initialize();
		avgUtil.initialize();
		avgThroughput.initialize();
		avgRespTime.initialize();
		avgQueueLen.initialize();

		avgArrivalRate.initialize();
		avgServiceTime.initialize();
		blockProb.initialize();
		serverName = null;
		devName = null;
		hostName = null;
	}

	public double getBlockingProbability() {
		return blockProb.getValue();
	}
}