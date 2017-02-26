package perfcenter.simulator.virtualization.migration.policy;

import perfcenter.simulator.virtualization.migration.technique.MigrationTechniqueIface;
import perfcenter.simulator.SoftServerSim;

import java.lang.reflect.Constructor;

import perfcenter.baseclass.enums.MigrationTechnique;

abstract public class MigrationPolicy {
	public abstract boolean migrationRequired();
	public abstract String getVmName();
	public abstract String getTargetPmName();
	private MigrationTechniqueIface technique;
	public void loadMigrationTechnique(MigrationTechnique techniquename){
		try {
			Class c = Class.forName("perfcenter.simulator.virtualization.migration.technique." + techniquename.toString());

			Constructor cons = c.getConstructor();
			technique = (MigrationTechniqueIface) cons.newInstance();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public double computeDownTime(SoftServerSim srvr, double lnkspeed){
		return technique.computeDownTime(srvr, lnkspeed);
	}
}
