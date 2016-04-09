package perfcenter.parser;

import perfcenter.baseclass.Machine;
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

	public void undef() throws Exception {
		SoftServer srv;
		Machine host;
		ModelParameters.isModified = true;
		try {
			// undeploy smtp hp1
			if (ModelParameters.inputDistSys.isPM(name2)) {
				if (ModelParameters.inputDistSys.isServer(name1) == false) {
					throw new Error(" \"" + name1 + "\" is not server");
				}
				srv = ModelParameters.inputDistSys.getServer(name1);
				host = ModelParameters.inputDistSys.getPM(name2);
				srv.removeMachine(name2);
				host.removeServer(name1);
				srv.unDeploySoftResOnHost(host);
				return;
			}
			// undeploy hp1 lan1
			else if (ModelParameters.inputDistSys.isLan(name2)) {
				if (ModelParameters.inputDistSys.isPM(name1) == false) {
					throw new Error(" \"" + name1 + "\" is not host");
				}
				host = ModelParameters.inputDistSys.getPM(name1);
				Lan ln = ModelParameters.inputDistSys.getLan(name2);
				host.removeLan(name2);
				ln.removeHost(name1);
				return;
			}
			throw new Error(" \"" + name2 + "\" is not host or lan");
		} catch (Error e) {
			throw e;
		}
	}
}
