package perfcenter.simulator.migrationpolicy;

abstract public class MigrationPolicy {
	public abstract boolean migrationRequired();
	public abstract String getVmName();
	public abstract String getTargetPmName();
}
