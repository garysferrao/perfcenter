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
package perfcenter.output;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collections;

import org.apache.log4j.Logger;

import perfcenter.analytical.PerfAnalytic;
import perfcenter.baseclass.Device;
import perfcenter.baseclass.Host;
import perfcenter.baseclass.LanLink;
import perfcenter.baseclass.ModelParameters;
import perfcenter.baseclass.SoftServer;
import perfcenter.baseclass.VirtualResource;
import perfcenter.baseclass.enums.SolutionMethod;
import perfcenter.baseclass.enums.SystemType;
import perfcenter.baseclass.enums.Warnings;
import perfcenter.baseclass.exception.DeviceNotFoundException;
import perfcenter.simulator.PerfSim;
import perfcenter.simulator.SimulationParameters;
import perfcenter.simulator.queue.QServerInstance;
import perfcenter.simulator.queue.QueueSim;
import static perfcenter.baseclass.ModelParameters.resultDistributedSystem;
/**
 * This executes the PerfCenter(simulation/analytical) and fetches results from the appropriate places.
 * 
 * @author akhila
 */
public class Output {

	/** Has list of resources(queues) and their utilization */
	public ArrayList<BottleNeck> bottleneck = new ArrayList<BottleNeck>();
	Logger logger = Logger.getLogger("Output");

	public Output() throws Exception {
		// Analysis is done only if input configuration is changed
		if (ModelParameters.isModified == true) {
			// set the default values it some some them were not set in input file
			ModelParameters.setDefault();

			// check parameters like sum of prob of all scenarios should be one
			ModelParameters.inputDistributedSystem.checkParameters();

			// if its open. set the scenario arrival rate
			if (ModelParameters.getSystemType() == SystemType.OPEN) {
				ModelParameters.inputDistributedSystem.setScenarioArrivalRate();
			}

			// validate input
			if (ModelParameters.isValidated == false && !(ModelParameters.getWarnings() == Warnings.DISABLE)) {
				ModelParameters.inputDistributedSystem.validate();
				ModelParameters.isValidated = true;
			}

			// get type of analysis
			if (ModelParameters.getSolutionMethod() == SolutionMethod.SIMULATION) {
				// analysis for simulation
				PerfSim ps = new PerfSim(ModelParameters.inputDistributedSystem);
				resultDistributedSystem = ps.performSimulation();
			} else {
				// analysis for analytical
				PerfAnalytic pa = new PerfAnalytic(ModelParameters.inputDistributedSystem);
				resultDistributedSystem = pa.performAnalysis();
			}
			ModelParameters.isModified = false;
		}
	}

	public String findScenarioResponseTime(int slot, String scenarioname) {
		if (scenarioname.compareToIgnoreCase("eters") == 0) {
			return resultDistributedSystem.overallResponseTime.toString(slot);
		} else {
			return resultDistributedSystem.getScenario(scenarioname).averageResponseTime.toString(slot);
		}
	}

	public String findScenarioThroughput(int slot, String scenarioname) {
		if (scenarioname.compareToIgnoreCase("eters") == 0) {
			return resultDistributedSystem.overallThroughput.toString(slot);
		} else {
			return resultDistributedSystem.getScenario(scenarioname).averageThroughput.toString(slot);
		}
	}

	public String findScenarioBadput(int slot, String scenarioname) {
		if (scenarioname.compareToIgnoreCase("eters") == 0) {
			return resultDistributedSystem.overallBadput.toString(slot);
		} else {
			return resultDistributedSystem.getScenario(scenarioname).averageBadput.toString(slot);
		}
	}

	public String findScenarioGoodput(int slot, String scenarioname) {
		if (scenarioname.compareToIgnoreCase("eters") == 0) {
			return resultDistributedSystem.overallGoodput.toString(slot);
		} else {
			return resultDistributedSystem.getScenario(scenarioname).averageGoodput.toString(slot);
		}
	}

	public String findScenarioBuffTimeout(int slot, String scenarioname) {
		if (scenarioname.compareToIgnoreCase("eters") == 0) {
			return resultDistributedSystem.overallBuffTimeout.toString(slot);
		} else {
			return resultDistributedSystem.getScenario(scenarioname).buffTimeout.toString(slot);
		}
	}

	public String findScenarioDropRate(int slot, String scenarioname) {
		if (scenarioname.compareToIgnoreCase("eters") == 0) {
			return resultDistributedSystem.overallDroprate.toString(slot);
		} else {
			return resultDistributedSystem.getScenario(scenarioname).dropRate.toString(slot);
		}
	}

	public String findScenarioArrivalRate(int slot, String scenarioname) {
		if (scenarioname.compareToIgnoreCase("eters") == 0) {
			return resultDistributedSystem.overallArrivalRate.toString(slot);
		} else {
			return resultDistributedSystem.getScenario(scenarioname).arateToScenarioDuringSimulation.toString(slot);
		}
	}

	public String findScenarioBlockProb(int slot, String scenarioname) {
		if (scenarioname.compareToIgnoreCase("eters") == 0) {
			return resultDistributedSystem.overallBlockingProbability.toString(slot);
		} else {
			return resultDistributedSystem.getScenario(scenarioname).blockingProb.toString(slot);
		}
	}

	/** finds respt of device/ link or soft server or virtual resource */
	public String findResponseTime(int slot, String name1, String name2, String name3) throws DeviceNotFoundException, Exception {
		if (resultDistributedSystem.isLan(name1) == true) {
			return resultDistributedSystem.getLink(name1, name2).getResourceQueue(name1, name2).averageResponseTime.toString(slot);
		} else if (resultDistributedSystem.isHost(name1) == true) {
			Host host = resultDistributedSystem.getHost(name1);
			if (name3 == ")") {
				if (host.isDeviceDeployed(name2)) {
					if (host.isDeviceDeployed(name2) == false) {
						throw new Error("second parameter to respt \"" + name2 + "\" is not device on host");
					}
					return (host.getDevice(name2)).getResourceQueue().averageResponseTime.toString(slot);
				} else if (host.isServerDeployed(name2)) {
					return (host.getServer(name2)).getResourceQueue().averageResponseTime.toString(slot);
				} else if (host.isVirtualResourceDeployed(name2)) {
					if (host.isVirtualResourceDeployed(name2) == false) {
						throw new Error("second parameter to respt \"" + name2 + "\" is not deployed on host");
					}
					return host.getVirtualRes(name2).getResourceQueue().averageResponseTime.toString(slot);
				} else {
					throw new Error("second parameter to respt \"" + name2 + "\" is not deployed on server");
				}
			} else {// for retreiving perServer level value on device

				if (host.isServerDeployed(name2)) {

					if (host.isDeviceDeployed(name3)) {
						String respt = host.getDevice(name3).resourceQueue.averageResponseTime.toString(slot, name2);
						return (respt);
					} else {
						throw new Error("Third parameter to respt \"" + name3 + "\" is either not Device or not deployed on " + name1);
					}
				} else {
					throw new Error("Second parameter to respt \"" + name2 + "\" is either not a server or not deployed on " + name1);
				}

			}
		}
		throw new Error("first parameter to respt \"" + name1 + "\" is not lan or host");
	}

	/** finds tput of device/ link or soft server or virtual resource */
	public String findThroughput(int slot, String name1, String name2, String name3) throws DeviceNotFoundException, Exception {
		// System.out.println("Reaching here with name1 : " + name1 +
		// "   name2 : " + name2);
		if (resultDistributedSystem.isLan(name1) == true) {
			return resultDistributedSystem.getLink(name1, name2).getResourceQueue(name1, name2).averageThroughput.toString(slot);
		} else if (resultDistributedSystem.isHost(name1) == true) {
			Host host = resultDistributedSystem.getHost(name1);
			if (name3 == ")") {
				if (host.isDeviceDeployed(name2)) {
					return host.getDevice(name2).resourceQueue.averageThroughput.toString(slot);
				} else if (host.isServerDeployed(name2)) {
					return host.getServer(name2).resourceQueue.averageThroughput.toString(slot);
				} else if (host.isVirtualResourceDeployed(name2)) {
					return host.getVirtualRes(name2).resourceQueue.averageThroughput.toString(slot);
				} else {
					throw new Error("second parameter to tput \"" + name2 + "\" is not deployed on server");
				}
			} else {// for retreiving perServer level value on device

				if (host.isServerDeployed(name2)) {

					if (host.isDeviceDeployed(name3)) {
						String tput = host.getDevice(name3).resourceQueue.averageThroughput.toString(slot, name2);
						return (tput);
					} else {
						throw new Error("Third parameter to tput\"" + name3 + "\" is either not Device or not deployed on " + name1);
					}
				} else {
					throw new Error("Second parameter to tput \"" + name2 + "\" is either not a server or not deployed on " + name1);
				}
			}
		}
		throw new Error("first parameter to tput \"" + name1 + "\" is not lan or host");
	}

	/** finds arate of device/ link or soft server or virtual resource */
	public String findArrivalRate(int slot, String name1, String name2, String name3) throws DeviceNotFoundException, Exception {
		if (resultDistributedSystem.isLan(name1) == true) {
			return resultDistributedSystem.getLink(name1, name2).getResourceQueue(name1, name2).averageArrivalRate.toString(slot);
		} else if (resultDistributedSystem.isHost(name1) == true) {
			Host host = resultDistributedSystem.getHost(name1);
			if (name3 == ")") {
				if (host.isDeviceDeployed(name2)) {
					if (host.isDeviceDeployed(name2) == false) {
						throw new Error("second parameter to arate \"" + name2 + "\" is not deployed on host");
					}
					return host.getDevice(name2).resourceQueue.averageArrivalRate.toString(slot);
				} else if (host.isServerDeployed(name2)) {
					if (host.isServerDeployed(name2) == false) {
						throw new Error("second parameter to arate \"" + name2 + "\" is not deployed on host");
					}
					return host.getServer(name2).resourceQueue.averageArrivalRate.toString(slot);
				} else if (host.isVirtualResourceDeployed(name2)) {
					if (host.isVirtualResourceDeployed(name2) == false) {
						throw new Error("second parameter to arate \"" + name2 + "\" is not deployed on host");
					}
					return host.getVirtualRes(name2).resourceQueue.averageArrivalRate.toString(slot);
				} else {
					throw new Error("second parameter to arate \"" + name2 + "\" is not deployed on server");
				}
			} else {// for retreiving perServer level value on device

				if (host.isServerDeployed(name2)) {

					if (host.isDeviceDeployed(name3)) {
						String arrate = host.getDevice(name3).resourceQueue.averageArrivalRate.toString(slot, name2);
						return (arrate);
					} else {
						throw new Error("Third parameter to arate \"" + name3 + "\" is either not Device or not deployed on " + name1);
					}
				} else {
					throw new Error("Second parameter to arate \"" + name2 + "\" is either not a server or not deployed on " + name1);
				}

			}
		}
		throw new Error("first parameter to arate \"" + name1 + "\" is not lan or host");
	}

	/** finds block prob of device/ link or soft server or virtual resource */
	public String findBlockProb(int slot, String name1, String name2, String name3) throws DeviceNotFoundException, Exception {
		if (resultDistributedSystem.isLan(name1) == true) {
			return resultDistributedSystem.getLink(name1, name2).getResourceQueue(name1, name2).blockingProbability.toString(slot);
		} else if (resultDistributedSystem.isHost(name1) == true) {
			Host host = resultDistributedSystem.getHost(name1);
			if (name3 == ")") {
				if (host.isDeviceDeployed(name2)) {
					if (host.isDeviceDeployed(name2) == false) {
						throw new Error("second parameter to blockprob \"" + name2 + "\" is not deployed on host");
					}
					return host.getDevice(name2).getResourceQueue().blockingProbability.toString(slot);
				} else if (host.isServerDeployed(name2)) {
					if (host.isServerDeployed(name2) == false) {
						throw new Error("second parameter to blockprob \"" + name2 + "\" is not deployed on host");
					}
					return host.getServer(name2).getResourceQueue().blockingProbability.toString(slot);
				} else if (host.isVirtualResourceDeployed(name2)) {
					if (host.isVirtualResourceDeployed(name2) == false) {
						throw new Error("second parameter to blockprob \"" + name2 + "\" is not deployed on host");
					}
					return host.getVirtualRes(name2).getResourceQueue().blockingProbability.toString(slot);
				} else {
					throw new Error("second parameter to blockprob \"" + name2 + "\" is not deployed on server");
				}
			} else {// for retreiving perServer level value on device

				if (host.isServerDeployed(name2)) {

					if (host.isDeviceDeployed(name3)) {
						String blockProb = host.getDevice(name3).resourceQueue.blockingProbability.toString(slot, name2);
						return blockProb;
					} else {
						throw new Error("Third parameter to blockprob \"" + name3 + "\" is either not Device or not deployed on " + name1);
					}
				} else {
					throw new Error("Second parameter to blockprob \"" + name2 + "\" is either not a server or not deployed on " + name1);
				}

			}

		}
		throw new Error("first parameter to blockprob \"" + name1 + "\" is not lan or host");
	}

	/** finds avgservt of device/ link or softserver or virtual resource */
	public String findAvgServiceTime(int slot, String name1, String name2, String name3) throws DeviceNotFoundException, Exception {
		if (resultDistributedSystem.isLan(name1) == true) {
			return resultDistributedSystem.getLink(name1, name2).getResourceQueue(name1, name2).averageServiceTime.toString(slot);
		} else if (resultDistributedSystem.isHost(name1) == true) {
			Host host = resultDistributedSystem.getHost(name1);
			if (name3 == ")") {
				if (host.isDeviceDeployed(name2)) {
					if (host.isDeviceDeployed(name2) == false) {
						throw new Error("second parameter to avgservt \"" + name2 + "\" is not deployed on host");
					}
					return host.getDevice(name2).resourceQueue.averageServiceTime.toString(slot);
				} else if (host.isServerDeployed(name2)) {
					if (host.isServerDeployed(name2) == false) {
						throw new Error("second parameter to avgservt \"" + name2 + "\" is not deployed on host");
					}
					return host.getServer(name2).resourceQueue.averageServiceTime.toString(slot);
				} else if (host.isVirtualResourceDeployed(name2)) {
					if (host.isVirtualResourceDeployed(name2) == false) {
						throw new Error("second parameter to avgservt \"" + name2 + "\" is not deployed on host");
					}
					return host.getVirtualRes(name2).resourceQueue.averageServiceTime.toString(slot);
				} else {
					throw new Error("second parameter to avgservt \"" + name2 + "\" is not deployed on server");
				}
			} else {// for retreiving perServer level value on device

				if (host.isServerDeployed(name2)) {

					if (host.isDeviceDeployed(name3)) {
						String servt = host.getDevice(name3).resourceQueue.averageServiceTime.toString(slot, name2);
						return servt;
					} else {
						throw new Error("Third parameter to avgservt \"" + name3 + "\" is either not Device or not deployed on " + name1);
					}
				} else {
					throw new Error("Second parameter to avgservt \"" + name2 + "\" is either not a server or not deployed on " + name1);
				}

			}
		}
		throw new Error("first parameter to avgservt \"" + name1 + "\" is not lan or host");
	}

	/** finds waitt of device/ link or softserver or virtual resource */
	public String findWaitingTime(int slot, String name1, String name2, String name3) throws DeviceNotFoundException, Exception {
		if (resultDistributedSystem.isLan(name1) == true) {
			return resultDistributedSystem.getLink(name1, name2).getResourceQueue(name1, name2).averageWaitingTime.toString(slot);
		} else if (resultDistributedSystem.isHost(name1) == true) {
			Host host = resultDistributedSystem.getHost(name1);
			if (name3 == ")") {
				if (host.isDeviceDeployed(name2)) {
					return host.getDevice(name2).resourceQueue.averageWaitingTime.toString(slot);
				} else if (host.isServerDeployed(name2)) {
					if (host.isServerDeployed(name2) == false) {
						throw new Error("second parameter to waitt \"" + name2 + "\" is not deployed on host");
					}
					return host.getServer(name2).resourceQueue.averageWaitingTime.toString(slot);
				} else if (host.isVirtualResourceDeployed(name2)) {
					if (host.isVirtualResourceDeployed(name2) == false) {
						throw new Error("second parameter to waitt \"" + name2 + "\" is not deployed on host");
					}
					return host.getVirtualRes(name2).resourceQueue.averageWaitingTime.toString(slot);
				} else {
					throw new Error("second parameter to waitt \"" + name2 + "\" is not deployed on server");
				}
			} else {// for retreiving perServer level value on device

				if (host.isServerDeployed(name2)) {

					if (host.isDeviceDeployed(name3)) {
						String waitTime = host.getDevice(name3).resourceQueue.averageWaitingTime.toString(slot, name2);
						return (waitTime==null?"0":waitTime);
					} else {
						throw new Error("Third parameter to waitt \"" + name3 + "\" is either not Device or not deployed on " + name1);
					}
				} else {
					throw new Error("Second parameter to waitt \"" + name2 + "\" is either not a server or not deployed on " + name1);
				}

			}
		}
		throw new Error("first parameter to waitt \"" + name1 + "\" is not lan or host");
	}

	/** finds util of device/ link or softserver or virtual resource */
	public String findUtilization(int slot, String name1, String name2, String name3) throws DeviceNotFoundException, Exception {
		if (resultDistributedSystem.isLan(name1) == true) {
			return resultDistributedSystem.getLink(name1, name2).getResourceQueue(name1, name2).averageUtilization.toString(slot);
		} else if (resultDistributedSystem.isHost(name1) == true) {
			Host host = resultDistributedSystem.getHost(name1);
			if (name3 == ")") {// third parameter is not specified in utilization function
				if (host.isServerDeployed(name2)) {
					return host.getServer(name2).resourceQueue.averageUtilization.toString(slot);
				} else if (host.isDeviceDeployed(name2)) {
					return host.getDevice(name2).resourceQueue.averageUtilization.toString(slot);
				} else if (host.isVirtualResourceDeployed(name2)) {
					return host.getVirtualRes(name2).resourceQueue.averageUtilization.toString(slot);
				} else {
					throw new Error("second parameter to util \"" + name2 + "\" is not deployed on server");
				}

			} else {// third parameter is specified in utilzation function

				if (host.isServerDeployed(name2)) {

					if (host.isDeviceDeployed(name3)) {
						String util = host.getDevice(name3).resourceQueue.averageUtilization.toString(name2);// nadeesh
																												// addded
						return util;
					} else {
						throw new Error("Third parameter to util \"" + name3 + "\" is either not Device or not deployed on " + name1);
					}
				} else {
					throw new Error("Second parameter to util \"" + name2 + "\" is either not a server or not deployed on " + name1);
				}
			}
		} else {
			throw new Error("first parameter to util \"" + name1 + "\" is not lan or host");
		}
	}

	/** finds qparms of device/ link or softserver or virtual resource */
	public String findQueParameters(String name1, String name2) throws DeviceNotFoundException, Exception {
		if (resultDistributedSystem.isLan(name1) == true) {
			return findLinkQueParameters(name1, name2);
		} else if (resultDistributedSystem.isHost(name1) == true) {
			Host host = resultDistributedSystem.getHost(name1);
			if (host.isDeviceDeployed(name2)) {
				return findDeviceQueParameters(name1, name2);
			} else if (host.isServerDeployed(name2)) {
				return findServerQueParameters(name1, name2);
			} else if (host.isVirtualResourceDeployed(name2)) {
				return findVirtualResQueParameters(name1, name2);
			} else {
				throw new Error("second parameter to qparms \"" + name2 + "\" is not deployed on server");
			}
		}
		throw new Error("first parameter to qparms \"" + name1 + "\" is not lan or host");
	}

	/** finds qlen of device/ link or softserver or virtual resource */
	public String findQueueLength(int slot, String name1, String name2,String name3) throws Exception, DeviceNotFoundException {
		if (resultDistributedSystem.isLan(name1) == true) {
			return resultDistributedSystem.getLink(name1, name2).getResourceQueue(name1, name2).averageQueueLength.toString(slot);
		} else if (resultDistributedSystem.isHost(name1) == true) {
			Host host = resultDistributedSystem.getHost(name1);
			if (name3 == ")") // third parameter is not specified in utilization
			// function
			{
				if (host.isDeviceDeployed(name2)) {
					return host.getDevice(name2).resourceQueue.averageQueueLength.toString(slot);
				} else if (host.isServerDeployed(name2)) {
					return host.getServer(name2).resourceQueue.averageQueueLength.toString(slot);
				} else if (host.isVirtualResourceDeployed(name2)) {
					return host.getVirtualRes(name2).resourceQueue.averageQueueLength.toString(slot);
				} else {
					throw new Error("second parameter to qlen \"" + name2 + "\" is not deployed on server");
				}
			} else {// third parameter is specified in utilzation function

				if (host.isServerDeployed(name2)) {

					if (host.isDeviceDeployed(name3)) {
						String qlen = host.getDevice(name3).resourceQueue.averageQueueLength.toString(slot, name2);// nadeesh addded
						return qlen;
					} else {
						throw new Error("Third parameter to util \"" + name3 + "\" is either not Device or not deployed on " + name1);
					}
				} else {
					throw new Error("Second parameter to util \"" + name2 + "\" is either not a server or not deployed on " + name1);
				}
			}
		}
		throw new Error("first parameter to qlen \"" + name1 + "\" is not lan or host");
	}

	/** finds *power* consumption of the device */
	public String findPowerConsumption(int slot, String name1, String name2,String name3)  throws DeviceNotFoundException {
			
		if (resultDistributedSystem.isHost(name1) == true) {
			Host host = resultDistributedSystem.getHost(name1);
			if (name3 == ")") // third parameter is not specified in utilization
			// function
			{
				if (host.isDeviceDeployed(name2)) {
					return host.getDevice(name2).resourceQueue.averagePowerConsumed.toString(slot);
				} else if (host.isServerDeployed(name2)) {
					return host.getServer(name2).resourceQueue.averagePowerConsumed.toString(slot);
				} else {
					throw new Error("second parameter to power \"" + name2 + "\" is not deployed on host");
				}
			} else {// third parameter is specified in utilzation function
				if (host.isServerDeployed(name2)) {

					if (host.isDeviceDeployed(name3)) {
						String power = host.getDevice(name3).resourceQueue.averagePowerConsumed.toString(slot, name2);// nadeesh addded
						return power;
					} else {
						throw new Error("Third parameter to power \"" + name3 + "\" is either not Device or not deployed on " + name1);
					}
				} else if(name2.compareToIgnoreCase("_idle")==0){
					if (host.isDeviceDeployed(name3)) {
						String power = host.getDevice(name3).resourceQueue.averagePowerConsumed.toString(slot, name2);// nadeesh addded
						return power;
					} else {
						throw new Error("Third parameter to power \"" + name3 + "\" is either not Device or not deployed on " + name1);
					}
				}else{
					throw new Error("Second parameter to power \"" + name2 + "\" is either not a server or not deployed on " + name1);
				}
			}
		}
		throw new Error("first parameter to power \"" + name1 + "\" is not  host");
		
		
		
	}

	/** finds the *power* delay product */
	public String findPowerDelayProduct(int slot, String hostname, String devicename) throws DeviceNotFoundException {
		Host host = resultDistributedSystem.getHost(hostname);
		if (host.isDeviceDeployed(devicename) == false) {
			throw new Error("second parameter to PDP \" " + devicename + "\" is not deployed on host");
		}
		return host.getDevice(devicename).getResourceQueue().powerDelayProduct.toString(slot);
	}

	/** finds the *power* efficiency */
	public String findPowerEfficiency(int slot, String hostname, String devicename) throws DeviceNotFoundException {
		Host host = resultDistributedSystem.getHost(hostname);
		if (host.isDeviceDeployed(devicename) == false) {
			throw new Error("second parameter to peff \" " + devicename + "\" is not deployed on host");
		}
		return host.getDevice(devicename).getResourceQueue().powerEfficiency.toString(slot);
	}

	/** find the energy consumption per request for specific device */
	public String findTotalEnergyConsumptionperRequest(int slot, String name1, String name2,String name3) throws DeviceNotFoundException {
		if (resultDistributedSystem.isHost(name1) == true) {
			Host host = resultDistributedSystem.getHost(name1);
			if (name3 == ")") // third parameter is not specified in utilization
			// function
			{
				if (host.isDeviceDeployed(name2)) {
					return host.getDevice(name2).resourceQueue.averageEnergyConsumptionPerRequest.toString(slot);
				} else {
					throw new Error("second parameter to power \"" + name2 + "\" is not deployed on host");
				}
			} else {// third parameter is specified in utilzation function

				if (host.isServerDeployed(name2)) {

					if (host.isDeviceDeployed(name3)) {
						String qlen = host.getDevice(name3).resourceQueue.averageEnergyConsumptionPerRequest.toString(slot, name2);// nadeesh addded
						return qlen;
					} else {
						throw new Error("Third parameter to power \"" + name3 + "\" is either not Device or not deployed on " + name1);
					}
				} else {
					throw new Error("Second parameter to power \"" + name2 + "\" is either not a server or not deployed on " + name1);
				}
			}
		}
		throw new Error("first parameter to power \"" + name1 + "\" is not  host");
	}
	
	/** finds average frequency of the device. Makes sense only if device is power managed. */
	public String findAvgFrequency(int slot, String tempname1, String tempname2) throws DeviceNotFoundException {
		if (ModelParameters.resultDistributedSystem.getHost(tempname1).isDeviceDeployed(tempname2) == false) {
			throw new Error("second parameter to freq \"" + tempname2 + "\" is not deployed on host");
		}
		return ModelParameters.resultDistributedSystem.getHost(tempname1).getDevice(tempname2).averageFrequency.toString(slot);
	}

	/** Find Queue parameters of device */
	String findDeviceQueParameters(String hostname, String devicename) throws DeviceNotFoundException {
		Object dev = resultDistributedSystem.getHost(hostname).getDevice(devicename);
		logger.error(" \nQueue Parameters " + hostname + "/" + devicename + ": ");
		logger.error("ArrivalRate " + ((Device) dev).getAverageArrivalRate());
		logger.error("Throughput " + ((Device) dev).getThroughput());
		logger.error("ResponseTime " + ((Device) dev).getAvgResponseTime());
		logger.error("Utilization " + ((Device) dev).getUtilization());
		logger.error("Avg QueueLength " + ((Device) dev).getAvgQueueLength());
		logger.error("Avg WaitingTime " + ((Device) dev).getAvgWaitingTime());
		logger.error("Avg ServiceTime " + ((Device) dev).getAvgServiceTime());
		logger.error("BlockingProbability " + ((Device) dev).getBlockingProbability());
		return "0.0";
	}

	/** find qparms of server */
	String findServerQueParameters(String hostname, String servername) {
		SoftServer serv = resultDistributedSystem.getHost(hostname).getServer(servername);
		logger.error("\nQueue Parameters " + hostname + "/" + servername + ": ");
		logger.error("ArrivalRate " + serv.getAverageArrivalRate());
		logger.error("Throughput " + serv.getThroughput());
		logger.error("ResponseTime " + serv.getAvgResponseTime());
		logger.error("Utilization " + serv.getUtilization());
		logger.error("Avg QueueLength " + serv.getAvgQueueLength());
		logger.error("Avg WaitingTime " + serv.getAvgWaitingTime());
		logger.error("Avg ServiceTime " + serv.getAvgServiceTime());
		logger.error("BlockingProbability " + serv.getBlockingProbability());
		return "0.0";
	}

	/** find qparms of virtual res */
	String findVirtualResQueParameters(String hostname, String vsname) throws Exception {
		VirtualResource vsres = resultDistributedSystem.getHost(hostname).getVirtualRes(vsname);
		logger.error("\nQueue Parameters " + hostname + "/" + vsname + ": ");
		logger.error("ArrivalRate " + vsres.getAverageArrivalRate());
		logger.error("Throughput " + vsres.getThroughput());
		logger.error("ResponseTime " + vsres.getAvgResponseTime());
		logger.error("Utilization " + vsres.getUtilization());
		logger.error("Avg QueueLength " + vsres.getAvgQueueLength());
		logger.error("Avg WaitingTime " + vsres.getAvgWaitingTime());
		logger.error("Avg ServiceTime " + vsres.getAvgServiceTime());
		logger.error("BlockingProbability " + vsres.getBlockingProbability());
		return "0.0";
	}

	/** find qparms of link */
	public String findLinkQueParameters(String lanname1, String lanname2) {
		LanLink lk = resultDistributedSystem.getLink(lanname1, lanname2);
		logger.error("\nQueue Parameters Link" + lk.getName());
		logger.error("ArrivalRate " + lk.getArrRate(lanname1, lanname2));
		logger.error("Throughput " + lk.getThroughput(lanname1, lanname2));
		logger.error("ResponseTime " + lk.getResponseTime(lanname1, lanname2));
		logger.error("Utilization " + lk.getUtilization(lanname1, lanname2));
		logger.error("Avg QueueLength " + lk.getAvgQueueLength(lanname1, lanname2));
		logger.error("Avg WaitingTime " + lk.getAvgWaitingTime(lanname1, lanname2));
		logger.error("Avg ServiceTime " + lk.getAvgServiceTime(lanname1, lanname2));
		logger.error("BlockingProbability " + lk.getBlockingProbability(lanname1, lanname2));
		return "0.0";
	}

	/** print bottleneck resource among devices,soft servers,virtual res and network links.
	 * 
	 * This method needs reverification, and possible fixing. */
	public String findBottleNeck(Double n) {
		for (Host host : resultDistributedSystem.hosts) {
			for (Object d : host.devices) {
				// BottleNeck bn = new BottleNeck( ((Device) d).getUtilization(), host.name + ":" + ((Device) d).getDeviceName() );
				BottleNeck bn = new BottleNeck(((Device) d).getUtilization(), host, ((Device) d));
				bottleneck.add(bn);
			}
			for (Object s : host.softServers) {
				BottleNeck bn = new BottleNeck(((SoftServer) s).getUtilization(), host.name + ":" + ((SoftServer) s).name);
				bottleneck.add(bn);
			}
			for (Object vr : host.virResources) {
				BottleNeck bn = new BottleNeck(((VirtualResource) vr).getUtilization(), host.name + ":" + ((VirtualResource) vr).name);
				bottleneck.add(bn);
			}
		}
		for (LanLink lk : resultDistributedSystem.links) {
			BottleNeck bn = new BottleNeck(lk.getUtilization(lk.srclan, lk.destlan), "link " + lk.srclan + ":" + lk.destlan);
			bottleneck.add(bn);
			BottleNeck bn1 = new BottleNeck(lk.getUtilization(lk.destlan, lk.srclan), "link " + lk.destlan + ":" + lk.srclan);
			bottleneck.add(bn1);
		}
		Collections.sort(bottleneck);
		int count = 0;

		// run for all the DevQ in a host
		for (Host h : resultDistributedSystem.hosts) {
			for (Device d : h.devices) {
				d.calculateAndPrintAverageDeviceSpeedup(h);
			}
		}

		System.out.println("\n resource_name --- util");
		for (BottleNeck bn : bottleneck) {
			bn.print();

			if (bn.device != null && bn.device.isDevicePowerManaged) {
				double wt_util = 0.0;
				//FIXME typecast to QueueSim means that this method will not work for the analytical part
				for (QServerInstance qsi : ((QueueSim) bn.device.resourceQueue).qServerInstances) {
					wt_util += qsi.totalBusyTime.getTotalValue() * qsi.avgSpeedup / SimulationParameters.currentTime;
				}
				wt_util = (wt_util / bn.device.availabelSpeedLevels[bn.device.totalFrequencyLevels])
						/ ((QueueSim) bn.device.resourceQueue).qServerInstances.size();
				System.out.println("\t weighted_util: " + wt_util);

				// write results into a file
				write_into_file(bn);
			} else {
				System.out.println();
			}

			count++;
			if (count >= n) {
				return " ";
			}
		}
		return " ";
	}

	// analyze results. Go through all the queues. if any of queues utilization is greater
	// than specified value then print the message and return 1.
	// else return 0.
	
	//FIXME reverify this method
	public String analyseResults(double refUtil) {
		boolean error = false;
		if (refUtil > 1) {
			refUtil = 1;
		}
		for (Host host : resultDistributedSystem.hosts) {
			for (Device d : host.devices) {
				if (d.getUtilization() > refUtil) {
					logger.error("Analyse:Overload at " + host.name + "/" + d.getDeviceName() + " util:" + d.getUtilization());
					error = true;
				}
			}
			for (SoftServer s : host.softServers) {
				if (s.getUtilization() > refUtil) {
					logger.error("Analyse:Overload at " + host.name + "/" + s.name + " util:" + s.getUtilization());
					error = true;
				}
			}
			for (VirtualResource s : host.virResources) {
				if (s.getUtilization() > refUtil) {
					logger.error("A	// find response timenalyse:Overload at " + host.name + "/" + s.name + " util:"
							+ s.getUtilization());
					error = true;
				}
			}
		}
		for (LanLink lk : resultDistributedSystem.links) {
			if (lk.getUtilization(lk.srclan, lk.destlan) > refUtil) {
				logger.error("Analyse:Overload at Link:" + lk.getName() + " util:" + lk.getUtilization(lk.srclan, lk.destlan));
				error = true;
			}
			if (lk.getUtilization(lk.destlan, lk.srclan) > refUtil) {
				logger.error("Analyse:Overload at Link:" + lk.getName() + " util:" + lk.getUtilization(lk.destlan, lk.srclan));
				error = true;
			}
		}
		if (error == true) {
			return "1";
		} else {
			return "0";
		}
	}

	//FIXME: tidy up this method
	private void write_into_file(BottleNeck bn) {
		try { // Create file
			FileOutputStream fos = null;
			try {
				fos = new FileOutputStream("/home/yogesh/" + "ondemand_" + bn.host.name + bn.device.name, true);
			} catch (FileNotFoundException e) { // Bhavin
				e.printStackTrace();
			}
			// FileOutputStream fos = new FileOutputStream("graphs/dev_speed_vs_arate/"+"conservative_"+bn.host.name+bn.device.name, true);
			// FileOutputStream fos = new FileOutputStream("graphs/dev_speed_vs_arate/"+"performance_"+bn.host.name+bn.device.name, true);
			PrintStream ps = new PrintStream(fos);

			double P_static, P_dynamic, P_total;
			double volt1 = 1, volt2 = 2, freq1, freq2;
			freq1 = bn.device.availabelSpeedLevels[0];
			freq2 = bn.device.availabelSpeedLevels[bn.device.totalFrequencyLevels];

			double voltage = 0.0, capacitance = 0.0, energy = 0.0, energy_per_req = 0.0;
			// linear relationship b/w frequency and voltage
			voltage = volt1 + (bn.device.avgDeviceSpeedup - freq1) * (volt2 - volt1) / (freq2 - freq1);
			// capacitance also varies as voltage varies -- assumption: as const.
			capacitance = 1;
			P_static = 2.45;
			P_dynamic = capacitance * voltage * voltage * bn.device.avgDeviceSpeedup;
			P_total = P_static + P_dynamic;
			energy = P_total * SimulationParameters.currentTime;
			energy_per_req = energy / ModelParameters.getTotalNumberOfRequests();
			System.out.println("Voltage: " + voltage + "  P_dyn: " + P_dynamic + "  P_tot: " + P_total + "  energy_per_req: " + energy_per_req);

			// This gives data for plotting various graphs
			ps.print(findScenarioArrivalRate(-1, "eters"));
			ps.print("  ");
			ps.print(findScenarioResponseTime(-1, "eters"));
			ps.print("  ");
			ps.print(findScenarioThroughput(-1, "eters"));
			ps.print("  ");
			ps.print(bn.device.avgDeviceSpeedup);
			ps.print("  ");
			ps.print(bn.device.downThreshold);
			ps.print("  ");
			ps.print(bn.device.upThreshold);
			ps.print("  ");
			ps.print(bn.device.deviceProbeInterval);
			ps.print("  ");
			ps.print(bn.getUtil());
			ps.print("  ");
			ps.print(bn.getUtil() * bn.device.avgDeviceSpeedup);
			ps.print("  ");

			ps.print(voltage);
			ps.print("  ");
			ps.print(P_dynamic);
			ps.print("  ");
			ps.print(P_total);
			ps.print("  ");
			ps.print(energy);
			ps.print("  ");
			ps.print(energy_per_req);
			ps.print("  ");
			ps.print("\n");
			// Close the output stream
			ps.close();
			// } catch (Exception e) { //Catch exception if any
			// System.err.println("Error: " + e.getMessage()); //Bhavin
		} finally {
		}
	}
}
