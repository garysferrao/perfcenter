package perfcenter.simulator.metric;

import java.util.Set;

/**
 * Contains the skeleton methods and variables for concrete implementation of MetricSim classes.
 * @author bhavin
 *
 */
abstract public class MetricSim {
	public abstract void computeConfIvalsAtEndOfRepl();

	public abstract double getMean(int slot);

	public abstract double getMean(int slot, String serverName);

	public abstract double getCI(int slot);

	public abstract double getCI(int slot, String serverName);

	public abstract double getProbability(int slot);

	public abstract Set<String> getServerList(int slot);
}

/**
 * Contain Slot Level methods
 * @author nadeesh
 * 
 */

abstract class _MetricSimSlotLevel {
	public abstract double getMean(String name);
	public abstract double getCI(String serverName);
	public abstract Set<String> getServerList();
}
/**
 * Contain Lowest Level methods
 * @author nadeesh
 */
abstract class _MetricSimLowestLevel {
	protected ConfInterval confInterval;

	public _MetricSimLowestLevel(double probability) {
		super();
		confInterval = new ConfInterval(probability);
	}

	abstract public void recordCISample();

	public final void clearEverything() {
		clearValuesButKeepConfInts();
		confInterval = new ConfInterval(confInterval.prob);
	}

	abstract public void clearValuesButKeepConfInts();

	public final void calculateConfidenceIntervalsAtTheEndOfReplications() {
		confInterval.calculateConfidenceIntervals();
	}

	public double getProbability() {
		return confInterval.prob;
	}

	public double getCI() {
		return confInterval.getCI();
	}

	public abstract double getMean();
}