package perfcenter.parser;

import perfcenter.baseclass.Node;
import perfcenter.baseclass.Variable;

/**
 * Tree is used by parser when building the scenario tree
 * 
 * @author akhila
 */
public class Tree {
	Node root;
	Variable prob;

	public Tree(Variable p) {
		prob = p;
	}

	public Node addArc(Node srcN, String src, String src_server, String dest, String dest_server, Variable pktsize, boolean isSync) {

		Node destNode = new Node(dest, dest_server, pktsize, isSync);
		destNode.prob = prob;
		if (srcN.name.compareToIgnoreCase("root") == 0) {
			srcN.name = src;
			srcN.prob = prob;
			srcN.issync = false;
			if (src_server != null) {
				srcN.servername = src_server;
			}
			srcN.children.add(destNode);
			destNode.parent = srcN;
			srcN.isRoot = true;
			srcN.pktsize = new Variable("local", 0);
			root = srcN;
		} else if (srcN.name.compareToIgnoreCase("branch") == 0) {
			srcN.name = src;
			if (src_server != null) {
				srcN.servername = src_server;
			}
			srcN.children.add(destNode);
			destNode.parent = srcN;
			destNode.isRoot = true;
			root = srcN;
		} else {
			if (srcN.name.compareToIgnoreCase(src) != 0) {
				throw new Error("Task(" + srcN.name + ") at end of previous arc and task(" + src + ") at start of current arc is not same");
			}
			srcN.children.add(destNode);
			destNode.parent = srcN;
		}
		return destNode;
	}

	public void addBranch(Node src, Node dest) {
		if (src.name.compareToIgnoreCase(dest.name) != 0) {
			throw new Error("Task(" + src.name + ") at end of previous arc and task(" + dest.name + ") at start of current arc is not same");
		}
		Node n = dest.children.get(0);
		src.children.add(n);
		n.parent = src;
	}

	public Node getRootNode() {
		return root;
	}
}
