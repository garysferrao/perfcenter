package perfcenter.baseclass;

import java.util.HashMap;

import perfcenter.baseclass.enums.SolutionMethod;
import perfcenter.baseclass.enums.SystemType;
import perfcenter.simulator.SimulationParameters;

/** Holds value, confidence interval and confidence level of any performance or power metric.
 * 
 * It can hold these values for cyclic interval slots, and per server values. 
 * @author bhavin
 * @author nadeesh
 */
public class Metric {
	
	/** mean value of the metric, array size equal to cyclic workload's cycle length */
	private double[] value;
	/** confidence interval levels of metric, array size equal to cyclic workload's cycle length */
	private double[] confidenceInterval;
	/** confidence probability of the computed values, e.g. 95% etc */
	private double confidenceProbability;

	/** store per Server Metric value */
	private HashMap<Integer, HashMap<String, Double>> slotwisePerServerValue;
	/** store per Server Metric Confidence interval */
	private HashMap<Integer, HashMap<String, Double>> slotwisePerServerCI;

	public Metric() {
		initialize();
	}

	public void initialize() {
		//TODO: initialize structures only for simulation, reduce memory for analytical
		int size = ModelParameters.intervalSlotCount;
		size = size == 0 ? 1 : size;
		value = new double[size];
		confidenceInterval = new double[size];
		for (int i = 0; i < size; i++) {
			value[i] = 0;
			confidenceInterval[i] = 0;
		}
		slotwisePerServerValue = new HashMap<Integer, HashMap<String, Double>>();
		slotwisePerServerCI = new HashMap<Integer, HashMap<String, Double>>();
	}

	public void copy(Metric other) {
		value = new double[other.value.length];
		confidenceInterval = new double[other.confidenceInterval.length];
		for (int i = 0; i < value.length; i++) {
			value[i] = other.value[i];
			confidenceInterval[i] = other.confidenceInterval[i];
		}
		confidenceProbability = other.confidenceProbability;
		slotwisePerServerValue.putAll(other.slotwisePerServerValue);
		slotwisePerServerCI.putAll(other.slotwisePerServerCI);
	}

	public double getValue() {
		if (ModelParameters.solutionMethod == SolutionMethod.ANALYTICAL) {
			return value[0];
		} else {
			return value[SimulationParameters.getIntervalSlotCounter()];
		}
	}

	public double getValue(int slot) {
		return value[slot];
	}

	/**
	 * Retrieve perServer metric value
	 * 
	 * @author nadeesh
	 */
	public double getValue(int slot, String serverName) {
		if (this.slotwisePerServerValue.containsKey(slot)) {
			if (this.slotwisePerServerValue.get(slot).containsKey(serverName)) {
				return this.slotwisePerServerValue.get(slot).get(serverName).doubleValue();
			}
		}
		return 0;
	}

	public void setValue(double value) {
		if (ModelParameters.solutionMethod == SolutionMethod.ANALYTICAL) {
			this.value[0] = value;
		} else {
			this.value[SimulationParameters.getIntervalSlotCounter()] = value;
		}
	}

	public void setValue(int slot, double value) {
		this.value[slot] = value;
	}

	/**
	 * Set Value of the metric per Server level
	 * @author:nadeesh 
	 */
	public void setValue(int slot, String serverName, double value) {
		if (this.slotwisePerServerValue.containsKey(slot)) {

			this.slotwisePerServerValue.get(slot).put(serverName, value);

		} else {
			HashMap<String, Double> hMap = new HashMap<String, Double>();
			hMap.put(serverName, value);
			this.slotwisePerServerValue.put(slot, hMap);
		}
	}

	public double getConfidenceInterval() {
		if (ModelParameters.solutionMethod == SolutionMethod.ANALYTICAL) {
			return confidenceInterval[0];
		} else {
			return confidenceInterval[SimulationParameters.getIntervalSlotCounter()];
		}
	}

	public double getConfidenceInterval(int slot) {
		return confidenceInterval[slot];
	}

	
	public double getConfidenceInterval(int slot, String serverName) {
		if (this.slotwisePerServerCI.containsKey(slot)) {
			if (this.slotwisePerServerCI.get(slot).containsKey(serverName)) {
				return this.slotwisePerServerCI.get(slot).get(serverName).doubleValue();
			}
		}
		return 0;
	}

	public void setConfidenceInterval(double confidenceInterval) {
		if (ModelParameters.solutionMethod == SolutionMethod.ANALYTICAL) {
			this.confidenceInterval[0] = confidenceInterval;
		} else {
			this.confidenceInterval[SimulationParameters.getIntervalSlotCounter()] = confidenceInterval;
		}
	}

	public void setConfidenceInterval(int slot, double confidenceInterval) {
		this.confidenceInterval[slot] = confidenceInterval;
	}



	/**
	 * Set Confidence Interval perServerLevel
	 * 
	 * @author nadeesh 
	 */
	public void setConfIval(int slot, String serverName, double confidenceInterval) {
		if (this.slotwisePerServerCI.containsKey(slot)) {
			this.slotwisePerServerCI.get(slot).put(serverName, confidenceInterval);

		} else {
			HashMap<String, Double> hMap = new HashMap<String, Double>();
			hMap.put(serverName, confidenceInterval);
			this.slotwisePerServerCI.put(slot, hMap);
		}
	}

	public double getConfidenceProbability() {
		return confidenceProbability;
	}

	public void setConfidenceProbability(double confidenceProbability) {
		this.confidenceProbability = confidenceProbability;
	}

	public void clear() {
		initialize();
	}

	private void print(double print_time, int slot, double load, StringBuilder stringBuilder) {
		stringBuilder.append(print_time + " " + toString(slot) + " " + load + "\n");
	}

	public String toString(int slot) {
		if(slot == -1) {
			return toString();
		} else {
			return (ModelParameters.confIvalsEnabled && ModelParameters.solutionMethod != SolutionMethod.ANALYTICAL) ? value[slot] + "+-" + confidenceInterval[slot] : Double.toString(value[slot]);
		}
	}

	public String toString() {
	
		if (value.length == 1) {
			return toString(0);
		} else {
			double print_time = 0;
			Variable[] loadDetails = null;
			loadDetails = SystemType.CLOSED == ModelParameters.getSystemType() ? ModelParameters.noOfUsersCyclic : ModelParameters.arrivalRatesCyclic;
			StringBuilder returnString = new StringBuilder(1000);
			for (int slot = 0; slot < value.length; slot++) {
				print_time += ModelParameters.ivalSlotDurCyclic[slot].value;
				print(print_time, slot, loadDetails[slot].value, returnString);
			}
			returnString.deleteCharAt(returnString.length()-1); //remove the last newline
			return returnString.toString();
		}
	}

	/**
	 * Convert Server Level Metric to String value
	 * 
	 * @author nadeesh
	 */
	public String toString(int slot, String serverName) {
		String value = "0";
		String ci = "0";
		if (this.slotwisePerServerValue.containsKey(slot)) {
			if (this.slotwisePerServerValue.get(slot).containsKey(serverName)) {
				value = "" + this.slotwisePerServerValue.get(slot).get(serverName).doubleValue();
			}
		}
		if (ModelParameters.confIvalsEnabled) {
			if (this.slotwisePerServerCI.containsKey(slot)) {
				if (this.slotwisePerServerCI.get(slot).containsKey(serverName)) {
					ci = "" + this.slotwisePerServerCI.get(slot).get(serverName).doubleValue();
				}
			}
			return value + "+-" + ci;
		} else {
			return value;
		}

	}

	public String toString(String serverName) {
		if (value.length == 1) {
			return toString(0, serverName);
		} else {
			double print_time = 0;
			Variable[] loadDetails = null;
			loadDetails = SystemType.CLOSED == ModelParameters.getSystemType() ? ModelParameters.noOfUsersCyclic : ModelParameters.arrivalRatesCyclic;
			StringBuilder returnString = new StringBuilder(1000);
			for (int slot = 0; slot < value.length; slot++) {
				print_time += ModelParameters.ivalSlotDurCyclic[slot].value;
				returnString.append(print_time + " " + toString(slot, serverName) + " " + loadDetails[slot].value + "\n");
			}
			
			return returnString.toString();

		}
	}

}
