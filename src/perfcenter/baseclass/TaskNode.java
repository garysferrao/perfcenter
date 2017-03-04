/*
 * Copyright (C) 2011-12  by Varsha Apte - <varsha@cse.iitb.ac.in>, et al.
 * This file is distributed as part of PerfCenter
 *
 *  This program is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License as published by
 *   the Free Software Foundation, either version 3 of the License, or
 *   (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License
 *   along with this program.  If not, see <http://www.gnu.org/licenses/>. 
 */
package perfcenter.baseclass;

import java.util.ArrayList;

import org.apache.log4j.Logger;

/**
 * This is used to make tree for a scenario
 * 
 * @author akhila
 */
public class TaskNode {
	public String name;

	/** probability of the node */
	public Variable prob;

	/** packet size received from the parent(if thru network link)  */
	public Variable pktsize;

	/** set to true if an arc is defined as SYNC in input file  */
	public boolean issync;

	/** arrival rate to the node  */  //QUES: Why arrate here?
	public double arrate;

	/** pointer to parent node */
	public TaskNode parent;

	/** server name the node/task belongs to */
	public String servername = "none";

	/** used when finding compound task */ //QUES: What is the difference between normal task and compound task
	public boolean isCT = false;

	/** set to true of node is of type root or branch(start of branch) */
	public boolean isRoot = false;

	/** has the name of compound task this node is part of */
	public String belongsToCT;
	
	/*Index of Tasknode in scenario. Index is given level-wise */
	public int index;
	
	/** a node can have list of children nodes */
	public ArrayList<TaskNode> children  = new ArrayList<TaskNode>();
	
	private Logger logger = Logger.getLogger("TaskNode");

	public TaskNode(String name1) {
		name = name1;
	}

	/**
	 * 
	 * @param src name of the task
	 * @param server name of the server on which task will happen
	 * @param size packetsize
	 * @param sync is this a synchronous task
	 */
	public TaskNode(String src, String server, Variable size, boolean sync) {
		name = src;
		pktsize = size;
		issync = sync;
		if (server != null) {
			servername = server;
		}
	}

	public boolean isSync() {
		return issync;
	}
	
	public String getServerName(){
		return ModelParameters.inputDistSys.tasks.get(name).softServerName;
	}

	public String toString() {
		StringBuilder builder = new StringBuilder(name); 
		builder.append(" prob ").append( prob.value)
				.append(" sync:").append(issync)
				.append(" packet:").append(pktsize.value)
				.append(" arate:").append(arrate)
				.append(" servername:").append(servername);
		for(TaskNode child : children){
			builder.append("\t").append(name).append(" ").append(child.name).append(" prob ")
					.append(child.prob.value)
					.append((child.issync ? " sync " : " async"));
		}
		return builder.toString();
	}
	
	// prints the node details
	public void printFullInfo() {
		//logger.debug(" thisnode:" + name + " prob " + prob.value + " sync:" + issync + " packet:" + pktsize.value + " arate:" + arrate + " servername:"
			//	+ servername);
		for(TaskNode child : children){
			logger.debug("\t" + name + " " + child.name);
			logger.debug(" prob " + child.prob.value);
			logger.debug(" sync:" + child.issync);
			logger.debug(" arrate:" + child.arrate);
			logger.debug(" belongsToCT:" + String.valueOf(child.belongsToCT));
			logger.debug(" isCT:" + String.valueOf(child.isCT));
			logger.debug(" isRoot:" + String.valueOf(child.isRoot));
			logger.debug(child.toString());
		}
	}

	public TaskNode getCopy()
	{
		TaskNode n = new TaskNode(name, servername, pktsize, issync);
		n.arrate = this.arrate;
		n.belongsToCT = this.belongsToCT;
		n.isCT = this.isCT;
		n.isRoot = this.isRoot;
		n.parent = this.parent;
		n.prob = this.prob;
		for (TaskNode child : this.children) {
			n.children.add(child.getCopy());
		}
		return n;
	}
	
	public TaskNode getCopyWithoutChildren() // added by niranjan
	{
		TaskNode n = new TaskNode(name, servername, pktsize, issync);
		n.arrate = this.arrate;
		n.belongsToCT = this.belongsToCT;
		n.isCT = this.isCT;
		n.isRoot = this.isRoot;
		n.parent = this.parent;
		n.prob = this.prob;
		return n;
	}
}