package perfcenter.simulator.virtualization;

import perfcenter.simulator.SoftServerSim;

public class StopAndCopy implements MigrationTechniqueIface {

	@Override
	public double computeDownTime(SoftServerSim srvr, double lnkspeed) {
		return srvr.currRamUtil / (lnkspeed/1000000.0);
	}

}
