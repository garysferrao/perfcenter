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
		resourceQueue.averageArrivalRate.setValue(arate);
	}

	public double getAverageArrivalRate() {
		return resourceQueue.averageArrivalRate.getValue();
	}

	public void setAverageResponseTime(double rtime) {
		resourceQueue.averageResponseTime.setValue(rtime);
	}

	public double getAvgResponseTime() {
		return resourceQueue.averageResponseTime.getValue();
	}

	public double getBlockingProbability() {
		return resourceQueue.getBlockingProbability();
	}

	public double getAvgServiceTime() {
		return resourceQueue.averageServiceTime.getValue();
	}

	public void setAvgWaitingTime(double wtime) {
		resourceQueue.averageWaitingTime.setValue(wtime);
	}

	public double getAvgWaitingTime() {
		return resourceQueue.averageWaitingTime.getValue();
	}

	public double getAvgQueueLength() {
		return resourceQueue.averageQueueLength.getValue();
	}

	public void setAvgQueueLength(double len) {
		resourceQueue.averageQueueLength.setValue(len);
	}

	public void setUtilization(double util) {
		resourceQueue.averageUtilization.setValue(util);
	}

	public double getUtilization() {
		return resourceQueue.averageUtilization.getValue();
	}

	public void setThroughput(double thru) {
		resourceQueue.averageThroughput.setValue(thru);
	}

	public double getThroughput() {
		return resourceQueue.averageThroughput.getValue();
	}

	public void setAvgServiceTime(double t) {
		resourceQueue.averageServiceTime.setValue(t);
	}

	public double getAveragePowerConsumed() {
		return resourceQueue.averagePowerConsumed.getValue();
	}
	
	public Queue getResourceQueue() {
		return resourceQueue;
	}

}