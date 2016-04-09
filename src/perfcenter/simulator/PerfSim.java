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
package perfcenter.simulator;

import java.util.HashMap;
import java.util.PriorityQueue;

import org.apache.log4j.Logger;

import perfcenter.baseclass.Device;
import perfcenter.baseclass.DistributedSystem;
import perfcenter.baseclass.Machine;
import perfcenter.baseclass.ModelParameters;
import perfcenter.baseclass.Scenario;
import perfcenter.baseclass.SoftServer;
import perfcenter.baseclass.Variable;
import perfcenter.baseclass.enums.SystemType;
import perfcenter.simulator.request.Request;

/**
 * This is the main class that performs simulation.
 * 
 * @author akhila
 */
public class PerfSim {

	//TODO: regenerate the parser, and remove all the compilation errors by using setters in the parser.
	// length of interval after which sample will be taken
	Logger logger = Logger.getLogger("PerfSim");

	public PerfSim(DistributedSystem perfc) {
		SimulationParameters.distributedSystemSim = new DistributedSystemSim(perfc);
		SimulationParameters.requestMap = new HashMap<Integer, Request>();
		SimulationParameters.eventQueue = new PriorityQueue<Event>();
		SimulationParameters.reqIdGenerator = 0;
		SimulationParameters.currTime = 0.0;
		SimulationParameters.clearIntervalSlotCounter();
	}

	public DistributedSystem performSimulation() throws Exception {

		// execute replications for the given set of arrival rates for the different
		// scenarios. the performance measures generated from these scenarios are
		// then used to generate confidence intervals.
		for (SimulationParameters.replicationNo = 0;
				SimulationParameters.replicationNo < ModelParameters.getNumberOfReplications();
				SimulationParameters.replicationNo++) {

			// before starting each simulation iteration, we will have to reset
			// bookkeeping data structures to get rid of info from previous run
			clearValuesButKeepConfInts();
			createObjectReferences();
			startSimulation();
		}
		SimulationParameters.replicationNo--;
		SimulationParameters.distributedSystemSim.computeConfIvalsAtEndOfRepl();
		return SimulationParameters.distributedSystemSim;
	}

	/**
	 * While parsing the input file, all the linking between various objects
	 * happen via their names. These are essentially strings. At runtime,
	 * since searching for a particular component via their name would involve
	 * string search for a gazillion times, it would not scale.
	 * 
	 * This method hence converts all the string names to their respective object references
	 * for the *Sim classes. These references are then used by the simulation logic
	 * to invoke appropriate methods on the various components of the distributed system.
	 */
	private void createObjectReferences() {
		DistributedSystemSim distributedSystemSim = SimulationParameters.distributedSystemSim;
		for(SoftServer softServer : distributedSystemSim.softServers) {
			SoftServerSim softServerSim = (SoftServerSim) softServer;
			for(String hostName : softServerSim.machines) {
				softServerSim.hostObjects.add(distributedSystemSim.getPM(hostName));
			}
		}
		
		/*
		 * scenario
		 * host
		 * device
		 * virtual resource
		 * soft server
		 * lan
		 * lanlink
		 * variable
		 * task
		 * queue
		 */
	}


	/** 
	 * Clear the variables and all other book keeping data-structures
	 * for a daisy-fresh simulation to run.
	 */
	private static void clearValuesButKeepConfInts() {
		SimulationParameters.requestMap.clear();
		SimulationParameters.eventQueue.clear();
		SimulationParameters.currTime = 0.0;
		SimulationParameters.totalReqArrived = 0;
		SimulationParameters.distributedSystemSim.clearValuesButKeepConfIvals();
		SimulationParameters.reqIdGenerator = 0;
		SimulationParameters.currEventBeingHandled = null;
		SimulationParameters.lastScenarioArrivalTime = 0.0;
		SimulationParameters.clearIntervalSlotCounter();
	}

	/**
	 * Start the simulation. Generate initial set of requests (for cold system).
	 * 
	 * Once the simulation starts rolling, it takes care of the warmup and cooldown,
	 * and all other intermediate tasks of a simulation. Keep processing the event list,
	 * till the SIMULATION_ENDS event is encountered.
	 * 
	 * This method is equivalent to one simulation where no parameters are changing.
	 * Once a load parameter / configuration parameter changes, its essentially a
	 * different simulation. Before the input script can do such a thing, this method
	 * would have finished running by design. Next simulation would be the next run
	 * of this method.
	 */
	public void startSimulation() throws Exception {
		// generate requests and add them to the request list
		// and add corresponding arrival events to event list
		if (ModelParameters.getSystemType() == SystemType.OPEN) {
			generateRequestsForOpenSystem();
		} else {
			generateRequestsForClosedSystem();
		}
		
		// run simulation till SIMULATION_ENDS event is encountered.
		while (true) {
			
			Event currentEventToBeHandled = SimulationParameters.eventQueue.poll();

			SimulationParameters.currEventBeingHandled = currentEventToBeHandled;
			if (currentEventToBeHandled == null) {
				assert false;
				throw new Error("No event in event list. It should NEVER happen. This is a bug.");
			}
			
			logger.debug("Event: " + currentEventToBeHandled.type + "\t\ttime: " + currentEventToBeHandled.timestamp);
			switch (currentEventToBeHandled.type) {
			case NO_OF_USERS_CHANGES:
				//System.out.println("No of User event is being handled");
				currentEventToBeHandled.numberOfUserChanged();
				break;
			case ARRIVAL_RATE_CHANGES:
				//System.out.println("Arrival Rate event is being handled");
				currentEventToBeHandled.arrivalRateChanged();
				break;
			case DEVICE_PROBE:
				//System.out.println("Device Probe event is being handled");
				currentEventToBeHandled.deviceProbe();
				break;
			case SCENARIO_ARRIVAL:
				//System.out.println("Scenario Arrival event is being handled");
				currentEventToBeHandled.scenarioArrival();
				break;

			case SOFTWARE_TASK_STARTS:
				//System.out.println("Software Task Starts event is being handled");
				currentEventToBeHandled.softwareTaskStarts();
				break;

			case SOFTWARE_TASK_ENDS:
				//System.out.println("Software Task Ends event is being handled");
				currentEventToBeHandled.softwareTaskEnds();
				break;

			case HARDWARE_TASK_STARTS:
				//System.out.println("Hardware Task Starts event is being handled");
				currentEventToBeHandled.hardwareTaskStarts();
				break;

			case HARDWARE_TASK_ENDS:
				//System.out.println("Hardware Task Starts event is being handled");
				currentEventToBeHandled.hardwareTaskEnds();
				break;

			case NETWORK_TASK_STARTS:
				//System.out.println("Network Task Starts event is being handled");
				currentEventToBeHandled.networkTaskStarts();
				break;

			case NETWORK_TASK_ENDS:
				//System.out.println("Network Task Ends event is being handled");
				currentEventToBeHandled.networkTaskEnds();
				break;

			case SOFTRES_TASK_STARTS:
				//System.out.println("Softwre Resource Starts event is being handled");
				currentEventToBeHandled.softResourceTaskStarts();
				break;

			case SOFTRES_TASK_ENDS:
				//System.out.println("Software Resource Task Starts event is being handled");
				currentEventToBeHandled.softResourceTaskEnds();
				break;

			case REQUEST_DONE:
				//System.out.println("Request Done event is being handled");
				currentEventToBeHandled.requestCompleted();
				break;

			case WARMUP_ENDS:
				//SimulationParameters.warmupEnabled = false;
				break;

			case COOLDOWN_STARTS:
			//	SimulationParameters.warmupEnabled = true;
				break;

			case SIMULATION_COMPLETE:
				//System.out.println("Simulation Complete event is being handled");
				SimulationParameters.recordIntervalSlotRunTime();
				SimulationParameters.distributedSystemSim.recordCISampleAtTheEndOfSimulation();
				logger.debug("Sim end at : " + SimulationParameters.currTime);
				//System.out.println("Sim TotalReq Processed : " + SimulationParameters.getTotalRequestProcessed());
				return;

			default:
				break;
			}
		}
	}

	/**
	 * This function generates random requests for each of the scenarios(given their arrival rates). We assume right now that the arrival rates are
	 * Poisson(lambda). So inter arrival rates will be Exp(1/lambda). Each scenario will have separate arrival rates. And we will generate one
	 * requests of each scenario and add to the event list A request is generated for each scenario
	 */
	public void generateRequestsForOpenSystem() throws Exception {

		// First of all set the time for which this arrival rate will be consider if workload type is cyclic
		if (ModelParameters.isWorkloadTypeSet) {
			if (ModelParameters.arrivalRateCount != ModelParameters.intervalSlotCount) {
				System.out.println("Please check the pair of rate and associate interval ");
				System.exit(1);
			}
			double eventTime = SimulationParameters.currTime + SimulationParameters.getCurrentSlotLength();
			Event ev = new Event(eventTime, EventType.ARRIVAL_RATE_CHANGES);
			SimulationParameters.offerEvent(ev);
		}
		ExponentialDistribution exp = new ExponentialDistribution();

		// we will have to generate requests for each scenario
		for (Scenario sc : SimulationParameters.distributedSystemSim.scenarios) {
			// now we generate request
			if (sc.getArateToScenario() > 0) {
				double interArrivalTime = exp.nextExp(1 / sc.getArateToScenario());
				generateScenarioArrivalEvent((ScenarioSim)sc, interArrivalTime);
				logger.debug("scenario name: " + sc.getName() + "\t\t  scenario arrival time: " + interArrivalTime);
			}
		}

		// If device type is powermanaged then create events and add then to global eventList: rakesh
		for (Machine h : SimulationParameters.distributedSystemSim.pms) {
			PhysicalMachineSim hs = (PhysicalMachineSim) h;
			for (Device d : hs.devices) {
				DeviceSim ds = (DeviceSim) d;
				if (ds.isDevicePowerManaged) {
					generateDeviceAssociatedEvents(hs, ds);
				}
			}
		}
	}

	// Generate request for closed system. A request is generated for each user
	// added by akhila
	public void generateRequestsForClosedSystem() throws Exception {
		if (ModelParameters.isWorkloadTypeSet) {
			if (ModelParameters.numberofUsersCount != ModelParameters.intervalSlotCount) {
				System.out.println("Please check the pair of users and associate interval ");
				System.exit(1);
			}
			double event_time = SimulationParameters.currTime + SimulationParameters.getCurrentSlotLength();
			
			Event ev = new Event(event_time, EventType.NO_OF_USERS_CHANGES);
			SimulationParameters.offerEvent(ev);
			//Find the max users that this workload is using when workload is cyclic 
			double musers = findMaxUsers(ModelParameters.noOfUsersCyclic);
			ModelParameters.setMaxUsers(musers);
		}

		double noOfUsers = ModelParameters.getNumberOfUsers();
		for (int i = 0; i < noOfUsers; i++) {

			double interArrivalTimeNext = ModelParameters.getThinkTime().nextRandomVal(1);

			// picks a scenario randomly: Login, Send, Read, Delete
			ScenarioSim sce = SimulationParameters.getRandomScenarioSimBasedOnProb();

			generateScenarioArrivalEvent(sce, interArrivalTimeNext);
			logger.debug("scenario name: " + sce + "\t scenario_ID: " + i + "\t\t  scenario arrival time: " + interArrivalTimeNext);
            //System.out.println("No of users: "  + i + " scenario name: " + sce.getName() + "\t scenario_ID: " + i + "\t\t  scenario arrival time: " + interArrivalTimeNext);
		}

		// If device type id powermanaged then create events and add then to global eventList: rakesh
		for (Machine h : SimulationParameters.distributedSystemSim.pms) {
			PhysicalMachineSim hs = (PhysicalMachineSim) h;
			for (Device d : hs.devices) {
				DeviceSim ds = (DeviceSim) d;
				if (ds.isDevicePowerManaged) {
					generateDeviceAssociatedEvents(hs, ds);
				}
			}
		}
	}

	void generateDeviceAssociatedEvents(PhysicalMachineSim host, DeviceSim device) { //added by rakesh
		/***
		 * TODO PONDER Rakesh: what if 2 different types of cpu(heterogenous) present on a same host How service-time will be affected
		 ***/
		// add event to event list
		// probeIntervalDownFactor = 1 for ONDEMAND; it can have different value for CONSERVATIVE
		// System.out.println("Prob invent is generated");
		double event_time = device.deviceProbeInterval;
		Event ev = new Event(event_time, EventType.DEVICE_PROBE, host, device);

		// if (SimParams.cpu_freq_event_list.offer(ev) == false)
		SimulationParameters.offerEvent(ev);
	}

	/** this is where we create the request and add it to requestList */
	void generateScenarioArrivalEvent(ScenarioSim sceName, double time) {

		logger.debug("reqID: " + SimulationParameters.reqIdGenerator + " Time : " + time);
		//System.out.println("reqID: " + SimulationParameters.reqIdGenerator + " Time : " + time);
		Request req = new Request(SimulationParameters.reqIdGenerator++, sceName, time);
		// req.scenarioArrivalTime = time;

		/***** All the scenarios are added into request list here *****/
		SimulationParameters.addRequest(req);

		// this is where we create the corresponding arrival event
		// and add it to eventList
		Event ev = new Event(time, EventType.SCENARIO_ARRIVAL, req);

		// change lastscenarioarrivaltime only if scenario arrival event have greater value
		if (SimulationParameters.lastScenarioArrivalTime < ev.timestamp) {
			SimulationParameters.lastScenarioArrivalTime = ev.timestamp;
		}

		SimulationParameters.offerEvent(ev);
	}

	/**
	 * Calculate the maximum users in entire cycle... this is the value upto which we have to invoke users... added by yogesh
	 */
	double findMaxUsers(Variable[] users) {
		double maxUsers = 0.0;
		for (int i = 0; i < ModelParameters.intervalSlotCount; i++) {
			if (maxUsers < users[i].value) {
				maxUsers = users[i].value;
			}
		}
		return maxUsers;
	}
}
