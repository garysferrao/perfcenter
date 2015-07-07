package perfcenter.parser;

import java.util.ArrayList;

/**
 * Implements for statement
 * 
 * @author akhila
 */
public class ForStmt {
	String loopvar;
	double startvar = 0, endvar = 100, incr = 0;
	ArrayList<Statement> statements = new ArrayList<Statement>();
	SetStmt set;

	public ForStmt() {
		set = new SetStmt();
	}

	public void addVariable(String var) {
		loopvar = var;
		set.addVariable(var);
	}

	public void addStartVar(double val) {
		startvar = val;
	}

	public void addEndVar(double val) {
		endvar = val;
	}

	public void addIncrVal(double val) {
		incr = val;
	}

	public void addStatement(Statement stmt) {
		statements.add(stmt);
	}

	public void execute() throws Exception {
		while (startvar <= endvar) {
			set.addVariableValue(startvar);
			set.execute();
			for (Statement pr : statements) {
				pr.execute();
			}
			startvar += incr;
		}
	}
}
