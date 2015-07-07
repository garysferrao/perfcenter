package perfcenter.parser;

import java.util.ArrayList;

public class WhileStmt {
	Expression exp;
	ArrayList<Statement> statements = new ArrayList<Statement>();

	void addExpression(Expression e) {
		exp = e;
	}

	void addStatement(Statement s) {
		statements.add(s);
	}

	void execute() throws Exception {
		int count = 0;
		while (exp.getValue().contains("true")) {
			for (Statement stmt : statements) {
				stmt.execute();
			}
			count++;
			if (count > 1000)
				throw new Exception("While loop crossed 1000 iterations");
		}
	}
}
