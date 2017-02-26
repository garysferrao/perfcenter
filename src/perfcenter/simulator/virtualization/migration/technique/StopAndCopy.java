package perfcenter.simulator.virtualization.migration.technique;

import perfcenter.simulator.SoftServerSim;

public class StopAndCopy implements MigrationTechniqueIface {
	public double computeDownTime(SoftServerSim srvr, double lnkspeed){
		return srvr.getCurrRamUtil()/lnkspeed;
	}
}
