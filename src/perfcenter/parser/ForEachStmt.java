package perfcenter.parser;

import java.util.ArrayList;

/**
 * Implements the foreach contruct. Executes each statment in a for loop
 * 
 * @author akhila
 */
public class ForEachStmt {
	ArrayList<ForEachVariable> vars = new ArrayList<ForEachVariable>();
	ArrayList<Statement> statements = new ArrayList<Statement>();
	SetStmt set;

	public ForEachStmt() {
		set = new SetStmt();
	}

	public void addVariable(String var) {
		ForEachVariable lvar = new ForEachVariable(var);
		vars.add(lvar);
	}

	public void addVarValue(int index, double val) {
		ForEachVariable var = vars.get(index);
		var.addVarValue(val);
	}

	public void addStatement(Statement stmt) {
		statements.add(stmt);
	}

	int getVarListSize() {
		int len = 0;
		for (ForEachVariable var : vars) {
			if (var.varVals.size() > len) {
				len = var.varVals.size();
			}
		}
		return len - 1;
	}

	// very complicated logic. need to find out way tosimplify this
	public void execute() throws Exception {
		ArrayList<SetStmt> sets = new ArrayList<SetStmt>();
		for (ForEachVariable var : vars) {
			SetStmt set = new SetStmt();
			set.addVariable(var.forEachVar);
			/*** All values,variable will be in varVals[] ***/
			sets.add(set);
		}
		int maxvarlen = getVarListSize();
		for (int i = 0; i <= maxvarlen; i++) {
			/*** these two loops are like for j, i ***/
			for (int j = 0; j < sets.size(); j++) {
				double val = 0;
				SetStmt set = sets.get(j);
				int temp = vars.get(j).varVals.size();
				if (i < temp)
					val = vars.get(j).varVals.get(i);
				else
					val = vars.get(j).varVals.get(vars.get(j).varVals.size() - 1);
				set.addVariableValue(val);
				/*** Assigning a value to variable here ***/
				set.execute();
			}
			for (Statement pr : statements) {
				pr.execute();
			}
		}
	}

	class ForEachVariable {
		String forEachVar;
		ArrayList<Double> varVals = new ArrayList<Double>();

		public ForEachVariable(String name) {
			forEachVar = name;
		}

		public void addVarValue(double val) {
			varVals.add(val);
		}
	}
}
