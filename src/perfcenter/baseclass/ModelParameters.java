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

import perfcenter.baseclass.enums.SolutionMethod;
import perfcenter.baseclass.enums.SystemType;
import perfcenter.baseclass.enums.Warnings;
import perfcenter.simulator.SimulationParameters;

/**
 * All model input parameters in one class.
 * 
 * This class is also used to keep some intermediate variables.
 * @author  akhila
 */

public class ModelParameters {

	/** Method of solving the distributed system's layered queueing network.
	 * 
	 *  This can be simulation or analytical.
	 */
	static SolutionMethod solutionMethod = SolutionMethod.NONE;
	
	/** System type is the nature of the workload in the system. This can be open or closed.
	 * 
	 *  The current workload details does not allow mixing both of these modes, but in simulation
	 *  mode this can be implemented in future.
	 */
	private static SystemType systemType = SystemType.NONE;
	
	/**
	 * Decides whether to validate the system for undeployed hosts / servers before solving it.
	 * 
	 * A value of DISABLE would switch off this checking.
	 */
	private static Warnings warnings = Warnings.NONE;
	
	/** 
	 * Total number of users / requests in a closed system, either being processed, or waiting, or thinking.
	 */
	private static Variable numberOfUsers; // this variable is used when type is closed
	
	/**
	 * Arrival rate to the system, in case of open system.
	 */
	public static Variable arrivalRate;
	
	/**
	 * Time for which the requests think. This is essentially outside the system.
	 * 
	 * This is used to determine when to generate the next request when one request exists the system.
	 */
	private static Distribution thinkTime; // this variable is used when type is closed
	
	/**
	 * Timeout value indicates the time after which end-user would stop waiting for the reply.
	 * 
	 * Since the user would stop waiting, any processing done beyond this time is considered bad.
	 * This is essential in computing the badput, goodput etc of the system.
	 */
	private static Distribution timeout;
	
	/**
	 * Probability with which the user would send the same request back to the system, as a retry attempt after timing out.
	 * 
	 * When a request times out, the user can choose to just leave, and not issue the request again, or the request
	 * can be reissued for re-processing. This variable is the probability of retrying and sending the request again.
	 */
	private static Variable retryProbabilty = new Variable("", 0);
	
	/**
	 * Output would be redirected to this file also. This does not work for all classes, and
	 * needs some improvement in the way its implemented. Low priority.
	 * 
	 * Way-around is to get all the output on the console and manually redirecting it to a file.
	 */
	static String outputFileString = "";
	
	/**
	 * Total number of requests which will be simulated.
	 * 
	 * If not specified, this defaults to 10000; 
	 */
	private static double totalNumberOfRequests = 10000;
	
	/**
	 * Time for which the simulation should be run.
	 * 
	 * This parameter is considered if and only if the total number of requests are not specified in the input file.
	 * Its value defaults to 100 time units.
	 */
	private static Variable simulationEndTime = new Variable("", 100);
	
	/**
	 * Flag indicating if input file specified the total number of requests. Defaults to false.
	 */
	private static boolean totalNumberOfRequestEnabled = false;
	
	/**
	 * Flag indicating if input file specified the total time to run simulation. Defaults to false.
	 */
	private static boolean simulationEndTimeEnabled = false;
	
	/**
	 * Flag indicating if confidence intervals are enabled in the input. Defaults to false.
	 */
	static boolean confIntervalsEnabled = false;
	/**
	 * Number of replications to run if the confidence interval is true. Defaults to 1.
	 */
	private static double numberOfReplications = 1;
	
	/**
	 * Total number of requests to be considered in warm-up phase.
	 * 
	 * This is considered only if the total number of requests is specified in the input file. 
	 */
	private static double startUpSampleNumber = 1000;
	/**
	 * Total number of requests to be considered in the warm-down phase.
	 * 
	 * This is considered only if the total number of requests is specified in the input file.
	 */
//	private static double coolDownSampleNumber = 1000; //FIXME fix startup and cooldown
	
	/**
	 * Confidence interval level of queue length metric. Defaults to 0.95.
	 */
	private static double qlenCILevel = 0.95;
	/**
	 * Confidence interval level of utilization metric. Defaults to 0.95.
	 */
	private static double utilCILevel = 0.95;
	/**
	 * Confidence interval level of throughput metric. Defaults to 0.95.
	 */
	private static double tputCILevel = 0.95;
	/**
	 * Confidence interval level of response time metric. Defaults to 0.95.
	 */
	private static double resptCILevel = 0.95;
	
	//RIGHTNOW: resume from here
	/** arrival rates for cyclic workload */
	public static Variable[] arrivalRates = new Variable[1000]; // ARCHITECTURE: change this to ArrayList
	
	/** number of users for cyclic workload */
	public static Variable[] numberofUsers = new Variable[1000];// ARCHITECTURE: change this to ArrayList
	
	/** slot durations of cyclic workload */
	public static Variable[] intervalSlotDurations = new Variable[1000];// ARCHITECTURE: change this to ArrayList
	
	/** Number of entries in the array arrivalRates */
	public static int arrivalRateCount = 0;
	
	/** Number of entries in the array intervalSlotDurations */
	public static int intervalSlotCount = 0;
	
	/** Number of entries in the array numberofUsers */ 
	public static int numberofUsersCount = 0;

	/** Maximum number of users in the cyclic workload load specification.
	 * 
	 * This is used by request generation logic to reuse the old requests.
	 */
	private static double maxUsers = 0.0;
	
	/** Maximum number of retries that a user might do, before she would give up irrespective of the retry probability.
	 * 
	 *  This defaults to 5.
	 */
	private static Variable maxRetry = new Variable("", 5);

	/** Flags used to indicate if system is modified since last solution. */
	public static boolean isModified = true;
	
	/** Flag to indicate if system is validated. */
	public static boolean isValidated = false;
	
	/** Flag to indicate if timeouts are enabled, and should be simulated. */
	public static boolean timeoutEnabled = false;
	
	/** All the values read from input file is stored into this variable */
	public static DistributedSystem inputDistributedSystem;
	
	/**
	 * The results are stored in this variable. Output uses this variable to read and print the results.
	 */
	public static DistributedSystem resultDistributedSystem;
	public static boolean isWorkloadTypeSet = false;

	/** Before the first run if any of variables are not set then default is set. */
	public static void setDefault() {
		if (solutionMethod == SolutionMethod.NONE) {
			solutionMethod = SolutionMethod.ANALYTICAL;
		}
		if (systemType == SystemType.NONE) {
			systemType = SystemType.OPEN;
		}
		if (timeout == null) {
			timeout = new Distribution("exp", 0.04);
		}

		if (systemType == SystemType.CLOSED) {
			if (numberOfUsers == null) {
				numberOfUsers = new Variable("local", 10);
			}
			if (thinkTime == null) {
				thinkTime = new Distribution("exp", 0.02);
			}
		}
		if (systemType == SystemType.OPEN) {
			if (arrivalRate == null) {
				arrivalRate = new Variable("local", 10);

			} else if (arrivalRate.value < 0) {
				throw new Error("Arrival rate cannot be negative value");
			}

		}

		// if confidence interval is enabled then number of replication should
		// be
		// at least two
		if (confIntervalsEnabled == true) {
			if (getNumberOfReplications() <= 1) {
				numberOfReplications = 2.0;
			}
		}

	}

	public static void setSolutionMethod(SolutionMethod m) {
		solutionMethod = m;
	}

	public static void setWarnings(Warnings w) {
		warnings = w;
	}

	public static void setSystemType(SystemType t) {
		systemType = t;
	}

	public static void addNoOfUsers(Variable var) {
		numberOfUsers = var;
		numberofUsers[numberofUsersCount++] = var;
		intervalSlotCount = 1;
	}

	public static void addArrivalRate(Variable var) {
		arrivalRate = var;
		arrivalRates[arrivalRateCount++] = var;
		intervalSlotCount = 1;
	}

	public static void addArrivalRates(Variable var) {
		arrivalRates[arrivalRateCount++] = var;
		arrivalRate = arrivalRates[0];
		isWorkloadTypeSet = true;
	}

	public static void addNumberOfUsers(Variable var) {
		numberofUsers[numberofUsersCount++] = var;
		numberOfUsers = numberofUsers[0];
		isWorkloadTypeSet = true;
	}

	public static void addIntervalSlots(Variable var) {
		intervalSlotDurations[intervalSlotCount++] = var;
	}

	public static void addThinkTime(Distribution d) {
		thinkTime = d;
	}

	public static void addTimeout(Distribution d) {
		timeout = d;
		timeoutEnabled = true;
	}

	public static void addNoOfRequests(double var) {
		totalNumberOfRequests = var;
		totalNumberOfRequestEnabled = true;
	}

	public static void addRetryProbability(Variable var) {
		retryProbabilty = var;
	}

	public static void setConfIntervalsEnabled(boolean var) {
		confIntervalsEnabled = var;
	}

	public static void setMaxUsers(double musers) {
		maxUsers = musers;
	}

	public static void addNoOfReplications(double var) {
		numberOfReplications = var;
	}

	public static void addStartUpSampleNo(double var) {
		startUpSampleNumber = var;
		SimulationParameters.warmupEnabled = true;
	}

//	public static void addCoolDownSampleNo(double var) {
//		coolDownSampleNumber = var;
//	}

	public static void addQlencilevel(double var) {
		qlenCILevel = var;
	}

	public static void addUtilcilevel(double var) {
		utilCILevel = var;
	}

	public static void addTputcilevel(double var) {
		tputCILevel = var;
	}

	public static void addResptcilevel(double var) {
		resptCILevel = var;
	}

	public static void addMaxRetry(Variable var) {
		maxRetry = var;
	}

	public static void addOutputFile(String var) {
		outputFileString = var;
	}

	public static void addSimulationEndTime(Variable var) {
		simulationEndTime = var;
		simulationEndTimeEnabled = true;
	}

	public static SolutionMethod getSolutionMethod() {
		return solutionMethod;
	}

	public static Warnings getWarnings() {
		return warnings;
	}

	public static SystemType getSystemType() {
		return systemType;
	}

	public static double getNumberOfUsers() {
		return numberOfUsers.getValue();
	}

	public static double getNumberOfUserss(int slot) {
		slot = (slot == -1) ? intervalSlotCount - 1 : slot; 
		return numberofUsers[slot].getValue();
	}
	
	public static Distribution getThinkTime() {
		return thinkTime;
	}

	public static Distribution getTimeout() {
		return timeout;
	}

	public static double getTotalNumberOfRequests() {
		return totalNumberOfRequests;
	}

	public static double getArrivalRate() {
		return arrivalRate.value;
	}
	
	public static Variable getArrivalRate(int slot) {
		slot = (slot == -1) ? intervalSlotCount - 1 : slot; 
		return arrivalRates[slot];
	}

	public static double getMaxRetry() {
		return maxRetry.value;
	}

	public static boolean getSimulationEndTimeEnabled() {
		return simulationEndTimeEnabled;
	}

	public static boolean getTotalNumberOfRequestEnabled() {
		return totalNumberOfRequestEnabled;
	}

	/**
	 * Returns the number of simulations to run to calculate confidence interval.
	 * 
	 * {@link #numberOfReplications numberOfReplications}
	 */
	public static double getNumberOfReplications() {
		return numberOfReplications;
	}

	public static double getStartUpSampleNumber() {
		return startUpSampleNumber;
	}

//	public static double getCoolDownSampleNumber() {
//		return coolDownSampleNumber;
//	}

	public static double getQlenCILevel() {
		return qlenCILevel;
	}

	public static double getUtilCILevel() {
		return utilCILevel;
	}

	public static double getTputCILevel() {
		return tputCILevel;
	}

	public static double getResptCILevel() {
		return resptCILevel;
	}

	public static double getRetryProbability() {
		return retryProbabilty.value;
	}

	public static double getSimulationEndTime() {
		return simulationEndTime.value;
	}

	public static double getMaxUsers() {
		return maxUsers;
	}
}