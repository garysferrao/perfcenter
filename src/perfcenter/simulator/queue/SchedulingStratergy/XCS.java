package perfcenter.simulator.queue.SchedulingStratergy;

import java.util.HashMap;

import perfcenter.simulator.DeviceSim;
import perfcenter.simulator.SoftServerSim;
import perfcenter.simulator.SoftResSim;
import perfcenter.simulator.MachineSim;
import perfcenter.simulator.ScenarioSim;
import perfcenter.simulator.queue.QueueServer;
import perfcenter.simulator.queue.QueueSim;
import perfcenter.simulator.request.Request;

public class XCS extends QueueSim{	

	//This is incomplete. QuantumOver event needs to be added. 
	public XCS(Integer buffSize, Integer numInstances, /* String resType, */QueueServer qs) {
		super(buffSize, numInstances,/* resType, */qs);
//				
	}
	
	public void enqueue(Request req, double currTime) throws Exception {
		int idleDeviceId = getIdleInstanceId();

		//Mark time when request enters the system
		bookkeepRequestArrival(req, idleDeviceId, currTime);
		
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
			int cap = req.machineObject.cap;
			int deduction = req.machineObject.deduction;
			HashMap<String, Integer> creditMap;
			HashMap<String, Integer> statusMap;
			String name;
			if(qServer instanceof DeviceSim){
				creditMap = req.machineObject.deviceCreditMap;
				statusMap = req.machineObject.deviceStatusMap;
				name = req.devName;
			}else if(qServer instanceof SoftServerSim){
				creditMap = req.machineObject.softServerCreditMap;
				statusMap = req.machineObject.softServerStatusMap;
				name = req.softServName;
			}else{
				assert qServer instanceof SoftResSim == true;
				creditMap = req.machineObject.virtResCreditMap;
				statusMap = req.machineObject.virtResStatusMap;
				name = req.softResName;
			}
			int tempCredit = creditMap.get(name);
			int tempStatus = statusMap.get(name);
			if(tempStatus == 1){
				creditMap.put(name, new Integer(tempCredit-deduction));
				if(tempCredit-deduction >= 0)
					statusMap.put(name, 1);
				else
					statusMap.put(name, 0);
				/**********/	
				req.qServerInstanceID = idleDeviceId;
				createStartTaskEvent(req, idleDeviceId, currTime);
				//Update the average waiting time for this resource
				averageWaitingTimeSim.recordValue(req,qServerInstances.get(idleDeviceId).reqStartTime - qServerInstances.get(idleDeviceId).reqArrivalTime);
				
				/**********/
			}else{
				//FIXME If status of this softServer is "under", then check for all softServer's status. 
				// If one of them has status "over", then schedule task for that softServer
				//else assign this server cpu instance.
			}
			
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
		int idleDeviceId = getIdleInstanceId();
		/*************/
		int cap = req.machineObject.cap;
		int deduction = req.machineObject.deduction;
		HashMap<String, Integer> creditMap;
		HashMap<String, Integer> statusMap;
		String name;
		if(qServer instanceof DeviceSim){
			creditMap = req.machineObject.deviceCreditMap;
			statusMap = req.machineObject.deviceStatusMap;
			name = req.devName;
		}else if(qServer instanceof SoftServerSim){
			creditMap = req.machineObject.softServerCreditMap;
			statusMap = req.machineObject.softServerStatusMap;
			name = req.softServName;
		}else{
			assert qServer instanceof SoftResSim == true;
			creditMap = req.machineObject.virtResCreditMap;
			statusMap = req.machineObject.virtResStatusMap;
			name = req.softResName;
		}
		int tempCredit = creditMap.get(name);
		int tempStatus = statusMap.get(name);
		if(tempStatus == 1){
			creditMap.put(name, new Integer(tempCredit-deduction));
			if(tempCredit-deduction >= 0)
				statusMap.put(name, 1);
			else
				statusMap.put(name, 0);
		/********************/
			req.qServerInstanceID = idleDeviceId;
			createStartTaskEvent(req, idleDeviceId, currTime);
			//Update the average waiting time for this resource
			averageWaitingTimeSim.recordValue(req,qServerInstances.get(idleDeviceId).reqStartTime - qServerInstances.get(idleDeviceId).reqArrivalTime);
		/*********************/	
		}else{
			//"over" status
			//FIXME If status of this softServer is "under", then check for all softServer's status. 
			// If one of them has status "over", then schedule task for that softServer
			//else assign this server cpu instance.
		}
		
		/******************/
		
		
	}
}

