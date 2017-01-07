package perfcenter.baseclass;

/**
 * This class abstracts the queuing resources like Device, LanLink and SoftServer.
 * 
 * Many of the methods and functionality is similar across these classes, and
 * this abstraction was created to reduce the code redundancy in the project.
 */
public abstract class QueuingResource {

	/** Underlying queue of this queuing resource, which will actually serve the requests. */
	public Queue resourceQueue = new Queue();

	public QueuingResource() {
		super();
	}

	public void setAverageArrivalRate(double arate) {
		resourceQueue.avgArrivalRate.setValue(arate);
	}

	public double getAverageArrivalRate() {
		return resourceQueue.avgArrivalRate.getValue();
	}

	public void setAverageResponseTime(double rtime) {
		resourceQueue.avgRespTime.setValue(rtime);
	}

	public double getAvgResponseTime() {
		return resourceQueue.avgRespTime.getValue();
	}

	public double getBlockingProbability() {
		return resourceQueue.getBlockingProbability();
	}

	public double getAvgServiceTime() {
		return resourceQueue.avgServiceTime.getValue();
	}

	public void setAvgWaitingTime(double wtime) {
		resourceQueue.avgWaitingTime.setValue(wtime);
	}

	public double getAvgWaitingTime() {
		return resourceQueue.avgWaitingTime.getValue();
	}

	public double getAvgQueueLength() {
		return resourceQueue.avgQueueLen.getValue();
	}

	public void setAvgQueueLength(double len) {
		resourceQueue.avgQueueLen.setValue(len);
	}

	public void setUtilization(double util) {
		resourceQueue.avgUtil.setValue(util);
	}

	public double getUtilization() {
		return resourceQueue.avgUtil.getValue();
	}

	public void setThroughput(double thru) {
		resourceQueue.avgThroughput.setValue(thru);
	}

	public double getThroughput() {
		return resourceQueue.avgThroughput.getValue();
	}

	public void setAvgServiceTime(double t) {
		resourceQueue.avgServiceTime.setValue(t);
	}

	public double getAveragePowerConsumed() {
		return resourceQueue.avgPowerConsumed.getValue();
	}
	
	public Queue getResourceQueue() {
		return resourceQueue;
	}

}