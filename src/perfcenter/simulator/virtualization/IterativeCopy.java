package perfcenter.simulator.virtualization;

import perfcenter.simulator.SoftServerSim;

public class IterativeCopy implements MigrationTechniqueIface {

	@Override
	public double computeDownTime(SoftServerSim srvr, double lnkspeed) {
		//Modify this
		return srvr.currRamUtil / (lnkspeed/1000000.0);
	}

}
