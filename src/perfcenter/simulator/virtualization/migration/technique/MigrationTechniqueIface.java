package perfcenter.simulator.virtualization.migration.technique;

import perfcenter.simulator.SoftServerSim;

public interface MigrationTechniqueIface {
	double computeDownTime(SoftServerSim srvr, double lnkspeed);
}
