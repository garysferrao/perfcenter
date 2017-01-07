package perfcenter.parser;

import java.util.ArrayList;

public class IfStmt {
	Expression exp;
	ArrayList<Statement> statements = new ArrayList<Statement>();
	ArrayList<Statement> elsestatements = new ArrayList<Statement>();

	void addExpression(Expression e) {
		exp = e;
	}

	void addStatement(Statement s) {
		statements.add(s);
	}

	void addElseStatement(Statement s) {
		elsestatements.add(s);
	}

	void execute() throws Exception {
		if (exp.getValue().contains("true")) {
			for (Statement stmt : statements) {
				stmt.execute();
			}
		} else if (elsestatements.size() > 0) {
			for (Statement stmt : elsestatements) {
				stmt.execute();
			}
		}

	}
}
