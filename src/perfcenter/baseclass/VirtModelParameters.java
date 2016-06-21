package perfcenter.baseclass;

import perfcenter.baseclass.enums.SchedulingPolicy;

public class VirtModelParameters {
	public static double hypervisorStaticSize = 50;
	public static double hypervisorThreadSize = 5;
	public static double hypervisorThreadCount = 100;
	public static double hypervisorThreadBufferSize = 100;
	
	public static SchedulingPolicy hypervisorSchedP = SchedulingPolicy.FCFS;
	
	public static double vmStaticSize = 100;
	public static double vmThreadSize = 10;

	public static double networkingOverhead = 0.002;
	public static String nwOverheadDist = "const"; 
	public static double ioOverhead = 0.002;
	public static String ioOverheadDist = "const";
}