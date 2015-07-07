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
package perfcenter.simulator.queue;

import static perfcenter.simulator.DistributedSystemSim.calculateConfidenceIntervalForMetric;

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.Stack;

import perfcenter.baseclass.Device;
import perfcenter.baseclass.Host;
import perfcenter.baseclass.ModelParameters;
import perfcenter.baseclass.Queue;
import perfcenter.baseclass.SoftServer;
import perfcenter.baseclass.exception.DeviceNotFoundException;
import perfcenter.simulator.SimulationParameters;
import perfcenter.simulator.metric.DiscreteSampleAverageMetric;
import perfcenter.simulator.metric.ManuallyComputedMetric;
import perfcenter.simulator.metric.SummationMetric;
import perfcenter.simulator.metric.TimeAverageMetric;
import perfcenter.simulator.request.Request;
/**
 * Implementation of M/M/C queue
 */
public class QueueSim extends Queue {

	/**
	 * pointer to qserver that services the requests for this queue.
	 * qserver can be softserver, device, networklink or virtualresource
	 */
	public QueueServer qServer;

	/**
	 * number of active instances or servers as in m/m/s queue this was used to find qlen(number in system).
	 * This is not being used by any module today, but still retaining for any possible future use
	 */
	public int numBusyInstances;
	/** 
	 * number of queueserver instances. in M/M/C this is C value 
	 */
	public int numberOfInstances;
	public ArrayList<QServerInstance> qServerInstances;
	
	public Stack<QServerInstance> freeQServerInstances = new Stack<QServerInstance>();

	public ArrayList<QueueBufferSlot> queueBuffer; //SCALABILITY: change to LinkedList
	private int bufferSize;
	
	private SummationMetric totalNumberOfRequestArrivals = new SummationMetric(); //DOUBT: dropProb in cyclic workload? req coming in one slot, and serviced in another
	private SummationMetric totalNumberOfRequestsServed = new SummationMetric();
	public SummationMetric totalNumberOfRequestsBlocked = new SummationMetric();

	/** queue measurements */
	// nadeesh Commented totalWaitingTime. It used earlier when averageWaitingTimeSim was ManuallyComputedMetric
	// now averageServiceTimeSim is DiscreteSampleAverageMetric therefore not required
	//public SummationMetric totalWaitingTime = new SummationMetric(); 
	public DiscreteSampleAverageMetric averageWaitingTimeSim = new DiscreteSampleAverageMetric(0.95); //FIXME remove hardcoding
	private DiscreteSampleAverageMetric averageServiceTimeSim = new DiscreteSampleAverageMetric(0.95); //FIXME remove hardcoding

	private DiscreteSampleAverageMetric averageResponseTimeSim = new DiscreteSampleAverageMetric(ModelParameters.getResptCILevel());
	private TimeAverageMetric averageQueueLengthSim = new TimeAverageMetric(ModelParameters.getQlenCILevel());
	private ManuallyComputedMetric averageThroughputSim = new ManuallyComputedMetric(ModelParameters.getTputCILevel());
	private ManuallyComputedMetric averageUtilizationSim = new ManuallyComputedMetric(ModelParameters.getUtilCILevel());
	private ManuallyComputedMetric averagePowerConsumedSim = new ManuallyComputedMetric(0.95); //FIXME remove hardcoding
	private ManuallyComputedMetric averageEnergyConsumptionPerRequestSim = new ManuallyComputedMetric(0.95); //FIXME: remove hardcoding
	private ManuallyComputedMetric averageArrivalRateSim = new ManuallyComputedMetric(0.95); //FIXME: remove hardcoding
	private ManuallyComputedMetric blockingProbabilitySim = new ManuallyComputedMetric(0.95); //FIXME: remove hardcoding
	
	private ManuallyComputedMetric powerDelayProductSim = new ManuallyComputedMetric(0.95); //FIXME: remove hardcoding
	private ManuallyComputedMetric powerEfficiencySim = new ManuallyComputedMetric(0.95); //FIXME: remove hardcoding

	public QueueSim(Integer bufferSize_, Integer numberOfInstances_,/* String resType, */
			QueueServer qServer_) {
		bufferSize = bufferSize_.intValue();
		numberOfInstances = numberOfInstances_.intValue();
		queueBuffer = new ArrayList<QueueBufferSlot>(bufferSize);

		qServerInstances = new ArrayList<QServerInstance>();
		for (int i = 0; i < numberOfInstances; i++) {
			qServerInstances.add(new QServerInstance(i, this));
		}

		qServer = qServer_;

	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	public static QueueSim loadSchedulingPolicyClass(String schedulingPolicy, int qLength, int noOfInst,/* String resType, */QueueServer qs) {
		QueueSim queueS = null;

		try {
			schedulingPolicy.toUpperCase();
			Class c = Class.forName("perfcenter.simulator.queue.SchedulingStratergy." + schedulingPolicy);

			Class[] proto = new Class[3];
			proto[0] = Integer.class;
			proto[1] = Integer.class;
			// proto[2] = String.class;

			// added queueserver
			proto[2] = QueueServer.class;

			Object[] params = new Object[3];
			params[0] = new Integer(qLength);
			params[1] = new Integer(noOfInst);
			// params[2] = resType;
			params[2] = qs;

			Constructor cons = c.getConstructor(proto);
			queueS = (QueueSim) cons.newInstance(params);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return (queueS);
	}

	/**
	 * This procedure calculates the utilization measures of the device RAM. RAM utilization is calculated for all the host and all the softServres
	 * deployed on that host.
	 * 
	 * @param currentHost
	 *            : current host for which utilization is to be calculated Assumption : Busy and idle threads consumes the same amount of memory
	 */
	//FIXME: method contains more than it should
	public void calculateConfidenceIntervalsForRAM(Host currentHost) {

		/* variables to calcualte a softServer's RAM utilization */
		// average queue lenght of a softServer
		double qLength = 0;
		// average queue lenght CI at a softServer
		double qLengthCI = 0;
		// qlen + qlenCI = maxQlen.
		double maximumQLength = 0;
		// memory required by a softServer in MB
		double memoryRequirementOfCurrentServer = 0;
		// Max memory required by a softServer in MB, corresponding to maxQlen.
		double memoryRequirementOfCurrentServerCI = 0;
		// softServer util
		double softServerUtilization = 0;
		// softServer utilCI
		@SuppressWarnings("unused")
		double softServerUtilizationCI = 0;

		/* varialbes to calcualte the RAM util of host */
		// Sum of memory requirements of all softServers
		double memoryOfAllSoftServers = 0.0;
		// Sum of max memory requirements of all softServers
		double memoryOfAllSoftServersCI = 0.0;
		// total RAM size available
		double ramSize = 0.0;

		// List of all software servers on the current host.
		ArrayList<SoftServer> softServers = currentHost.getSoftServersList();

		// find out the device id for device RAM from the device list of current host.
		Device ram = null;
		ArrayList<Device> devices = currentHost.getDevicesList();
		for (Device device : devices) {
			if (device.name.equals("ram")) {
				ram = device;
			}
		}

		// RAM size of current host
		ramSize = ram.buffer.value;

		// calculate the utilization of each softServer.
		for (SoftServer server : softServers) {
			// Reset all local variable
			qLength = 0;
			qLengthCI = 0;
			maximumQLength = 0;
			softServerUtilization = 0;
			softServerUtilizationCI = 0;
			memoryRequirementOfCurrentServer = 0;
			memoryRequirementOfCurrentServerCI = 0;

			// get the handle of the server which contains the output data of current server.
			SoftServer softServer = ((SoftServer) SimulationParameters.distributedSystemSim.getHost(currentHost.name).getServer(server.name));

			/* calcualte utilization for current softServer */
			
			//FIXME: replace this with averageQueueLengthSim,
			//but make sure that calculateConfidenceIntervals() is getting called on it before this line
			qLength = softServer.resourceQueue.averageQueueLength.getValue();  
			memoryRequirementOfCurrentServer = (server.getStaticSize() + (server.getThreadSize() * server.thrdCount.value) + (server.getRequestSize() * qLength));
			softServerUtilization = (memoryRequirementOfCurrentServer / ramSize);
			this.averageUtilization.setValue((memoryOfAllSoftServers / ramSize));
			this.averageUtilization.setConfidenceInterval(((memoryOfAllSoftServersCI - memoryOfAllSoftServers) / ramSize));
			if (averageUtilization.getValue() > 1) {
				this.averageUtilization.setValue(1.0);
			}

			if (softServerUtilization > 1.0) {
				softServerUtilization = 1.0;
			}

			/* calcualte utilization CI for current softServer */
			//FIXME: refer previous block
			qLengthCI = softServer.resourceQueue.averageQueueLength.getConfidenceInterval();
			maximumQLength = qLengthCI + qLength;
			memoryRequirementOfCurrentServerCI = ((server.getStaticSize() + (server.getThreadSize() * server.thrdCount.value) + (server
					.getRequestSize() * maximumQLength)));
			softServerUtilizationCI = ((memoryRequirementOfCurrentServerCI - memoryRequirementOfCurrentServer) / ramSize);
			// update the host utilization measures due to current softServer.
			memoryOfAllSoftServers += memoryRequirementOfCurrentServer;
			memoryOfAllSoftServersCI += memoryRequirementOfCurrentServerCI;
		}

		// calculate util and utilCI for host
		this.averageUtilization.setValue((memoryOfAllSoftServers / ramSize));
		this.averageUtilization.setConfidenceInterval(((memoryOfAllSoftServersCI - memoryOfAllSoftServers) / ramSize));
		if (averageUtilization.getValue() > 1) {
			this.averageUtilization.setValue(1.0);
		}
	}

	/**
	 * this method returns the number of instance which is free if all instances are busy it returns -1 to indicate so
	 * 
	 * @return
	 */
	public int getIdleInstanceId() {
		if(freeQServerInstances.empty()) {
			return -1;
		} else {
			QServerInstance qsi = freeQServerInstances.pop();
			qsi.onFreeStack = false;
			if(qsi.isBusyStatus()) {
				System.err.println("Busy qserver picked up. This is a bug.");
				try {
					throw new Exception();
				} catch (Exception e) {
					System.err.println("size: " + freeQServerInstances.size());
					System.err.println(this.qServer.getClass());
					
					e.printStackTrace();
					System.exit(1);
				}
			}
			
//			this.busyQServerInstances.push(qsi);
			return qsi.id;
		}
		/*for (QServerInstance qsi : qServerInstances) {
			if (!qsi.isBusyStatus()) {
				return qsi.id;
			}
		}
		return -1;*/
	}

	public void enqueue(Request req, double currTime) throws Exception {
		//this will not be called. The enqueue method in SchedulingStratergy package's one of the classes (either FCFS or LCFS, or something else) will be called.
	}

//	public void enqueue_Device(Request req, double currTime) throws Exception {}

	public void dequeueAndProcess(double currTime) throws Exception {}

	// returns true if buffer is full.
	public boolean isBufferFull() {
		if (queueBuffer.size() < bufferSize) {
			return (false);
		} else {
			return (true);
		}
	}

	public int getBuffersize() {
		return (queueBuffer.size());
	}

	/** Method is invoked at the end of simulation. */
	public void calculateConfidenceIntervalsAtTheEndOfReplications() {
		calculateConfidenceIntervalForMetric(averageQueueLength, averageQueueLengthSim);
		calculateConfidenceIntervalForMetric(averageThroughput, averageThroughputSim);
		calculateConfidenceIntervalForMetric(averageResponseTime, averageResponseTimeSim);
		calculateConfidenceIntervalForMetric(averageUtilization, averageUtilizationSim);
		
		calculateConfidenceIntervalForMetric(averageServiceTime, averageServiceTimeSim);
		calculateConfidenceIntervalForMetric(averageWaitingTime, averageWaitingTimeSim);
		calculateConfidenceIntervalForMetric(averageArrivalRate, averageArrivalRateSim);
		calculateConfidenceIntervalForMetric(blockingProbability, blockingProbabilitySim);
		
		calculateConfidenceIntervalForMetric(powerDelayProduct, powerDelayProductSim);
		calculateConfidenceIntervalForMetric(powerEfficiency, powerEfficiencySim);
		calculateConfidenceIntervalForMetric(averagePowerConsumed, averagePowerConsumedSim);
		calculateConfidenceIntervalForMetric(averageEnergyConsumptionPerRequest, averageEnergyConsumptionPerRequestSim);
		
		// End Calculation: Calculate the utilization and utilCI for all s/w servers on host individually.
		// Added by  Nikhil
		/*if (deviceName != null) {
			ArrayList<SoftServer> softServers = SimulationParameters.distributedSystemSim.getHost(hostName).getSoftServersList();

			for (SoftServer srv : softServers) {
				ArrayList<UtilizationVariable> utilArray = ((SoftServer) SimulationParameters.distributedSystemSim.getHost(hostName).getServer(
						srv.name)).resourceQueue.deviceUtilizationList;
				for (UtilizationVariable var : utilArray) {
					if (var.name.compareToIgnoreCase(deviceName) == 0) {
						((SoftServer) SimulationParameters.distributedSystemSim.getHost(hostName).getServer(srv.name)).resourceQueue
								.getUtilVariable(deviceName).deviceDataForUtilizationConfInt.calculateConfidenceIntervals();
						((SoftServer) SimulationParameters.distributedSystemSim.getHost(hostName).getServer(srv.name)).resourceQueue
								.getUtilVariable(deviceName).utilization = ((SoftServer) SimulationParameters.distributedSystemSim.getHost(hostName)
								.getServer(srv.name)).resourceQueue.getUtilVariable(deviceName).deviceDataForUtilizationConfInt.getMean();
						((SoftServer) SimulationParameters.distributedSystemSim.getHost(hostName).getServer(srv.name)).resourceQueue
								.getUtilVariable(deviceName).utilizationConfInt = ((SoftServer) SimulationParameters.distributedSystemSim.getHost(
								hostName).getServer(srv.name)).resourceQueue.getUtilVariable(deviceName).deviceDataForUtilizationConfInt.getCI();
					}
				}
			}
		}*/
	}

	// when the service starts update the bookkeeping structures
	// and transfer control to qserver. akhila
	public void createStartTaskEvent(Request req, int instanceID, double time) {

		// marks the startime, and sets busy status to true
		qServerInstances.get(instanceID).startServiceForInstance(req,time);  //added req also as a param nadeesh
		numBusyInstances++;
		// qserver instance is now called
		qServer.createStartTaskEvent(req, instanceID, time);
	}

	public void endServiceTimeout(int instanceID, double time) throws Exception {
//		totalNumberOfRequestsBlocked.recordValue(1);
		if (!(queueBuffer.isEmpty())) {
			dequeueAndProcess(SimulationParameters.currentTime);
		}
	}

	/**
	 * 
	 * @author Akhila
	 */
	public void endService(Request request, int instanceID, double time) throws Exception {
		numBusyInstances--;
		totalNumberOfRequestsServed.recordValue(request,1);
		averageResponseTimeSim.recordValue(request,time - qServerInstances.get(instanceID).reqArrivalTime);
		averageServiceTimeSim.recordValue(request,time - qServerInstances.get(instanceID).reqStartTime);

		// set busy status to false and update total busy time
		qServerInstances.get(instanceID).endServiceForInstance(time,request);

		averageQueueLengthSim.recordValue(request,queueBuffer.size());
		
		// instance is again available. now check whether the queue buffer is empty or not.
		// if !empty then use current resource instance to process next request in buffer
		if (!(queueBuffer.isEmpty())) {
			dequeueAndProcess(SimulationParameters.currentTime);
		}
		
	}

/*	 the following method is exactly identical to endService(int, double) (I did a diff before commenting th
	 
	 *this is called by queueserver when a request ends.
	 *queuesim updates the book keeping datastructures.
	 
	 public void endDeviceService(int instanceID, double time) {
	 //decrement num of busy instances
	 numBusyInstances--;
	
	 // increment num of requests served
	 numRequestServed++;
	
	 // increment total response time
	 // total_responce_time & service_time will be affected by cpu freq.,
	 // service_time_remaining has to be changed 
	 totalResponseTime += time - qServerInstances.get(instanceID).reqArrivalTime;
	
	 //update total service time
	 totalServiceTime += time - qServerInstances.get(instanceID).reqStartTime;
	
	 //set busy status to false and update total busy time
	 // we need this constructor for PM devices -rakesh
	 qServerInstances.get(instanceID).endServiceForInstance(time);
	
	 updateIntermediatePerformanceMeasures(SimulationParameters.currentTime);
	
	
	 // instance is again available.
	 // now check whether the queue buffer is empty or not.
	 // if !empty then use current resource instance to process next
	 // request in buffer
	 if (!isBufferEmpty()) {
	 dequeueAndProcess(instanceID, SimulationParameters.currentTime);
	 }
	
	 }
*/
	
	
	public void processRequestArrival(Request request, int instanceID, double time) {
		if (instanceID > -1) {
			// mark req arrival time
			qServerInstances.get(instanceID).reqArrivalTime = time;
		}
		// increment number of requests arrived
		this.totalNumberOfRequestArrivals.recordValue(request,1);
	}

	public void addRequestToBuffer(Request request, double time) {
		// any changes in the buffersize calls for updating avgqlen
		averageQueueLengthSim.recordValue(request,queueBuffer.size());

		// save request id and req arrival time in buffer
		QueueBufferSlot qb = new QueueBufferSlot(request, time);
		queueBuffer.add(qb);
	}

	public Request getRequestFromBuffer(int position, double time) throws Exception {
		//FIXME: remove time as method argument. Use SimulationParameters.currentTime instead directly wherever required.
		if (position > queueBuffer.size()) { 
			// FIXME: handle this situation in a better way by not throwing an Exception, but may a more precise
			// exception, if at all that is required.
			throw new Exception("getRequestFromBuffer error");
		}

		// remove the request from specified position
		QueueBufferSlot qb = queueBuffer.remove(position);
		
		// To record request , moved the averageQueueLengthSim from top to this place 
		// +1 added to the queueBuffer.size() for including the removed request from the buffer
		averageQueueLengthSim.recordValue(qb.requestObject,queueBuffer.size()+1);

		// set the arrival time of the request
//		qServerInstances.get(instanceID).reqArrivalTime = qb.reqArrivalTime;
		return qb.requestObject;
	}

	// this function is called at the end of simulation.
	// Calcuates all the queue parameters.
	public void recordCISampleAtTheEndOfSimulation() {
		assert numberOfInstances == qServerInstances.size() : "Number of instances and qServerInstance dont agree" + debugPrint();
		averageQueueLengthSim.recordValue(queueBuffer.size());
		assert SimulationParameters.totalRequestsArrived >= ModelParameters.getTotalNumberOfRequests() : "sim: " + SimulationParameters.totalRequestsArrived + " model: "
				+ ModelParameters.getTotalNumberOfRequests();
		for (int slot = 0; slot < ModelParameters.intervalSlotCount; slot++) {
			// averageArrivalRateSim.recordCISample(slot,
			// totalNumberOfRequestArrivals.getTotalValue(slot) /
			// SimulationParameters.getIntervalSlotRunTime(slot));
			if ((int) totalNumberOfRequestsServed.getTotalValue(slot) > 0) {

				averageQueueLengthSim.recordCISample(slot);
				averageResponseTimeSim.recordCISample(slot);
				averageServiceTimeSim.recordCISample(slot);
				averageWaitingTimeSim.recordCISample(slot);
				// nadeesh if deviceName and hostName != null then store metric
				// value perServer level
				// else pass null as server name
				if (deviceName != null && hostName != null) {
					for (SoftServer softServ : SimulationParameters.distributedSystemSim.getHost(hostName).getSoftServersList()) {
						recordCISampleofManuallyComputedMetricAtTheEndOfSimulation(slot, softServ.name);
					}
					
					double totalEnergyConsumedInThisRun = 0 ;
					for (QServerInstance qsi : qServerInstances) {
						totalEnergyConsumedInThisRun += qsi.totalEnergyConsumption.getTotalValue(slot,"_idle");
					}
					try {
						averagePowerConsumedSim.recordCISample(slot, "_idle", totalEnergyConsumedInThisRun / SimulationParameters.getIntervalSlotRunTime(slot) / SimulationParameters.distributedSystemSim.getHost(this.hostName).getDevice(this.deviceName).count.getValue());
					} catch (DeviceNotFoundException e) {
						//will never come here, as device name passed is always valid
						e.printStackTrace();
					}
				} else {
					recordCISampleofManuallyComputedMetricAtTheEndOfSimulation(slot, null);
				}
			
}
		}
	}

	/**
	 * added by nadeesh
	 * Used to  recordCI sample of ManuallyComputedMetric
	 * @param slot
	 * @param softServerName
	 */
	private void recordCISampleofManuallyComputedMetricAtTheEndOfSimulation(int slot, String softServerName) {
		double totalEnergyConsumedInThisRun = 0 ;
		double averageThroughputSample = 0 ;
		double serverBusyTime = 0;
		for (QServerInstance qsi : qServerInstances) {
			totalEnergyConsumedInThisRun += qsi.totalEnergyConsumption.getTotalValue(slot,softServerName);
			serverBusyTime += qsi.totalBusyTime.getTotalValue(slot, softServerName);
		}
		averageArrivalRateSim.recordCISample(slot, softServerName, totalNumberOfRequestArrivals.getTotalValue(slot,softServerName) / SimulationParameters.getIntervalSlotRunTime(slot));
		averageEnergyConsumptionPerRequestSim.recordCISample(slot, softServerName, totalEnergyConsumedInThisRun / totalNumberOfRequestsServed.getTotalValue(slot,softServerName));
		int count = 1; //count of cpu cores
		if(this.hostName != null && this.deviceName != null) {
			try {
				count = (int) SimulationParameters.distributedSystemSim.getHost(this.hostName).getDevice(this.deviceName).count.getValue();
			} catch (DeviceNotFoundException e) {
				// will never come here as the device name is always valid
				e.printStackTrace();
			}
		}
		averagePowerConsumedSim.recordCISample(slot, softServerName, totalEnergyConsumedInThisRun / SimulationParameters.getIntervalSlotRunTime(slot) / count);
		
		powerDelayProductSim.recordCISample(slot, softServerName,
				totalEnergyConsumedInThisRun / SimulationParameters.getIntervalSlotRunTime(slot) * averageResponseTimeSim.getMean(slot)); 
		// the metric is ill-defined: average power is for the length of the simulation,
		// and average response time is at a request level. Is EPERR*RESPT more sensible?
		averageThroughputSample = totalNumberOfRequestsServed.getTotalValue(slot,softServerName) / SimulationParameters.getIntervalSlotRunTime(slot);
		averageThroughputSim.recordCISample(slot, softServerName, averageThroughputSample);
		//averageWaitingTimeSim.recordCISample(slot, softServerName, totalWaitingTime.getTotalValue(slot,softServerName) / totalNumberOfRequestsServed.getTotalValue(slot,softServerName));
		powerEfficiencySim.recordCISample(slot, softServerName, averageThroughputSample / (totalEnergyConsumedInThisRun / SimulationParameters.getIntervalSlotRunTime(slot)));
		averageUtilizationSim.recordCISample(slot, softServerName, (serverBusyTime / numberOfInstances) / SimulationParameters.getIntervalSlotRunTime(slot));
		blockingProbabilitySim.recordCISample(slot, softServerName, totalNumberOfRequestsBlocked.getTotalValue(slot,softServerName) / totalNumberOfRequestArrivals.getTotalValue(slot,softServerName));
	}

	// A request is getting discarded. This is called from queue server.
	public void discard(int instanceId, double time) {
		// set busystatus to false and updated total busy time
		qServerInstances.get(instanceId).discardReq(time);
		numBusyInstances--;
	}

	private String debugPrint() {
		System.err.println();
		System.err.println(qServer.getClass());

		System.err.println(numberOfInstances);
		System.err.println(qServerInstances);
		return "";
	}
	
	public void clearValuesButKeepConfInts() {
		numBusyInstances=0;
		queueBuffer.clear();
		
		totalNumberOfRequestArrivals.clearValuesButKeepConfInts();
		totalNumberOfRequestsServed.clearValuesButKeepConfInts();
		totalNumberOfRequestsBlocked.clearValuesButKeepConfInts();

		//totalWaitingTime.clearValuesButKeepConfInts();
		averageWaitingTimeSim.clearValuesButKeepConfInts();
		averageServiceTimeSim.clearValuesButKeepConfInts();
		averageArrivalRateSim.clearValuesButKeepConfInts();
		blockingProbabilitySim.clearValuesButKeepConfInts();

		averageResponseTimeSim.clearValuesButKeepConfInts();
		averageQueueLengthSim.clearValuesButKeepConfInts();
		averageThroughputSim.clearValuesButKeepConfInts();
		averageUtilizationSim.clearValuesButKeepConfInts();
		averagePowerConsumedSim.clearValuesButKeepConfInts();
		powerDelayProductSim.clearValuesButKeepConfInts();
		powerEfficiencySim.clearValuesButKeepConfInts();
		averageEnergyConsumptionPerRequestSim.clearValuesButKeepConfInts();
		
		freeQServerInstances.clear();

		for (QServerInstance qServerInstance : qServerInstances) {
			qServerInstance.clearValuesButKeepConfInts();
		}
	}
	
	// this class stores request id and the req arrival time. akhila
	private class QueueBufferSlot {

		private Request requestObject;
		private double reqArrivalTime;

		private QueueBufferSlot(Request req, double time) {
			this.requestObject = req;
			reqArrivalTime = time;
		}
	}
}
