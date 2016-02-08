package perfcenter.parser;

import perfcenter.baseclass.Distribution;
import perfcenter.baseclass.ModelParameters;
import perfcenter.baseclass.SoftServer;
import perfcenter.baseclass.Task;
import perfcenter.baseclass.Variable;
import perfcenter.baseclass.enums.SchedulingPolicy;
import perfcenter.baseclass.exception.DeviceNotFoundException;

/**
 * Implements set statement
 * 
 * @author akhila
 */
public class SetStmt {

	String host, device, server, lan1, lan2, taskName, scenario, var, mtuUnit, propUnit, transUnit;

	Distribution dist;

	SchedulingPolicy pol;

	double mtu = -1, prop = -1, trans = -1, varval = -1, prob = -1;

	double count = -1, buffer = -1;

	// these are to modify PM device attributes: rakesh
	double probe_interval = -1;
//	double gov_up_threshold = -1; //BHAVIN
	Variable gov_up_threshold;
//	double gov_down_threshold = -1; //BHAVIN
	Variable gov_down_threshold;

	public int lineno;

	public SetStmt(int lno) {
		lineno = lno;
		ModelParameters.isModified = true;
	}

	public SetStmt() {
		ModelParameters.isModified = true;
	}

	public void addCount(double cnt) {
		count = cnt;
	}

	public void addProbability(double p) {
		prob = p;
	}

	public void addBuffer(double buf) {
		buffer = buf;
	}

	// probe interval of a PM device can be changed.
	public void addProbeInterval(Variable pro_int) {
		probe_interval = pro_int.getValue();
	}

	// Function to change governor up_threshold.
	public void addGovUpThreshold(Variable up_thresh) {
//		gov_up_threshold = up_thresh.getValue();
		gov_up_threshold = up_thresh;
	}

	// Function to change governor down_threshold.
	public void addGovDownThreshold(Variable down_thresh) {
//		gov_down_threshold = down_thresh.getValue(); //BHAVIN
		gov_down_threshold = down_thresh;
	}

	public void addMTU(double m, String unit) {
		mtu = m;
		mtuUnit = unit;
	}

	public void addPropDelay(double d, String unit) {
		prop = d;
		propUnit = unit;
	}

	public void addTransRate(double t, String unit) {
		trans = t;
		transUnit = unit;
	}

	public void addMachine(String name) {
		host = name;
	}

	public void addVariable(String var1) {
		var = var1;
	}

	public void addVariableValue(double val) {
		varval = val;
	}

	public void addDevice(String name) {
		device = name;
	}

	public void addServer(String name) {
		server = name;
	}

	public void addScenario(String name) {
		scenario = name;
	}

	public void addLan(String name) {
		if (lan1 == null)
			lan1 = name;
		lan2 = name;
	}

	public void addTask(String name) {
		taskName = name;
	}

	public void addSchedulingPolicy(SchedulingPolicy p) {
		pol = p;
	}

	public void addServiceTime(Distribution d) {
		dist = d;
	}

	public void execute() throws DeviceNotFoundException, Exception {
		ModelParameters.isModified = true;
		try {
			if (host != null) {
				if (server != null) {
					if (count >= 0) {
						((SoftServer) ModelParameters.inputDistSys.getMachine(host).getServer(server)).modifyThreadCount(count);
						return;
					} else if (buffer >= 0) {
						((SoftServer) ModelParameters.inputDistSys.getMachine(host).getServer(server)).modifyThreadBuffer(buffer);
						return;
					} else if (pol != null) {
						((SoftServer) ModelParameters.inputDistSys.getMachine(host).getServer(server)).setSchedPolicy(pol);
						return;
					}

				} else if (device != null) {
					if (count >= 0) {
						ModelParameters.inputDistSys.getMachine(host).modifyDeviceCount(device, count);
						return;
					} else if (buffer >= 0) {
						ModelParameters.inputDistSys.getMachine(host).modifyDeviceBuffer(device, buffer);
						return;
					} else if (pol != null) {
						ModelParameters.inputDistSys.getMachine(host).modifyDeviceSchedPol(device, pol);
						return;
					} else if (probe_interval > 0) { // modify probe interval of device, added by rakesh
						ModelParameters.inputDistSys.getMachine(host).modifyProbeInterval(device, probe_interval);
						return;
					} else if (gov_up_threshold.getValue() > 0) { // modify governor up threshold of device, added by rakesh //BHAVIN
						ModelParameters.inputDistSys.getMachine(host).modifyGovUpThreshold(device, gov_up_threshold.getValue()); //BHAVIN
						return;
					} else if (gov_down_threshold.getValue() > 0) {// modify governor down threshold of device, added by rakesh //BHAVIN
//						ModelParameters.inputDistributedSystem.getHost(host).modifyGovDownThreshold(device, gov_down_threshold);
						ModelParameters.inputDistSys.getMachine(host).modifyGovDownThreshold(device, gov_down_threshold.getValue()); //BHAVIN
						return;
					}
				}
			} else if (lan1 != null) {
				if (mtu > 0) {
					ModelParameters.inputDistSys.getLink(lan1, lan2).modifyMTU(mtu, mtuUnit);
					return;
				} else if (prop >= 0) {
					ModelParameters.inputDistSys.getLink(lan1, lan2).modifyPropDelay(prop, propUnit);
					return;
				} else if (trans >= 0) {
					ModelParameters.inputDistSys.getLink(lan1, lan2).modifyTransRate(trans, transUnit);
					return;
				}
			} else if (taskName != null) {
				ModelParameters.inputDistSys.getTask(taskName).modifyServiceTime(device, dist);
			} else if (var != null) {
				ModelParameters.inputDistSys.getVariable(var).setValue(varval);
			} else if (scenario != null) {
				ModelParameters.inputDistSys.getScenario(scenario).modifyProbability(prob);
			}
		} catch (Error e) {
			throw new Error("Line no:" + lineno + " " + e.getMessage());
		}
	}
}
