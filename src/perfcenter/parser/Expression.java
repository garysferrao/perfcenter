package perfcenter.parser;

import perfcenter.baseclass.ModelParameters;
import perfcenter.baseclass.Variable;

public class Expression {
	Variable var;
	Expression exp2;
	LogicalExpression exp1;

	public Expression() {
	}

	void addVariable(Variable v) {
		var = v;
		var.updateUsedInfo();
	}

	void addExpression(Expression e) {
		exp2 = e;
	}

	void addLogicalExpression(LogicalExpression e) {
		exp1 = e;
	}

	String getValue() throws Exception {
		String ret = "0";

		if (exp2 != null) {
			ret = exp2.getValue();
			if (ret.contains("+-")) {
				String[] tmp1 = ret.split("\\+-");
				ret = tmp1[0];
			}
			if (ret.contains("true"))
				var.setValue(1);
			else if (ret.contains("false"))
				var.setValue(0);
			else
				var.setValue(Double.valueOf(ret.trim()).doubleValue());
			ModelParameters.isModified = true;
		} else if (exp1 != null) {
			ret = exp1.getValue();
		}

		return ret;
	}
}
