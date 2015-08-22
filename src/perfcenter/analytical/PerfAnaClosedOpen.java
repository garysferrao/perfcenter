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
package perfcenter.analytical;

import java.util.ArrayList;

import org.apache.log4j.Logger;

import perfcenter.analytical.CompoundTask.SubTask;
import perfcenter.baseclass.Device;
import perfcenter.baseclass.DeviceServiceTime;
import perfcenter.baseclass.Host;
import perfcenter.baseclass.Lan;
import perfcenter.baseclass.LanLink;
import perfcenter.baseclass.Node;
import perfcenter.baseclass.Queue;
import perfcenter.baseclass.Scenario;
import perfcenter.baseclass.SoftServer;
import perfcenter.baseclass.Task;

/**
 * @author supriyam
 */

public class PerfAnaClosedOpen {
	DistributedSystemAna ds;
	Logger logger = Logger.getLogger("PerfAnaClosedOpen");
	boolean overload = false;

	public PerfAnaClosedOpen(DistributedSystemAna ds1) {
		ds = ds1;
	}

	public DistributedSystemAna performAnalysisOpen() throws Exception {
		try {
			// logger.info("Performing analysis...");
			findTreeArrivalRate();
			findCompoundTask();
			// printCompoundTasks();
			if (logger.isDebugEnabled())
				printCompoundTasks();
			initializeArrivalRates();

			calculateInitialTaskHoldingValues();
			calculateHoldingValues();
			softPerformanceParams();
			// devicePerformanceParams();
			calculateDeviceThinkTimes();

			networkTest();

			calculateTaskHoldingValues();
			calculateHoldingValues();
			softPerformanceParams();
			calculateDeviceThinkTimes();

			// networkTest();

			calculateTaskHoldingValues();
			calculateHoldingValues();
			softPerformanceParams();

			calculateScenarioResponseTime();
			calculateEndToEndScenarioResponseTime();

		} catch (Exception e) {
			System.out.println(e.getMessage());
		}
		return ds;
	}

	/* Set the arrival rates and prob for each node of all scenarios. Akhila */
	void findTreeArrivalRate() {
		for (Scenario sce : ds.scenarios)
			sce.initialize();
	}

	/* Finds all compound tasks. Akhila */
	void findCompoundTask() {
		for (Host h : ds.hosts) {
			for (Object s : h.softServers) {
				for (Scenario sce : ds.scenarios) {
					((SoftServerAna) s).createCompoundTaskObjectsForGivenSoftServer(sce.rootNode, sce.getName());
				}
			}
		}
	}

	/* This is just to print compound tasks. Akhila */
	void printCompoundTasks() {

		for (Host h : ds.hosts) {
			System.out.println("Host " + h.getName());
			for (Object s : h.softServers) {
				System.out.println("Server " + ((SoftServerAna) s).name);
				((SoftServerAna) s).print();
			}
		}
	}

	// initialize arrival rate to simple tasks
	void dfsInitArr(Node node) {
		String taskid = node.name;
		for (Task t : ds.getServer(node.servername).simpleTasks) {
			if (t.name.compareTo(taskid) == 0) {
				t.setArrRate(t.getArrRate() + node.arrate);
				logger.debug("node " + node.name + "  arrival rate = " + t.getArrRate());
			}
		}
		for (Node n : node.children)
			dfsInitArr(n);
	}

	void initializeTaskArrivalRates() {
		for (Scenario s : ds.scenarios) {
			logger.debug("In task arrival calculation for Scenario " + s.getName());
			dfsInitArr(s.rootNode);
		}
	}

	// set task arrival rate, software server arrival rate and device arrival rate
	void initializeArrivalRates() {
		double s_arrRate = 0;
		double num_of_copies = 0;
		double dev_arrRate = 0;
		initializeTaskArrivalRates();

		for (Host host : ds.hosts) {
			for (Object softserv : (((Host) host).softServers)) {
				for (SoftServer distSysServers : ds.softServers) {
					if (((SoftServerAna) softserv).getName().compareTo(distSysServers.getName()) == 0) {
						num_of_copies = distSysServers.getNumCopies();
						for (Task task : ((SoftServerAna) softserv).simpleTasks) {
							for (Task task1 : distSysServers.simpleTasks) {
								if (task.name.compareTo(task1.name) == 0) {
									task.setArrRate(task1.getArrRate() / num_of_copies);
								}
							}
						}
					}
				}

				s_arrRate = 0;
				for (CompoundTask ct : ((SoftServerAna) softserv).compundTasks) {
					s_arrRate += ct.getArrRate();
				}
				((SoftServerAna) softserv).setAverageArrivalRate(s_arrRate / num_of_copies);
				logger.debug("host " + host.getName() + " server " + ((SoftServerAna) softserv).getName() + " arrival rate"
						+ ((SoftServerAna) softserv).getAverageArrivalRate());
			}

			/* device arrival rates being initialized */
			/* to be done .. divided by the total number of devices */
			for (Object d : ((Host) host).devices) {
				dev_arrRate = 0;

				for (Object softserv : ((Host) host).softServers) {
					for (Task t : ((SoftServerAna) softserv).simpleTasks) {
						for (DeviceServiceTime dst : t.deviceServiceTimes) {
							if (dst.getDeviceName().compareTo(((Device) d).getDeviceName()) == 0)
								dev_arrRate += t.getArrRate();
						}
					}
				}

				((Device) d).setAverageArrivalRate(dev_arrRate);
				logger.debug("host " + ((Host) host).getName() + " device " + ((Device) d).getDeviceName() + " arrival rate = "
						+ ((Device) d).getAverageArrivalRate());

			}

		}
	}

	// Find holding time of each subtask in a compound task
	void calculateInitialTaskHoldingValues() {
		double ct_htime;
		for (Host host : ds.hosts) {
			for (Object softserv : host.softServers) {
				for (CompoundTask ct : ((SoftServerAna) softserv).compundTasks) {
					ct_htime = 0;
					for (SubTask t : ct.subTasks) {
						if (t.getServerName().compareTo(((SoftServerAna) softserv).getName()) == 0) {
							// if it is a simple task on the same machine ..
							// currently unless n/w r implmented it is correct
							for (DeviceServiceTime dst : ds.getTask(t.getName()).deviceServiceTimes) {
								ct_htime += dst.getDistribution().getServiceTime();
							}
						}
						// else get the task response time adding network delay
						// if the machines are on different lans
						else {
							for (String hostname : ds.getServer(t.getServerName()).hosts) {
								ct_htime += getInitialIndividualTaskHoldingTime(hostname, t.getServerName(), t.getName(), ct.getScenarioName());
								ct_htime /= ds.getServer(t.getServerName()).hosts.size();

							}

						}
					}
					ct.setHoldingTime(ct_htime);
					logger.debug("host " + ((Host) host).getName() + " compnd task  " + ct.getName() + "compound task holding time = "
							+ ct.getHoldingTime());

				}
			}

		}

	}

	// recursively find the holding times
	public double getInitialIndividualTaskHoldingTime(String hostname, String servername, String taskname, String Scename) {
		double ct_htime;
		CompoundTask ct = ((SoftServerAna) (ds.getHost(hostname).getServer(servername))).getCTask(taskname, Scename);

		ct_htime = 0;
		for (SubTask t : ct.subTasks) {
			if (t.getServerName().compareTo(servername) == 0) {
				// if it is a simple task on the same machine ..
				// currently unless n/w r implmented it is correct
				for (DeviceServiceTime dst : ds.getTask(t.getName()).deviceServiceTimes) {
					ct_htime += dst.getDistribution().getServiceTime();
				}
			}
			// else get the task response time adding network delay
			// if the machines are on different lans
			else {
				for (String hname : ds.getServer(t.getServerName()).hosts) {
					ct_htime += messageDelay(hostname, hname, sizeOfMsg(taskname, t.getName()))
							+ getInitialIndividualTaskHoldingTime(hname, t.getServerName(), t.getName(), Scename)
							+ messageDelay(hname, hostname, sizeOfMsg(t.getName(), taskname));
					ct_htime /= ds.getServer(t.getServerName()).hosts.size();

				}

			}
		}
		ct.setHoldingTime(ct_htime);
		logger.debug(" In Individual  host " + hostname + "CT " + ct.getName() + " compound task holding time = " + ct.getHoldingTime());

		return ct_htime;
	}

	/* To be done: get response time from the particular resource... */
	// for second iteration
	void calculateTaskHoldingValues() {
		double ct_htime;
		for (Host host : ds.hosts) {
			for (Object softserv : host.softServers) {
				for (CompoundTask ct : ((SoftServerAna) softserv).compundTasks) {
					ct_htime = 0;
					SubTask prevtask = null;
					for (SubTask t : ct.subTasks) {
						if (t.getServerName().compareTo(((SoftServerAna) softserv).getName()) == 0) {

							// if it is a simple task on the same machine ..
							// currently unless n/w r implmented it is correct
							for (DeviceServiceTime dst : ((SoftServerAna) softserv).getSimpleTask(t.getName()).deviceServiceTimes) {
								ct_htime += dst.getResponseTime();
							}
						}
						// else get the task response time adding network delay
						// if the machines are on different lans
						else {
							for (String hostname : ds.getServer(t.getServerName()).hosts) {
								ct_htime += messageDelay(host.name, hostname, sizeOfMsg(prevtask.name, t.getName()))
										+ getIndividualTaskHoldingTime(hostname, t.getServerName(), t.getName(), ct.getScenarioName())
										+ messageDelay(
												hostname,
												host.name,
												sizeOfMsg(getTaskFromReply(ct, prevtask.name, t.getName()),
														getTaskAfterReply(ct, prevtask.name, t.getName())));
								/*
								 * ct_htime += getIndividualTaskHoldingTime( hostname, t.getServerName(), t .getName(),ct.getScenarioName());
								 */
								ct_htime /= ds.getServer(t.getServerName()).hosts.size();

							}

						}
						prevtask = t;
					}
					ct.setHoldingTime(ct_htime);
					logger.debug("host " + ((Host) host).getName() + " compnd task  " + ct.getName() + "compound task holding time = "
							+ ct.getHoldingTime());

				}
			}

		}

	}

	public double getIndividualTaskHoldingTime(String hostname, String servername, String taskname, String Scename) {
		double ct_htime;
		CompoundTask ct = ((SoftServerAna) (ds.getHost(hostname).getServer(servername))).getCTask(taskname, Scename);
		ct_htime = 0;
		SubTask prevtask = null;
		for (SubTask t : ct.subTasks) {
			if (t.getServerName().compareTo(servername) == 0) {
				// if it is a simple task on the same machine ..
				// currently unless n/w r implmented it is correct
				for (DeviceServiceTime dst : ((SoftServerAna) ds.getHost(hostname).getServer(servername)).getSimpleTask(t.getName()).deviceServiceTimes) {
					ct_htime += dst.getResponseTime();
				}
			}
			// else get the task response time adding network delay
			// if the machines are on different lans
			else {
				for (String hname : ds.getServer(t.getServerName()).hosts) {
					ct_htime += messageDelay(hostname, hname, sizeOfMsg(prevtask.name, t.getName()))
							+ getIndividualTaskHoldingTime(hname, t.getServerName(), t.getName(), ct.getScenarioName())
							+ messageDelay(hname, hostname,
									sizeOfMsg(getTaskFromReply(ct, prevtask.name, t.getName()), getTaskAfterReply(ct, prevtask.name, t.getName())));

					/*
					 * ct_htime += getIndividualTaskHoldingTime(hname, t .getServerName(), t.getName(),Scename);
					 */
					ct_htime /= ds.getServer(t.getServerName()).hosts.size();

				}

			}
			prevtask = t;
		}
		ct.setHoldingTime(ct_htime);
		logger.debug(" In Individual  host " + hostname + "compound task holding time = " + ct.getHoldingTime());

		return ct_htime;
	}

	void calculateServerHoldingValues() {
		double s_htime, s_arate, num_of_copies;
		for (Object host : ds.hosts) {
			for (Object softserv : (((Host) host).softServers)) {
				s_htime = 0;
				s_arate = 0;
				num_of_copies = (ds.getServer(((SoftServerAna) softserv).getName())).getNumCopies();
				for (CompoundTask ct : ((SoftServerAna) softserv).compundTasks) {
					s_htime += ct.getArrRate() * ct.getHoldingTime() / num_of_copies;
					s_arate += ct.getArrRate() / num_of_copies;
				}
				if (s_arate != 0) {
					((SoftServerAna) softserv).setHoldingTime(s_htime / s_arate);
				}
				logger.debug("host " + ((Host) host).getName() + " server name  " + ((SoftServerAna) softserv).getName() + "server holding time = "
						+ ((SoftServerAna) softserv).getHoldingTime());

			}
		}
	}

	void calculateDeviceHoldingValues() {
		double d_htime, dev_arrRate;
		for (Host host : ds.hosts) {
			for (Object d : host.devices) {
				dev_arrRate = 0;
				d_htime = 0;
				for (Object softserv : (((Host) host).softServers)) {
					for (Task t : ((SoftServerAna) softserv).simpleTasks) {
						for (DeviceServiceTime dst : t.deviceServiceTimes) {
							if (dst.getDeviceName().compareTo(((Device) d).getDeviceName()) == 0) {
								dev_arrRate += t.getArrRate();
								d_htime += t.getArrRate() * dst.getDistribution().getServiceTime();

							}
						}
					}

				}
				if (dev_arrRate != 0) {
					((Device) d).setHoldingTime(d_htime / dev_arrRate);
					logger.debug("Holding time for " + ((Host) host).getName() + "  device :" + ((Device) d).getDeviceName() + " = "
							+ ((Device) d).getHoldingTime());
				}
			}

		}
	}

	void calculateHoldingValues() {
		calculateServerHoldingValues();
		calculateDeviceHoldingValues();
	}

	void devicePerformanceParams() throws Exception {
		for (Host host : ds.hosts) {
			for (Object dev : host.devices) {
				Device d = (Device) dev;
				if ((d.getAverageArrivalRate() * d.getHoldingTime()) < d.count.getValue())
					d.setThroughput(d.getAverageArrivalRate());
				else {
					d.setThroughput(1 / d.getAverageArrivalRate());
					// akhila throw new Exception("Overload at device "+((Host)host).getName() +" "+d.getDeviceName());
				}
				d.setUtilization(d.getAverageArrivalRate() * d.getHoldingTime() / d.count.getValue());

				// m/m/c calculation

				double pizero = 1.0, term = 1.0;
				double utilztn = d.getUtilization();
				double NoOfThreads = d.count.getValue();
				int i;
				for (i = 1; i < NoOfThreads; i++) {
					// row'=row/c <--
					term *= (NoOfThreads * utilztn / i);
					pizero += term;
				}
				// row^c/c.c!
				term *= NoOfThreads * utilztn / i;

				// add row^c/(1-row/c)^2
				pizero += term / (1 - utilztn);
				pizero = (1 / pizero);

				// Queue length excluding requests already in system
				double tmp = (term * pizero * utilztn / (Math.pow((1 - utilztn), 2.0)));
				d.setAvgQueueLength(tmp);

				d.setAvgWaitingTime((d.getAvgQueueLength()) * (d.getHoldingTime()) / (utilztn * NoOfThreads));

				// set the response time of devices
				double dev_arrRate = 0;
				double d_rtime = 0;
				for (Object softserv : (((Host) host).softServers)) {
					for (Task t : ((SoftServerAna) softserv).simpleTasks) {
						for (DeviceServiceTime dst : t.deviceServiceTimes) {
							if (dst.getDeviceName().compareTo(d.getDeviceName()) == 0) {
								dev_arrRate += t.getArrRate();

								dst.setResponseTime(d.getAvgWaitingTime() + dst.getDistribution().getServiceTime());
								d_rtime += t.getArrRate() * dst.getResponseTime();
							}
						}
					}
				}
				if (dev_arrRate != 0) {
					d.setAverageResponseTime(d_rtime / dev_arrRate);
				}

				logger.debug("Host " + ((Host) host).getName() + "  device :" + d.getDeviceName() + " Throughput: " + d.getThroughput()
						+ "Utilization= " + d.getUtilization() + "Waiting time = " + d.getAvgWaitingTime());

			}

			// the response times of all tasks can now be calculated by
			// approximating their waiting time as m/m/c queues.
			for (Object serv : ((Host) host).softServers) {
				for (Task task : ((SoftServerAna) serv).simpleTasks) {
					for (DeviceServiceTime dst : task.deviceServiceTimes) {
						Device d1 = (Device) ((Host) host).getDevice(dst.getDeviceName());
						dst.setResponseTime(d1.getAvgWaitingTime() + dst.getDistribution().getServiceTime());
						logger.debug("Response time of " + task.name + "at host" + ((Host) host).getName() + "is+" + dst.getResponseTime());
					}

				}

			}

		}

	}

	void softPerformanceParams() throws Exception {
		for (Host host : ds.hosts) {
			for (Object softserv : host.softServers) {
				if ((((SoftServerAna) softserv).getAverageArrivalRate() * ((SoftServerAna) softserv).getHoldingTime()) < ((SoftServerAna) softserv).thrdCount
						.getValue())
					((SoftServerAna) softserv).setThroughput(((SoftServerAna) softserv).getAverageArrivalRate());
				else {

					((SoftServerAna) softserv).setThroughput(((SoftServerAna) softserv).thrdCount.getValue()
							/ ((SoftServerAna) softserv).getAverageArrivalRate());
					// throw new Error("Overload at software server "+((Host)host).GetName() +" "+((SoftServerAna) softserv).getName());
				}
				((SoftServerAna) softserv).setUtilization(((SoftServerAna) softserv).getAverageArrivalRate() * ((SoftServerAna) softserv).getHoldingTime()
						/ ((SoftServerAna) softserv).thrdCount.getValue());
				// akhila if(((SoftServerAna) softserv).getUtilization() > 1)
				// akhila throw new Exception("Overload at software server "+((Host)host).getName() +" "+((SoftServerAna) softserv).getName());

				// M/M/C formula.. copied from perfcentre..
				// trivedi page 422
				// term=(c*util)^c/c!

				double pizero = 1.0, term = 1.0;
				double utilztn = ((SoftServerAna) softserv).getUtilization();
				double NoOfThreads = ((SoftServerAna) softserv).thrdCount.getValue();
				int i;
				for (i = 1; i < NoOfThreads; i++) {
					// row'=row/c <--
					term *= (NoOfThreads * utilztn / i);
					pizero += term;
				}
				// row^c/c.c!
				term *= NoOfThreads * utilztn / i;

				// add row^c/(1-row/c)^2
				pizero += term / (1 - utilztn);
				pizero = (1 / pizero);

				// Queue length excluding requests already in system
				double tmp = (term * pizero * utilztn / (Math.pow((1 - utilztn), 2.0)));
				((SoftServerAna) softserv).setAvgQueueLength(tmp);

				((SoftServerAna) softserv).setAvgWaitingTime(((SoftServerAna) softserv).getAvgQueueLength()
						* ((SoftServerAna) softserv).getHoldingTime() / (utilztn * NoOfThreads));
				double serv_Resp = 0, serv_arr = 0;

				// set the response time of the server
				for (CompoundTask ct : ((SoftServerAna) softserv).compundTasks) {
					ct.setResponseTime(((SoftServerAna) softserv).getAvgWaitingTime() + ct.getHoldingTime());
					serv_Resp += ct.getResponseTime() * ct.getArrRate();
					serv_arr += ct.getArrRate();
					logger.debug(" CT :" + ct.getName() + " Response time" + ct.getResponseTime());

				}
				((SoftServerAna) softserv).setAverageResponseTime(serv_Resp / serv_arr);

				for (CompoundTask ct : ((SoftServerAna) softserv).compundTasks) {
					ct.setResponseTime(((SoftServerAna) softserv).getAvgWaitingTime() + ct.getHoldingTime());
					logger.debug(" CT :" + ct.getName() + " Response time" + ct.getResponseTime());

				}
				logger.debug("Host " + ((Host) host).getName() + "  softserver :" + ((SoftServerAna) softserv).getName() + " Throughput: "
						+ ((SoftServerAna) softserv).getThroughput() + "Utilization= " + ((SoftServerAna) softserv).getUtilization()
						+ " Queue Length = " + ((SoftServerAna) softserv).getAvgQueueLength() + "Waiting time = "
						+ ((SoftServerAna) softserv).getAvgWaitingTime());
			}

		}
	}

	void calculateScenarioResponseTime() {
		double sc_resptime;
		double arr_rate;
		Node curr;
		for (Scenario sc : ds.scenarios) {
			sc_resptime = 0;
			arr_rate = 0;
			if (sc.rootNode.name.compareToIgnoreCase("user") == 0) {
				curr = sc.rootNode.children.get(0);
			} else {
				curr = sc.rootNode;
			}
			String taskname = curr.name;
			for (Host host : ds.hosts) {
				for (Object softserv : host.softServers) {
					for (CompoundTask ct : ((SoftServerAna) softserv).compundTasks) {
						if ((ct.getName().compareTo(taskname) == 0) && (ct.getScenarioName().compareTo(sc.getName()) == 0)) {
							sc_resptime += ct.getArrRate() * ct.getResponseTime();
							arr_rate += ct.getArrRate();
						}
					}
				}

			}
			sc.setAverageResponseTime(sc_resptime / arr_rate);
			logger.debug("Scenario " + sc.getName() + " Response Time =" + sc_resptime / arr_rate);
		}
	}

	/* finds averaged response time over all scenarios. Akhila */
	void calculateEndToEndScenarioResponseTime() {
		double ete_resptime = 0;
		double tot_arrate = 0;
		for (Scenario s : ds.scenarios) {
			ete_resptime += (s.getArateToScenario() * s.getAverageResponseTime());
			tot_arrate += s.getArateToScenario();
		}
		ds.overallRespTime.setValue(ete_resptime / tot_arrate);
	}

	void calculateDeviceThinkTimes() throws Exception {
		for (Host host : ds.hosts) {
			for (perfcenter.baseclass.Device d : host.devices) {
				ArrayList<DeviceServiceTime> simpletasks = new ArrayList<DeviceServiceTime>();

				for (Object softserv : host.softServers) {
					// if(((SoftServerAna) softserv).getAvgServiceTime()>0){//removed by niranjan
					for (CompoundTask ct : ((SoftServerAna) softserv).compundTasks) {

						for (SubTask sbt : ct.subTasks) {
							if (sbt.getServerName() == ((SoftServerAna) softserv).getName()) {
								Task st = ((SoftServerAna) softserv).getSimpleTask(sbt.getName());

								/* if (st.getServiceTime(d.name).getServiceTime() > 0) { changed to */
								if (st.getServiceTime(d.name) != null) {
									st.getDevice(d.name).setHoldingTime(st.getServiceTime(d.name).getServiceTime());
									double thinktime = ((SoftServerAna) softserv).thrdCount.getValue() / ((SoftServerAna) softserv).getThroughput()
											- ((SoftServerAna) softserv).holdingTime;// niranjan:todo change to avgresponse time
									// -ct.getHoldingTime();
									// niranjan
									/*
									 * double thinktime = ((SoftServerAna) softserv).thrdCount .getValue() / ((SoftServerAna) softserv)
									 * .getThroughput() - ((SoftServerAna) softserv).getAvgResponseTime();
									 * System.out.println("avg serv time "+((SoftServerAna) softserv) .getAvgServiceTime()+
									 * " avg resp time "+((SoftServerAna) softserv).getAvgResponseTime());
									 */
									// niranjan

									double temp = 0;
									double inv = 0;
									SubTask prevtask = null;
									for (SubTask t : ct.subTasks) {

										if (t.servername != ((SoftServerAna) softserv).getName()) {
											if (t.getServerName().compareTo(((SoftServerAna) softserv).getName()) == 0) {

												// This part shd never be reached
												for (DeviceServiceTime dst : ((SoftServerAna) softserv).getSimpleTask(t.getName()).deviceServiceTimes) {
													if (!(t.name == st.name) && (dst.getDeviceName() == d.name)) {
														temp += dst.getDistribution().getServiceTime()
																+ ((Device) host.getDevice(dst.getDeviceName())).getAvgWaitingTime();
														;
													}
												}
											}
											// compound tasks invoked on other server before returning to the calling server.
											// response time
											else {
												double ct_htime = 0;
												for (String hostname : ds.getServer(t.getServerName()).hosts) {

													ct_htime += messageDelay(host.name, hostname, sizeOfMsg(prevtask.name, t.getName()))
															+ getIndividualTaskHoldingTime(hostname, t.getServerName(), t.getName(),
																	ct.getScenarioName())
															+ messageDelay(
																	hostname,
																	host.name,
																	sizeOfMsg(getTaskFromReply(ct, prevtask.name, t.getName()),
																			getTaskAfterReply(ct, prevtask.name, t.getName())));

													/*
													 * ct_htime +=((SoftServerAna) (ds.getHost(hostname) .getServer(t.getServerName())))
													 * .getCTask(t.getName(), ct.getScenarioName()) .getResponseTime();
													 */}
												ct_htime /= ds.getServer(t.getServerName()).hosts.size();

												temp += ct_htime;
											}
										} else {
											Task subtask = ((SoftServerAna) softserv).getSimpleTask(t.getName());
											if (subtask.getServiceTime(d.name) != null) {
												/*
												 * if loop added is because consider a case where task webcal cpu servt 0.124 disk servt 0.100 end
												 * 
												 * task database cpu servt 0.012 end
												 * 
												 * then while calculating thinktime for disk, thinktime should not be divided by no of
												 * invocations(over here 2), but it is done in CPU for above case because two of tasks uses CPU
												 */

												inv++;

											}
											if (t.getServerName().compareTo(((SoftServerAna) softserv).getName()) == 0) {

												// This part calculates the time spent by the simple task on other local devices
												for (DeviceServiceTime dst : ((SoftServerAna) softserv).getSimpleTask(t.getName()).deviceServiceTimes) {

													if (!(dst.getDeviceName().equalsIgnoreCase(d.name))) {
														temp += dst.getDistribution().getServiceTime()
																+ ((Device) host.getDevice(dst.getDeviceName())).getAvgWaitingTime();
														;
													}
												}
											}
										}
										prevtask = t;

									}
									/*
									 * tbd: check think time calculations (add temp as cal above)
									 */
									st.getDevice(d.name).setThinkTime((thinktime + temp) / inv);// niranjan todo: (inv-1)*getresponsetime

									// niranjan
									// System.out.println(inv+"before "+st.getDevice(d.name).getThinkTime());
									// st.getDevice(d.name).setThinkTime((thinktime+temp)/inv+(inv-1)*st.getDevice(d.name).getResponseTime());
									// System.out.println("after "+st.getDevice(d.name).getThinkTime());
									// niranjan
									/* this is bcoz the number of invocations of compound task = those of simpletask */

									st.getDevice(d.name).noOfUsers = ((SoftServerAna) softserv).thrdCount.getValue();
									st.getDevice(d.name).probabilty = ct.conditionalProbability / inv;
									// simpletasks.add(st.getDevice(d.name));
									// added by niranjan

									DeviceServiceTime tempdst = new DeviceServiceTime(d.name, null);
									tempdst.dist = st.getDevice(d.name).dist;
									tempdst.noOfUsers = st.getDevice(d.name).noOfUsers;
									tempdst.probabilty = ct.probability / inv;// check if it is going to work for all scenarios
									tempdst.setHoldingTime(st.getDevice(d.name).getHoldingTime());
									tempdst.setThinkTime(st.getDevice(d.name).getThinkTime());
									tempdst.setResponseTime(st.getDevice(d.name).getResponseTime());

									simpletasks.add(tempdst);

									// simpletasks.add(st.getDevice(d.name));
									// commented by niranjan

								}

							}
						}

					}
					// }
				}
				d.thinkTime = 0;
				d.numberOfUsers = 0;
				d.holdingTime = 0;
				if (simpletasks.size() == 0) {
					d.thinkTime = 0;
					d.holdingTime = 0;
					d.numberOfUsers = 0;
				}
				int i = 0;// to be removed
				DeviceServiceTime dst1 = null;
				for (DeviceServiceTime dst : simpletasks) {
					if (i == 0)// to be removed
					{ // d.thinkTime=d.thinkTime*2;
						// break;//to be removed
						d.numberOfUsers = dst.noOfUsers;
					}
					dst1 = dst;// to be removed

					// d.thinkTime+= dst.getThinkTime() * dst.noOfUsers;
					d.thinkTime += dst.getThinkTime() * dst.noOfUsers * dst.probabilty;
					/* d.numberOfUsers += dst.noOfUsers* dst.probabilty; */
					d.holdingTime += dst.getHoldingTime() * dst.noOfUsers * dst.probabilty;

					i++;// to be removed

				}
				// DeviceServiceTime dst=simpletasks.get(2);//to be removed
				// to be removed

				d.thinkTime /= d.numberOfUsers;
				// d.thinkTime=d.thinkTime*2/3;//to be removed
				logger.debug("device:" + d.name);
				logger.debug("think time: " + d.thinkTime);
				d.holdingTime /= d.numberOfUsers;

			}

			if (host.softServers.size() > 0) {
				for (Device d : host.devices) {
					String stationNames[] = new String[2];
					stationNames[0] = d.name;
					stationNames[1] = "infinite-server";
					int stationTypes[] = new int[2];
					stationTypes[0] = 1;
					stationTypes[1] = 2;
					double[] visits = new double[2];
					double[][] serviceTimes = new double[2][1];

					serviceTimes[0][0] = d.holdingTime;
					// System.out.println(d.holdingTime);

					serviceTimes[1][0] = d.thinkTime; // think time?
					// System.out.println(d.thinkTime);

					visits[0] = 1;
					visits[1] = 1;
					Solver solver;
					logger.debug("device on machine: " + host.getName());
					logger.debug("input: service time: " + serviceTimes[0][0] + " Think time: " + serviceTimes[1][0] + " no of users: "
							+ d.numberOfUsers + " no of servers " + d.count.getValue());
					SolverSingleClosedMVA closedsolver = new SolverSingleClosedMVA((int) Math.round(d.numberOfUsers), 2);// d. no of users
					if (!closedsolver.input(stationNames, stationTypes, serviceTimes, visits, (int) d.count.getValue()))
						throw new Exception("Error initializing MVASingleSolver", null);
					solver = closedsolver;
					if (d.numberOfUsers != 0) {
						solver.solve();

						d.setThroughput(solver.getThroughput(0));
						d.setUtilization(solver.getUtilization(0) / (int) d.count.getValue());
						d.setAverageResponseTime(solver.getResTime(0));
						d.setAvgWaitingTime(d.getAvgResponseTime() - d.holdingTime);
					}
					// d.setAvgServiceTime(aggrHoldingTime/noUsers);
					/*
					 * System.out.println(" "+solver.getResTime(0)); logger.("Utilization: server:" + getName() + "  " + getUtilization()+
					 * "Throughput:" + solver.getThroughput(0));
					 */
					logger.debug("output: Utilization:" + d.getUtilization() + " response time: " + solver.getResTime(0) + " Throughput:"
							+ solver.getThroughput(0));

				}

			}

			for (Device d : host.devices) {
				for (Object softserv : (((Host) host).softServers)) {
					for (Task t : ((SoftServerAna) softserv).simpleTasks) {
						for (DeviceServiceTime dst : t.deviceServiceTimes) {
							if (dst.getDeviceName().compareTo(d.getDeviceName()) == 0) {

								dst.setResponseTime(d.getAvgWaitingTime() + dst.getDistribution().getServiceTime());

							}
						}
					}
				}
			}
		}

	}

	void networkTest() {
		// System.out.println("niru's work*****************************************************");
		double mtu, trans_rate, prop_delay, avg_no_pkts, avg_pkt_size;// MTU between two hosts
		double link_arate = 0/* this is \lambda_{s,t,s',t'} */, size_of_msg, lan_arate = 0, tot_pkts_size = 0, lan_servtime;
		Host host1;// =ds.hosts.get(0);
		Host host2;// =ds.hosts.get(1);/* this should be taken as input*/
		LanLink lanlink;
		for (LanLink link : ds.links) {
			link.linkQforward = new Queue();
			link.linkQreverse = new Queue();
		}

		// mtu=getMtuBetweenLans(host1.lan,host2.lan);

		for (Lan lan1 : ds.lans) {
			for (Lan lan2 : ds.lans) {
				tot_pkts_size = 0;
				lan_arate = 0;
				lan_servtime = 0;
				avg_pkt_size = 0;

				if (!lan1.getName().equalsIgnoreCase(lan2.getName())) {
					lanlink = ds.getLink(lan1.getName(), lan2.getName());
					for (String host1str : lan1.hosts) {
						host1 = ds.getHost(host1str);
						for (String host2str : lan2.hosts) {
							host2 = ds.getHost(host2str);
							link_arate = 0;
							if (!host1.name.equalsIgnoreCase(host2.name)) {
								for (SoftServer softserv1 : host1.softServers) {

									for (SoftServer softserv2 : host2.softServers) {
										for (Task task1 : softserv1.simpleTasks) {
											for (Task task2 : softserv2.simpleTasks) {
												if (!task1.name.equalsIgnoreCase(task2.name)) {
													for (Scenario sc : ds.scenarios) {

														link_arate = link_arate + dfs(sc.rootNode, task1.name, task2.name)
																/ softserv1.getNumCopies();

													}
													if (link_arate > 0) {
														size_of_msg = sizeOfMsg(task1.name, task2.name);
														mtu = getMtuBetweenLans(lan1.getName(), lan2.getName());
														avg_no_pkts = Math.ceil(size_of_msg / (mtu - lanlink.headerSize.getValue() * 8));
														avg_pkt_size = (size_of_msg / avg_no_pkts) + lanlink.headerSize.getValue() * 8;
														lan_arate = lan_arate + link_arate * avg_no_pkts;
														tot_pkts_size = tot_pkts_size + link_arate * avg_pkt_size;
														link_arate = 0;

													}
												}
											}
										}
									}
								}
							}

						}

					}
					trans_rate = getTransmissionRate(lan1.getName(), lan2.getName());
					/* lan_servtime=tot_pkts_size*8/(lan_arate*trans_rate*1000000); */
					lan_servtime = tot_pkts_size / (lan_arate * trans_rate);
					lanlink.setArrRate(lan1.getName(), lan2.getName(), lan_arate);
					lanlink.setAvgServiceTime(lan1.getName(), lan2.getName(), lan_servtime);
				}
				/* if two hosts present on same lan then waiting time is zero */

			}
		}
		linkPerformanceParams();
	}

	String getTaskAfterReply(CompoundTask ct, String task1, String task2) {
		String returning_servname = "junkvalue";
		SubTask prevtask = ct.subTasks.get(0);
		for (SubTask t : ct.subTasks) {

			if (t.servername.equalsIgnoreCase(returning_servname)) {
				return t.name;
			}
			if (t.name.equalsIgnoreCase(task1)) {
				returning_servname = t.servername;
			}

		}
		return "did not found";
	}

	String getTaskFromReply(CompoundTask ct, String task1, String task2) {
		String returning_servname = null;
		SubTask prevtask = ct.subTasks.get(0);
		for (SubTask t : ct.subTasks) {
			if (t.servername.equalsIgnoreCase(returning_servname)) {
				return prevtask.name;
			}
			if (t.name.equalsIgnoreCase(task1)) {
				returning_servname = t.servername;
			}
			prevtask = t;
		}
		return "did not found";
	}

	double dfs(Node node, String task1_name, String task2_name) {
		double arrate_links = 0;
		for (Node child : node.children) {
			if (node.name.equals(task1_name) && child.name.equals(task2_name)) {
				arrate_links = arrate_links + child.arrate;
			}
			arrate_links = arrate_links + dfs(child, task1_name, task2_name);

		}
		return arrate_links;
	}

	double sizeOfMsg(String task1_name, String task2_name) {
		double length_of_msg;
		for (Scenario sc : ds.scenarios) {
			length_of_msg = sizeOfMsg(sc.rootNode, task1_name, task2_name);
			if (length_of_msg > 0)
				return length_of_msg;
		}
		return 0;
	}

	/*
	 * double sizeOfMsg(Node node,String task1_name,String task2_name) {
	 * 
	 * double length_of_msg=0;
	 * 
	 * for(Node child: node.children) { if(node.name.equals(task1_name)&&child.name.equals(task2_name)) { return child.pktsize.getValue(); }
	 * length_of_msg=sizeOfMsg(child,task1_name,task2_name); if(length_of_msg>0) return length_of_msg; }
	 * 
	 * return 0; }
	 */
	double sizeOfMsg(Node node, String task1_name, String task2_name) {

		double length_of_msg = 0;

		for (Node child : node.children) {
			if (node.name.equals(task1_name) && child.name.equals(task2_name)) {
				return child.pktsize.getValue() * 8;
				/*
				 * since size of message during syncro8s specified in Bytes, to convert to bits, length of message is multiplied by 8
				 */

			}
			length_of_msg = sizeOfMsg(child, task1_name, task2_name);
			if (length_of_msg > 0)
				return length_of_msg;
		}

		return 0;
	}

	void linkPerformanceParams() {
		double wtime, utilization;
		for (LanLink link : ds.links) {
			link.setThroughput(link.srclan, link.destlan, link.getArrRate(link.srclan, link.destlan));
			utilization = link.getArrRate(link.srclan, link.destlan) * link.getAvgServiceTime(link.srclan, link.destlan);
			link.setUtilization(link.srclan, link.destlan, utilization);
			wtime = link.getAvgServiceTime(link.srclan, link.destlan) / (1 - utilization);
			link.setAvgWaitingTime(link.srclan, link.destlan, wtime);
			logger.debug("link on LAN from " + link.srclan + " " + link.destlan);
			logger.debug("input: service time: " + link.getAvgServiceTime(link.srclan, link.destlan) + " arrival rate: "
					+ link.getArrRate(link.srclan, link.destlan) + " no of servers: 1 ");
			logger.debug("input: response time: " + (wtime + link.getAvgServiceTime(link.srclan, link.destlan)) + " utilization: " + utilization
					+ " no of servers: 1 ");
			link.setThroughput(link.destlan, link.srclan, link.getArrRate(link.destlan, link.srclan));
			utilization = link.getArrRate(link.destlan, link.srclan) * link.getAvgServiceTime(link.destlan, link.srclan);
			link.setUtilization(link.destlan, link.srclan, utilization);
			wtime = link.getAvgServiceTime(link.destlan, link.srclan) / (1 - utilization);
			link.setAvgWaitingTime(link.destlan, link.srclan, wtime);
			logger.debug("link on LAN from " + link.destlan + " " + link.srclan);
			logger.debug("input: service time: " + link.getAvgServiceTime(link.destlan, link.srclan) + " arrival rate: "
					+ link.getArrRate(link.destlan, link.srclan) + " no of servers: 1 ");
			logger.debug("input: respons time: " + (wtime + link.getAvgServiceTime(link.destlan, link.srclan)) + " utilization: " + utilization
					+ " no of servers: 1 ");

		}
	}

	/*
	 * double getMtuBetweenLans(String lan1,String lan2) { for(LanLink link : ds.links) { if(link.srclan.equals(lan1)&&
	 * link.destlan.equals(lan2)||link.srclan.equals(lan2)&& link.destlan.equals(lan1)) { return link.mtu.getValue(); } } return 0; }
	 */
	double getMtuBetweenLans(String lan1, String lan2) {
		for (LanLink link : ds.links) {
			if (link.srclan.equals(lan1) && link.destlan.equals(lan2) || link.srclan.equals(lan2) && link.destlan.equals(lan1)) {
				return link.mtu.getValue() * 8;
				/*
				 * since MTU of link is specified in Bytes, to convert to bits, mtu of message is multiplied by 8
				 */
			}
		}
		return 0;
	}

	/*
	 * double getTransmissionRate(String lan1,String lan2) { for(LanLink link : ds.links) { if(link.srclan.equals(lan1)&&
	 * link.destlan.equals(lan2)||link.srclan.equals(lan2)&& link.destlan.equals(lan1)) { return link.trans.getValue();
	 * 
	 * } } return 0; }
	 */

	double getTransmissionRate(String lan1, String lan2) {
		for (LanLink link : ds.links) {
			if (link.srclan.equals(lan1) && link.destlan.equals(lan2) || link.srclan.equals(lan2) && link.destlan.equals(lan1)) {
				if (link.transUnit.equalsIgnoreCase("bps"))
					return link.trans.getValue();
				else if (link.transUnit.equalsIgnoreCase("Kbps"))
					return link.trans.getValue() * 1000;
				else if (link.transUnit.equalsIgnoreCase("Mbps"))
					return link.trans.getValue() * 1000000;
				else if (link.transUnit.equalsIgnoreCase("Gbps"))
					return link.trans.getValue() * 1000000000;

			}
		}
		return 0;
	}

	/*
	 * double getPropogationDelay(String lan1,String lan2) { for(LanLink link : ds.links) { if(link.srclan.equals(lan1)&&
	 * link.destlan.equals(lan2)||link.srclan.equals(lan2)&& link.destlan.equals(lan1)) { return link.prop.getValue(); } } return 0; }
	 */
	double getPropBetweenLans(String lan1, String lan2) {
		for (LanLink link : ds.links) {
			if (link.srclan.equals(lan1) && link.destlan.equals(lan2) || link.srclan.equals(lan2) && link.destlan.equals(lan1)) {
				if (link.propUnit.equalsIgnoreCase("ms"))
					return link.prop.getValue() / 1000;
				else if (link.propUnit.equalsIgnoreCase("us"))
					return link.prop.getValue() / 1000000;
				else if (link.propUnit.equalsIgnoreCase("ns"))
					return link.prop.getValue() / 1000000000;
				else if (link.propUnit.equalsIgnoreCase("ps"))
					return link.prop.getValue() / 1000000000000.0;
				/*
				 * since MTU of link is specified in Bytes, to convert to bits, mtu of message is multiplied by 8
				 */
			}
		}
		return 0;
	}

	double messageDelay(String host1, String host2, double length) {
		LanLink lanlink;
		Lan lan1, lan2;
		int noofpkts;
		if (ds.links.size() == 0)
			return 0;
		else if (ds.getHost(host1).lan == null || ds.getHost(host2).lan == null)
			return 0;
		lan1 = ds.getLan(ds.getHost(host1).lan);
		lan2 = ds.getLan(ds.getHost(host2).lan);
		if (lan1.getName().equals(lan2.getName())) {
			return 0;
		} else {
			lanlink = ds.getLink(lan1.getName(), lan2.getName());
			noofpkts = (int) (length / (getMtuBetweenLans(lan1.getName(), lan2.getName()) - lanlink.headerSize.getValue() * 8));
			if (length % (getMtuBetweenLans(lan1.getName(), lan2.getName()) - lanlink.headerSize.getValue() * 8) >= 0.0) {
				noofpkts = noofpkts + 1;
			}
			/* return (lanlink.getAvgWaitingTime(lan1.getName(), lan2.getName())+lanlink.prop.getValue()+length/lanlink.trans.getValue()); */
			double returnvalue = lanlink.getAvgWaitingTime(lan1.getName(), lan2.getName()) + getPropBetweenLans(lan1.getName(), lan2.getName())
					+ (length + lanlink.headerSize.getValue() * 8) / getTransmissionRate(lan1.getName(), lan2.getName());
			return returnvalue;
		}

	}

}