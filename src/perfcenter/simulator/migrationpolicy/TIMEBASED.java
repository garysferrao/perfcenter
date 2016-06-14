package perfcenter.simulator.migrationpolicy;

import perfcenter.simulator.migrationpolicy.MigrationPolicy;
import perfcenter.simulator.SimulationParameters;

public class TIMEBASED extends MigrationPolicy{
	private static double nextMigrationTs;
	private static boolean migrationToBeDone;
	private static String vmname;
	private static String destPmName;
	public TIMEBASED(Double _nextMigrationTs, String _vmname, String _destPmName){
		nextMigrationTs = _nextMigrationTs;
		vmname = _vmname;
		destPmName = _destPmName;
		migrationToBeDone = true;
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
