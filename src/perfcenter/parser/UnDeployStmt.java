package perfcenter.parser;

import perfcenter.baseclass.PhysicalMachine;
import perfcenter.baseclass.VirtualMachine;
import perfcenter.baseclass.Lan;
import perfcenter.baseclass.ModelParameters;
import perfcenter.baseclass.SoftServer;

/**
 * Undeploys servers from host and hosts from lan
 * 
 * @author akhila
 */
public class UnDeployStmt {
	String name1, name2;

	public UnDeployStmt(String n1, String n2) {
		name1 = n1;
		name2 = n2;
	}

	public void undeploy() throws Exception {
		PhysicalMachine pm;
		ModelParameters.isModified = true;
		try {
			if (ModelParameters.inputDistSys.isServer(name1)) { // if first parameter is softServer then second should be machine
				if (ModelParameters.inputDistSys.isPM(name2)) {
					SoftServer srv = ModelParameters.inputDistSys.getServer(name1);
					pm = ModelParameters.inputDistSys.getPM(name2);
					srv.removeMachine(name2);
					pm.removeServer(srv.name);
					srv.unDeploySoftResOnHost(pm);
				} else if (ModelParameters.inputDistSys.isVM(name2)) {
					SoftServer srv = ModelParameters.inputDistSys.getServer(name1);
					VirtualMachine vmachine = ModelParameters.inputDistSys.getVM(name2);
					srv.removeMachine(name2);
					vmachine.removeServer(srv.name);
					srv.unDeploySoftResOnHost(vmachine);
				}else{
					throw new Error(" \"" + name2 + "\" is neither machine nor vm");
				}
				
				return;
			} else if (ModelParameters.inputDistSys.isPM(name1)) { // if first parameter is machine then second should be lan
				if (ModelParameters.inputDistSys.isLan(name2) == false) {
					throw new Error(" \"" + name2 + "\" is not Lan");
				}
				pm = ModelParameters.inputDistSys.getPM(name1);
				Lan ln = ModelParameters.inputDistSys.getLan(name2);
				pm.removeLan(name2);
				ln.removeMachine(name1);
				return;
			} else if (ModelParameters.inputDistSys.isVM(name1)){ //First Parameter is vmachine name, then second needs to be machine name
				if (ModelParameters.inputDistSys.isPM(name2)) {
					pm = ModelParameters.inputDistSys.getPM(name2);
					if(!pm.virtualizationEnabled){
						throw new Error("virtualization is not supported on " + "\"" + name2 + "\"");
					}
					VirtualMachine vmachine = ModelParameters.inputDistSys.getVM(name1);
					vmachine.host = null;
					pm.removeVM(vmachine);
				}else if(ModelParameters.inputDistSys.isVM(name2)){
					VirtualMachine hostvm = ModelParameters.inputDistSys.getVM(name2);
					if(!hostvm.virtualizationEnabled){
						throw new Error("virtualization is not supported on virtual machine " + "\"" + name2 + "\"");
					}
					VirtualMachine guestvm = ModelParameters.inputDistSys.getVM(name1);
					guestvm.host = null;
					hostvm.removeVM(guestvm);
					System.out.println("UnDeployStmt::" + "vm.name:" + guestvm.name + " hostvm.name:" + hostvm.name);
				}else{
					throw new Error(" \"" + name2 + "\" is neither pm nor vm. ");
				}
			}else {
				throw new Error(" \"" + name1 + "\" is neither machine nor server");
			}
			
		} catch (Error e) {
			throw e;
		}
	}
}
