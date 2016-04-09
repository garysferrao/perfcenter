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

import perfcenter.baseclass.enums.SchedulingPolicy;

/**
 * Used for defining connection parameters(mtu,prop,trans,headersize) between two lans
 * 
 * @author akhila
 */
public class LanLink {
	protected String name;
	public String srclan;
	public String destlan;

	public Variable mtu, trans, prop, headerSize;
	public String mtuUnit = "B", transUnit = "us", propUnit = "Mbps", headerSizeUnit = "B";
	/** queue from srclan to destlan */
	public Queue linkQforward;

	/** queue from destlan to srclan */
	public Queue linkQreverse;

	public Variable buffer;
	public SchedulingPolicy pol;

	/** used by analytical perfcenter. added by niranjan. */
	public double forward_holdingTime;
	/** used by analytical perfcenter. added by niranjan. */
	public double forward_thinkTime;
	/** used by analytical perfcenter. added by niranjan. */
	public double forward_numberOfUsers;
	
	/** used by analytical perfcenter. added by niranjan. */
	public double backward_holdingTime;
	/** used by analytical perfcenter. added by niranjan. */
	public double backward_thinkTime;
	/** used by analytical perfcenter. added by niranjan. */
	public double backward_numberOfUsers;

	public LanLink(String lnkname, String name1, String name2) {
		name = lnkname;
		srclan = name1;
		destlan = name2;
		mtu = new Variable("local", 1);
		prop = new Variable("local", 1);
		trans = new Variable("local", 1);
		headerSize = new Variable("local", 1);
		buffer = new Variable("local", 99999);
		pol = SchedulingPolicy.FCFS;
	}

	public LanLink() {

	}

	public String getName() {
		return name;
	}

	public String getSrcLanName() {
		return srclan;
	}

	public String getDestLanName() {
		return destlan;
	}

	// add network parameter and their unit
	public void addMTU(Variable val, String unit) {

		if (unit.compareToIgnoreCase("Bytes") == 0) {
			mtu = val;
			mtuUnit = "B";
			return;
		}
		throw new Error("MTU Unit can only be Bytes");
	}

	public void addPropDelay(Variable val, String unit) {
		if ((unit.compareTo("us") == 0) || (unit.compareTo("ns") == 0) || (unit.compareTo("ms") == 0)) {
			prop = val;
			propUnit = unit;
			return;
		}
		throw new Error("prop unit can be only ms/us/ns");
	}

	public void addTransRate(Variable val, String unit) {

		if ((unit.compareTo("Kbps") == 0) || (unit.compareTo("bps") == 0) || (unit.compareTo("Mbps") == 0) || (unit.compareTo("Gbps") == 0)) {
			trans = val;
			transUnit = unit;
			return;
		}
		throw new Error("trans unit can be only bps/Mbps/Kbps/Gbps");
	}

	public void addHeaderSize(Variable val, String unit) {

		if (unit.compareToIgnoreCase("Bytes") == 0) {
			headerSize = val;
			headerSizeUnit = "B";
			return;
		}
		throw new Error("MTU Unit can only be Bytes");
	}

	/** modify network parameter and/or unit */
	public void modifyTransRate(double var1, String unit) {
		if (trans.name.compareToIgnoreCase("local") == 0) {
			trans.value = var1;
			transUnit = unit;
			return;
		}
		throw new Error("Attempt to modify the TransRate of " + name + ", instead variable " + trans.name + " should be modified");
	}

	public void modifyPropDelay(double var1, String unit) {
		if (prop.name.compareToIgnoreCase("local") == 0) {
			prop.value = var1;
			propUnit = unit;
			return;
		}
		throw new Error("Attempt to modify the Propogation of " + name + ", instead variable " + prop.name + " should be modified");
	}

	public void modifyMTU(double var1, String unit) {
		if (mtu.name.compareToIgnoreCase("local") == 0) {
			mtu.value = var1;
			mtuUnit = unit;
			return;
		}
		throw new Error("Attempt to modify the MTU of " + name + ", instead variable " + mtu.name + " should be modified");
	}

	/** set and get queue parameters. Need to check for the queue type
	 *  network link has two queues for forward traffic and reverse traffic
	 */
	public void setArrRate(String lan1, String lan2, double arate) {
		if ((linkQforward == null) || (linkQreverse == null)) {
			return;
		}
		if (isForwardLink(lan1, lan2) == true) {
			linkQforward.avgArrivalRate.setValue(arate);
		} else {
			linkQreverse.avgArrivalRate.setValue(arate);
		}
	}

	public double getArrRate(String lan1, String lan2) {
		if ((linkQforward == null) || (linkQreverse == null)) {
			return 0;
		}
		if (isForwardLink(lan1, lan2) == true) {
			return linkQforward.avgArrivalRate.getValue();
		} else {
			return linkQreverse.avgArrivalRate.getValue();
		}
	}

	public double getResponseTime(String lan1, String lan2) {
		if ((linkQforward == null) || (linkQreverse == null)) {
			return 0;
		}
		if (isForwardLink(lan1, lan2) == true) {
			return linkQforward.avgRespTime.getValue();
		} else {
			return linkQreverse.avgRespTime.getValue();
		}
	}

	public void setThroughput(String lan1, String lan2, double thru) {
		if ((linkQforward == null) || (linkQreverse == null)) {
			return;
		}
		if (isForwardLink(lan1, lan2) == true) {
			linkQforward.avgThroughput.setValue(thru);
		} else {
			linkQreverse.avgThroughput.setValue(thru);
		}
	}

	public double getThroughput(String lan1, String lan2) {
		if ((linkQforward == null) || (linkQreverse == null)) {
			return 0;
		}
		if (isForwardLink(lan1, lan2) == true) {
			return linkQforward.avgThroughput.getValue();
		} else {
			return linkQreverse.avgThroughput.getValue();
		}
	}

	public double getBlockingProbability(String lan1, String lan2) {
		if ((linkQforward == null) || (linkQreverse == null)) {
			return 0;
		}
		if (isForwardLink(lan1, lan2) == true) {
			return linkQforward.getBlockingProbability();
		} else {
			return linkQreverse.getBlockingProbability();
		}
	}

	public void setUtilization(String lan1, String lan2, double util) {
		if ((linkQforward == null) || (linkQreverse == null)) {
			return;
		}
		if (isForwardLink(lan1, lan2) == true) {
			linkQforward.avgUtil.setValue(util);
		} else {
			linkQreverse.avgUtil.setValue(util);
		}
	}

	public double getUtilization(String lan1, String lan2) {
		if ((linkQforward == null) || (linkQreverse == null)) {
			return 0;
		}
		if (isForwardLink(lan1, lan2) == true) {
			return linkQforward.avgUtil.getValue();
		} else {
			return linkQreverse.avgUtil.getValue();
		}
	}

	public void setAvgWaitingTime(String lan1, String lan2, double wtime) {
		if ((linkQforward == null) || (linkQreverse == null)) {
			return;
		}
		if (isForwardLink(lan1, lan2) == true) {
			linkQforward.avgWaitingTime.setValue(wtime);
		} else {
			linkQreverse.avgWaitingTime.setValue(wtime);
		}
	}

	public double getAvgWaitingTime(String lan1, String lan2) {
		if ((linkQforward == null) || (linkQreverse == null)) {
			return 0;
		}
		if (isForwardLink(lan1, lan2) == true) {
			return linkQforward.avgWaitingTime.getValue();
		} else {
			return linkQreverse.avgWaitingTime.getValue();
		}
	}

	public double getAvgServiceTime(String lan1, String lan2) {
		if ((linkQforward == null) || (linkQreverse == null)) {
			return 0;
		}
		if (isForwardLink(lan1, lan2) == true) {
			return linkQforward.avgServiceTime.getValue();
		} else {
			return linkQreverse.avgServiceTime.getValue();
		}
	}

	public void setAvgServiceTime(String lan1, String lan2, double t) {
		if ((linkQforward == null) || (linkQreverse == null)) {
			return;
		}
		if (isForwardLink(lan1, lan2) == true) {
			linkQforward.avgServiceTime.setValue(t);
		} else {
			linkQreverse.avgServiceTime.setValue(t);
		}
	}

	public String getDeviceName() {
		return name;
	}

	public void setAvgQueueLength(String lan1, String lan2, double qlen) {
		if ((linkQforward == null) || (linkQreverse == null)) {
			return;
		}
		if (isForwardLink(lan1, lan2) == true) {
			linkQforward.avgQueueLen.setValue(qlen);
		} else {
			linkQreverse.avgQueueLen.setValue(qlen);
		}
	}

	public double getAvgQueueLength(String lan1, String lan2) {
		if ((linkQforward == null) || (linkQreverse == null)) {
			return 0;
		}
		if (isForwardLink(lan1, lan2) == true) {
			return linkQforward.avgQueueLen.getValue();
		} else {
			return linkQreverse.avgQueueLen.getValue();
		}
	}

	public void print() {
		System.out.println("Lan link " + name + " Src:" + srclan + " dest:" + destlan);
		System.out.println(" MTU :" + mtu.getValue() + " " + mtuUnit);
		System.out.println(" Transmission rate:" + trans.getValue() + " " + transUnit);
		System.out.println(" Propagation delay:" + prop.getValue() + " " + propUnit);
		System.out.println(" Header Size:" + headerSize.getValue() + " " + headerSizeUnit);
	}

	/** check whether link is forward or reverse */
	protected boolean isForwardLink(String lan1, String lan2) {
		if ((srclan.compareToIgnoreCase(lan1) == 0) && (destlan.compareToIgnoreCase(lan2) == 0))
			return true;
		if ((destlan.compareToIgnoreCase(lan1) == 0) && (srclan.compareToIgnoreCase(lan2) == 0))
			return false;
		throw new Error("Undefined link with lan names " + lan1 + " and " + lan2);
	}

	/** used by analytical part.
	 * @author niranjan
	 */
	public void initialize() {
		forward_holdingTime = 0;
		backward_holdingTime = 0;
		linkQforward = new Queue();
		linkQreverse = new Queue();
		linkQforward.initialize();
		linkQreverse.initialize();
	}
	
	public Queue getResourceQueue(String lan1, String lan2) {
		if (isForwardLink(lan1, lan2) == true) {
			return linkQforward;
		} else {
			return linkQreverse;
		}
	}
}
