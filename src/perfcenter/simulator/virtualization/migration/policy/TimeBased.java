package perfcenter.simulator.virtualization.migration.policy;

import perfcenter.baseclass.enums.MigrationTechnique;
import perfcenter.simulator.virtualization.migration.policy.MigrationPolicy;
import perfcenter.simulator.SimulationParameters;

public class TimeBased extends MigrationPolicy{
	private double nextMigrationTs;
	private boolean migrationToBeDone;
	private String vmname;
	private String destPmName;
	public TimeBased(Double _nextMigrationTs, String _vmname, String _destPmName, MigrationTechnique techniquename){
		nextMigrationTs = _nextMigrationTs;
		vmname = _vmname;
		destPmName = _destPmName;
		migrationToBeDone = true;
		loadMigrationTechnique(techniquename);
	}
	public boolean migrationRequired(){
		if(SimulationParameters.currTime > nextMigrationTs && migrationToBeDone){
			migrationToBeDone = false;
			return true;
		}
		return false;
	}
	
	public String getVmName(){
		if(vmname == null){
			throw new Error("ERROR: something went wrong, migration was not required.");
		}
		String ret = vmname;
		vmname = null;
		return ret;
	}
	
	public String getTargetPmName(){
		if(destPmName == null){
			throw new Error("ERROR: something went wrong, migration was not required.");
		}
		String ret = destPmName;
		destPmName = null;
		return ret;
	}
}
