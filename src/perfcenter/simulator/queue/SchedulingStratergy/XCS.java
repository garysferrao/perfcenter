package perfcenter.simulator.queue.SchedulingStratergy;

import java.util.HashMap;

import perfcenter.simulator.HostSim;
import perfcenter.simulator.ScenarioSim;
import perfcenter.simulator.queue.QueueServer;
import perfcenter.simulator.queue.QueueSim;
import perfcenter.simulator.request.Request;

public class XCS extends QueueSim{
	private int cap = 16000; //FIXME: No hard coding please
	public HashMap<String, Integer> creditMap = new HashMap<String, Integer>(); //Softservername and their credits

	//private HashMap
	public XCS(Integer buffSize, Integer numInstances, /* String resType, */QueueServer qs) {
		super(buffSize, numInstances,/* resType, */qs);
//		
////		for(int credit:credits){
////			credit = cap;
////		}
	}
	
	public void enqueue(Request req, double currTime) throws Exception {
		int idleDeviceId = getIdleInstanceId();

		//Mark time when request enters the system
		processRequestArrival(req, idleDeviceId, currTime);
		
		//Check if any instance of device is free
		if (idleDeviceId == -1) {
			//No device is idle, now check if buffer is full
			if (isBufferFull()) {
				//Discard request as buffer is full
				totalNumberOfRequestsBlocked.recordValue(req,1);
				qServer.dropRequest(req, currTime);
			} else {
				// Yay, there is space in buffer.
				addRequestToBuffer(req, currTime);
			}
		} else {
			//Some instance of device is free, so schedule the request
			//ADDHERE xcs specific things here
			/********/
			if(creditMap.size() == 0){
				
			}else if(creditMap.containsKey(req.hostObject.getServer(req.softServerName))){
				creditMap.put(req.hostObject.getServer(req.softServerName).name, cap);
				
			}
			
			
			/********/
			req.qServerInstanceID = idleDeviceId;
			createStartTaskEvent(req, idleDeviceId, currTime);
			//Update the average waiting time for this resource
			averageWaitingTimeSim.recordValue(req,qServerInstances.get(idleDeviceId).reqStartTime - qServerInstances.get(idleDeviceId).reqArrivalTime);
		}

	}
	// This is significantly different from it's FCFS counterpart
	public void dequeueAndProcess(double currTime) throws Exception {
		/*
		 * 1. Check the head request in the queue
		 * 2. If softserver's status of this request is "under" then (i)Decrease the credit of softserver by 100
		 *    (ii) If new credits of softserver < 0, change status of it to "over" and return the request 
		 * 3. Otherwise("over" case), check each runnable and running softserver's status, if status of every
		 *    softserver is "Over", then and only then return it. Otherwise don't return it.    
		 */
		Request req = getRequestFromBuffer(0, currTime);
	}
}

