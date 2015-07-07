package perfcenter.parser;

import java.util.ArrayList;

import org.apache.log4j.Logger;

/**
 * Implements a print statement. Calls specified method
 * 
 * @author akhila
 */
public class PrintStmt {

	ArrayList<PrintArgument> args = new ArrayList<PrintArgument>();
	Logger logger = Logger.getLogger("PrintStmt");
	public int lineno;

	public PrintStmt(int lno) {
		lineno = lno;
	}

	public void addArgument(PrintArgument a) {
		args.add(a);
	}

	public void execute() throws Exception {
		for (PrintArgument arg : args) {
			arg.print();
		}
		System.out.println();
	}
}
