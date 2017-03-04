package perfcenter.baseclass;
import org.apache.log4j.Logger;

import perfcenter.baseclass.enums.MigrationPolicyType;
import perfcenter.baseclass.enums.MigrationTechnique;
public class MigrationPolicyInfo {
	public MigrationPolicyType type;
	public String vmname;
	public double policyarg; 
	public String destpmname;
	public MigrationTechnique technique;
	
	private Logger logger = Logger.getLogger("Scenario");
	
	public MigrationPolicyInfo(MigrationPolicyType _type){
		type = _type;
	}
	
	public void addVmName(String _vmname){
		vmname = _vmname;
	}
	
	public void addDestPmName(String _destpmname){
		destpmname = _destpmname;
	}
	
	public void addPolicyArg(Variable v){
		policyarg = v.value;
	}
	
	public void addMigrationTechnique(MigrationTechnique t){
		technique = t;
	}
	
	public void print(){
		logger.info("Policy Type:" + type.toString());
		logger.info("Policy arg:" + String.valueOf(policyarg));
		logger.info("Vmname:" + vmname);
		logger.info("Destpmname:" + destpmname);
	}
}
