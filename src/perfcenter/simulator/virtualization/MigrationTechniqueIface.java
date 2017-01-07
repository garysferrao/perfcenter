package perfcenter.simulator.virtualization;
import perfcenter.simulator.SoftServerSim;

public interface MigrationTechniqueIface {
	public double computeDownTime(SoftServerSim srvr, double lnkspeed);
}

