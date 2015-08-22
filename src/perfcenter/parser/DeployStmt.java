package perfcenter.parser;

import perfcenter.baseclass.Host;
import perfcenter.baseclass.Lan;
import perfcenter.baseclass.ModelParameters;
import perfcenter.baseclass.SoftServer;

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
		SoftServer srv;
		Host host;
		ModelParameters.isModified = true;
		try {
			if (ModelParameters.inputDistSys.isServer(name1)) { // if first parameter is server then second should be host
				if (ModelParameters.inputDistSys.isHost(name2) == false) {
					throw new Error(" \"" + name2 + "\" is not host");
				}
				srv = ModelParameters.inputDistSys.getServer(name1);
				host = ModelParameters.inputDistSys.getHost(name2);
				srv.addHost(name2);
				host.addServer(srv);
				srv.deployVirtualResOnHost(host);
				return;
			} else if (ModelParameters.inputDistSys.isHost(name1)) { // if first parameter is host then second should be lan
				if (ModelParameters.inputDistSys.isLan(name2) == false) {
					throw new Error(" \"" + name2 + "\" is not Lan");
				}
				host = ModelParameters.inputDistSys.getHost(name1);
				Lan ln = ModelParameters.inputDistSys.getLan(name2);
				host.addLan(name2);
				ln.addHost(name1);
				return;
			}
			throw new Error(" \"" + name1 + "\" is not host or server");
		} catch (Error e) {
			throw e;
		}
	}
}
