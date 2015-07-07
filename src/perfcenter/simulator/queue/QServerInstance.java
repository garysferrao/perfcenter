package perfcenter.simulator.queue;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;

import perfcenter.simulator.DeviceSim;
import perfcenter.simulator.Event;
import perfcenter.simulator.SimulationParameters;
import perfcenter.simulator.metric.SummationMetric;
import perfcenter.simulator.request.Request;

//book keeping structure for qserver instance. akhila
//for PMDevices. rakesh

/**
 * This is a book keeping structure for a queue-server instance. It also contains power related numbers for the power-managed devices.
 * <p>
 * This is equivalent to one CPU core. So for a 4 core CPU, there will be one DeviceSim, one QueueSim and four QServerInstance objects.
 * 
 */
public class QServerInstance {

	Logger logger = Logger.getLogger("QServerInstance");
	public int id;
	/** The total time for which device was busy since last probe */
	public double busyTimeInProbeInterval;
	/** Fraction of time device was busy since last probe */
	public double utilizationInProbeInterval;
	/** Last simulation time when the book-keeping in probe was done */
	public double lastPowerUpdate;
	public double last_time_stamp = 0.0;
	public double intermediate_busy_time = 0.0;
	public double intermediate_weighted_busy_time = 0.0;
	public List<Event> deviceInstanceAssociatedEventList;
	public double avgSpeedup = 1;
	/** The index of current frequency level at which the device is operating. */
	public int currentSpeedLevelIndex;
	public double currentSpeed;
	public double lastSpeed = 0.0;
	public double newDeviceSpeedFactor = 1.0;
	public SummationMetric totalEnergyConsumption = new SummationMetric();
	/** time at which request has arrived to the queue */
	public double reqArrivalTime = 0;
	/** time when request starts processing for this qs instance */
	public double reqStartTime = 0;
	/** Currently Handling Request **/
	public Request currentRequest; // added by nadeesh
	/** total busy time for this qs instance */
	public SummationMetric totalBusyTime = new SummationMetric();
	private boolean busyStatus = false;
	private QueueSim parentQueue; // added by nadeesh
	public boolean onFreeStack = false;

	public void initialize() {
		busyTimeInProbeInterval = 0;
		utilizationInProbeInterval = 0;
		lastPowerUpdate = 0;
		last_time_stamp = 0;
		intermediate_busy_time = 0;
		intermediate_weighted_busy_time = 0;
		deviceInstanceAssociatedEventList.clear();
		lastSpeed = 0;
		newDeviceSpeedFactor = 0;
		reqArrivalTime = 0;
		reqStartTime = 0;
		currentRequest = null; // added by nadeesh
		onFreeStack = false;
		setBusyStatus(false);
	}

	/** Modify constructor argument to pass Queuesim **/
	public QServerInstance(int qid, QueueSim parent) {
		id = qid;
		avgSpeedup = 1;
		this.deviceInstanceAssociatedEventList = new ArrayList<Event>();
		parentQueue = parent;
	}

	public void endServiceForInstanceInBuffTimeout(Double time) {
		setBusyStatus(false);
		reqArrivalTime = 0;
		reqStartTime = 0;
		currentRequest = null; // added by nadeesh
	}

	// when request starts at qserver instance mark the starttime and
	// set busy status to true
	// Function overloading
	public void startServiceForInstance(Request req, Double time) {
		currentRequest = req; // added by nadeesh
		reqStartTime = time;
		setBusyStatus(true);
		logger.debug("::::::::::::::::::::::::::: [In start server instances] Time : " + time);

		double timeSinceLastPowerUpdate = SimulationParameters.currentTime - lastPowerUpdate;

		if (parentQueue.qServer instanceof DeviceSim) {
			DeviceSim parentDevice = (DeviceSim) parentQueue.qServer;

			/** Created dummy request namely _idle and store the power used while server is idle **/
			Request idleRequest = new Request(0, null, SimulationParameters.currentTime); // 2nd argument of Request isthe scenario name
			idleRequest.fromServer = "_idle"; // assign idle power to a _idle server
			double freeEnergy = parentDevice.computeEnergy(this, timeSinceLastPowerUpdate, false);
			totalEnergyConsumption.recordValue(idleRequest, freeEnergy);
		}

		lastPowerUpdate = time;
	}

	// total busy time for a request is current time minus request start time
	// Function overloading
	public void endServiceForInstance(Double time, Request request) {
		setBusyStatus(false);
		this.totalBusyTime.recordValue(request, time - reqStartTime);

		// here the busy times are accumulated within one probe interval
		busyTimeInProbeInterval += time - lastPowerUpdate;
		if (parentQueue.qServer instanceof DeviceSim) {
			double power = ((DeviceSim) parentQueue.qServer).computeEnergy(this, time - lastPowerUpdate, true);
			totalEnergyConsumption.recordValue(currentRequest, power);
		}

		lastPowerUpdate = SimulationParameters.currentTime;

		// lastUpdateTimeInProbeInterval = time; // this is updated in device sim
		reqArrivalTime = 0;
		reqStartTime = 0;
		currentRequest = null; // added by nadeesh
	}

	// even when the request is dropped it contributes to busy time and thus utilization
	public void discardReq(Double time) {
		setBusyStatus(false);
		/** nadeesh --added currentRequest as param to recordvalue **/
		this.totalBusyTime.recordValue(currentRequest, time - reqStartTime);
	}

	public void clearValuesButKeepConfInts() {
		totalBusyTime.clearValuesButKeepConfInts();
		totalEnergyConsumption.clearValuesButKeepConfInts();
		initialize();
	}

	public boolean isBusyStatus() {
		return busyStatus;
	}

	public void setBusyStatus(boolean busyStatus) {
		this.busyStatus = busyStatus;
		if (busyStatus == false) { // not busy
			if (!onFreeStack) {
				this.parentQueue.freeQServerInstances.push(this);
				onFreeStack = true;
			}
		}
	}
}
