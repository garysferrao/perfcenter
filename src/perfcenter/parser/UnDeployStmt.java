package perfcenter.parser;

import perfcenter.baseclass.Host;
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
		Host host;
		ModelParameters.isModified = true;
		try {
			// undeploy smtp hp1
			if (ModelParameters.inputDistributedSystem.isHost(name2)) {
				if (ModelParameters.inputDistributedSystem.isServer(name1) == false) {
					throw new Error(" \"" + name1 + "\" is not server");
				}
				srv = ModelParameters.inputDistributedSystem.getServer(name1);
				host = ModelParameters.inputDistributedSystem.getHost(name2);
				srv.removeHost(name2);
				host.removeServer(name1);
				srv.unDeployVirtualResOnHost(host);
				return;
			}
			// undeploy hp1 lan1
			else if (ModelParameters.inputDistributedSystem.isLan(name2)) {
				if (ModelParameters.inputDistributedSystem.isHost(name1) == false) {
					throw new Error(" \"" + name1 + "\" is not host");
				}
				host = ModelParameters.inputDistributedSystem.getHost(name1);
				Lan ln = ModelParameters.inputDistributedSystem.getLan(name2);
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
