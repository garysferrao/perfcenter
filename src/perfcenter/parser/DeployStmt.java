package perfcenter.parser;

import perfcenter.baseclass.Lan;
import perfcenter.baseclass.ModelParameters;
import perfcenter.baseclass.PhysicalMachine;
import perfcenter.baseclass.SoftServer;
import perfcenter.baseclass.VirtualMachine;

/**
 * This implements the deployment of server on to hosts and hosts on to Lan
 * 
 * @author akhila
 */
public class DeployStmt {
	String name1, name2;

	public DeployStmt(String n1, String n2) {
		name1 = n1;
		name2 = n2;
	}

	public void deploy() throws Exception {
		PhysicalMachine pm;
		ModelParameters.isModified = true;
		try {
			if (ModelParameters.inputDistSys.isServer(name1)) { // if first parameter is softServer then second should be machine
				if (ModelParameters.inputDistSys.isPM(name2)) {
					SoftServer srv = ModelParameters.inputDistSys.getServer(name1);
					pm = ModelParameters.inputDistSys.getPM(name2);
					srv.addMachine(name2);
					pm.addServer(srv);
					srv.deploySoftResOnHost(pm);
				} else if (ModelParameters.inputDistSys.isVM(name2)) {
					SoftServer srv = ModelParameters.inputDistSys.getServer(name1);
					VirtualMachine vmachine = ModelParameters.inputDistSys.getVM(name2);
					srv.addMachine(name2);
					vmachine.addServer(srv);
					srv.deploySoftResOnHost(vmachine);
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
				pm.addLan(name2);
				ln.addMachine(name1);
				return;
			} else if (ModelParameters.inputDistSys.isVM(name1)){ //First Parameter is vmachine name, then second needs to be machine name
				if (ModelParameters.inputDistSys.isPM(name2)) {
					pm = ModelParameters.inputDistSys.getPM(name2);
					if(!pm.virtualizationEnabled){
						throw new Error("virtualization is not supported on " + "\"" + name2 + "\"");
					}
					VirtualMachine vmachine = ModelParameters.inputDistSys.getVM(name1);
					vmachine.host = pm;
					pm.addVM(vmachine);
				}else if(ModelParameters.inputDistSys.isVM(name2)){
					VirtualMachine hostvm = ModelParameters.inputDistSys.getVM(name2);
					if(!hostvm.virtualizationEnabled){
						throw new Error("virtualization is not supported on virtual machine " + "\"" + name2 + "\"");
					}
					VirtualMachine guestvm = ModelParameters.inputDistSys.getVM(name1);
					guestvm.host = hostvm;
					hostvm.addVM(guestvm);
					//System.out.println("DeployStmt::" + "vm.name:" + guestvm.name + " hostvm.name:" + hostvm.name);
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
