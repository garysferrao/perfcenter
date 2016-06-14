package perfcenter.baseclass;
import perfcenter.baseclass.enums.MigrationPolicyType;
public class MigrationPolicyInfo {
	public MigrationPolicyType type;
	public String vmname;
	public double policyarg; 
	public String destpmname;
	
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
	
	public void print(){
		System.out.println("Policy Type:" + type.toString());
		System.out.println("Policy arg:" + String.valueOf(policyarg));
		System.out.println("Vmname:" + vmname);
		System.out.println("Destpmname:" + destpmname);
	}
}
