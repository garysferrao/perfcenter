package perfcenter.parser;

import perfcenter.baseclass.ModelParameters;
import perfcenter.output.Output;

/**
 * 
 * @author akhila
 * 
 */
public class FunctionDefinition {

	String name_; // Function name
	String scenario;
	String tempname1;
	String tempname2; // can be device/softserver name
	String tempname3; // can be device.
	int slot = -1; // can be slot number
	String message;
	Double num = 0.0;
	public int lineno;

	public FunctionDefinition() {
	}

	public void addTemp1(String name) {
		tempname1 = name;
	}

	public void addTemp2(String name) {
		tempname2 = name;
		tempname3 = ")"; //if tempname3 is changed then we can recognize that third parameter is specified.
	}

	public void addTemp3(String name) {
		tempname3 = name;
	}
	
	public void addSlot(String slotNumber) {
		try {
			slot = Integer.parseInt(slotNumber);
			if (slot >= ModelParameters.intervalSlotCount || slot < 0) {
				throw new Error("Invalid slot number. You gave \""
						+ slot + "\". It must be between 0 and " + (ModelParameters.intervalSlotCount-1));
			}
		} catch (NumberFormatException nfe) {
			throw new Error("Invalid slot number. You gave \""
						+ slotNumber + "\". Slot number must be integer.");
		}
	}

	public void addNumber(Double n) {
		num = n;
	}

	public void addScenario(String name) {
		scenario = name;
	}

	public void addFunction(String name) {
		name_ = name;
	}

	public String execute() throws Exception {
		try {

			// error checking for memory model	
			if (((tempname2 != null && tempname2.compareToIgnoreCase("ram") == 0) 
			  || (tempname3 != null && tempname3.compareToIgnoreCase("ram") == 0))
  			  && name_.compareToIgnoreCase("util") != 0) {
				
				return ("ERROR: '" + name_.toUpperCase() + "' is not supported on ram");
			}

			Output o = new Output();
			
			if (name_.compareTo("printcfg") == 0) {
				ModelParameters.inputDistSys.printConfiguration();
				return "";
			} else if (name_.compareTo("respt") == 0) {
				if (scenario != null) {
					return o.findScenarioResponseTime(slot, scenario);
				} else if (tempname1 != null) {
					return o.findResponseTime(slot, tempname1, tempname2, tempname3);
				}
			} else if (name_.compareTo("blockprob") == 0) {
				if (scenario != null) {
					return o.findScenarioBlockProb(slot, scenario);
				} else if (tempname1 != null) {
					return o.findBlockProb(slot, tempname1, tempname2, tempname3);
				}
			} else if (name_.compareTo("tput") == 0) {

				if (scenario != null) {
					return o.findScenarioThroughput(slot, scenario);
				}

				if (tempname1 != null) {
					return o.findThroughput(slot, tempname1, tempname2, tempname3);
				}
			} else if (name_.compareTo("bput") == 0) {
				if (scenario != null) {
					return o.findScenarioBadput(slot, scenario);
				} else if (tempname1 != null) {
					System.out.println("for given badput parameters, work in Progress");
					// this will execute when senerio name is null, like bput(x:y). but in this case
					// TODO: we should badput of what? is ambuiguity. so resolve it.
					// return o.findScenarioBadput(scenario);
				}
			} else if (name_.compareTo("gput") == 0) {
				if (scenario != null) {
					return o.findScenarioGoodput(slot, scenario);
				} else if (tempname1 != null) {
					System.out.println("for given badput parameters, work in Progress");
					// this will execute when senerio name is null, like bput(x:y). but in this case
					// TODO: we should badput of what? is ambuiguity. so resolve it.
					// return o.findScenarioBadput(scenario);
				}
			} else if (name_.compareTo("buffTimeout") == 0) {
				if (scenario != null) {
					return o.findScenarioBuffTimeout(slot, scenario);
				} else if (tempname1 != null) {
					System.out.println("for given badput parameters, work in Progress");
					// this will execute when senerio name is null, like bput(x:y). but in this case
					// TODO: we should badput of what? is ambuiguity. so resolve it. (To Do)
					// return o.findScenarioBadput(scenario);
				}
			} else if (name_.compareTo("droprate") == 0) {
				if (scenario != null) {
					return o.findScenarioDropRate(slot, scenario);
				} else if (tempname1 != null) {
					System.out.println("for given badput parameters, work in Progress");
					// this will execute when senerio name is null, like bput(x:y). but in this case
					// TODO: we should badput of what? is ambuiguity. so resolve it.
					// return o.findScenarioBadput(scenario);
				}
			} else if (name_.compareTo("arate") == 0) {
				if (scenario != null) {
					return o.findScenarioArrivalRate(slot, scenario);
				} else if (tempname1 != null) {
					return o.findArrivalRate(slot, tempname1, tempname2, tempname3);
				}
			} else if (name_.compareTo("waitt") == 0) {
				return o.findWaitingTime(slot, tempname1, tempname2, tempname3);
			} else if (name_.compareTo("util") == 0) {
				return o.findUtilization(slot, tempname1, tempname2, tempname3);
			} else if (name_.compareTo("qlen") == 0) {
				return o.findQueueLength(slot, tempname1, tempname2, tempname3);
			} else if (name_.compareTo("avgservt") == 0) {
				return o.findAvgServiceTime(slot, tempname1, tempname2, tempname3);
			} else if (name_.compareTo("power") == 0) { // changed energy to power: Bhavin
				return o.findPowerConsumption(slot, tempname1, tempname2, tempname3);
			} else if (name_.compareTo("eperr") == 0) {
				return o.findTotalEnergyConsumptionperRequest(slot, tempname1, tempname2, tempname3);
			} else if (name_.compareTo("pdp") == 0) { // changed edp to pdp: Bhavin
				return o.findPowerDelayProduct(slot, tempname1, tempname2);
			} else if (name_.compareTo("freq") == 0) {
				return o.findAvgFrequency(slot, tempname1, tempname2);
			} else if (name_.compareTo("peff") == 0) {
				return o.findPowerEfficiency(slot, tempname1, tempname2);
			} else if (name_.compareTo("qparms") == 0) { //FIXME figure out how to print output of this function with cyclic Metric hierarchy
				return o.findQueParameters(tempname1, tempname2);
			} else if (name_.compareTo("bottleneck") == 0) {
				if (num < 0) {
					num = 999.0;
				}
				return o.findBottleNeck(num);
			} else if (name_.compareTo("analyse") == 0) {
				if (num < 0) {
					num = 0.99;
				}
				return o.analyseResults(num);
			}
			return "0.0";
		} catch (Error e) {
			System.err.println("Line Number:" + lineno + " " + (e.getMessage()!=null?e.getMessage():""));
			throw e;
		}
	}
}
