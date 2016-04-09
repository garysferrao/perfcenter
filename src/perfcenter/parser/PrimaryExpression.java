package perfcenter.parser;

import perfcenter.baseclass.Variable;

public class PrimaryExpression {
	Variable var;
	String val = "0";
	FunctionDefinition func;
	Expression exp;

	void addVariable(Variable v) {
		var = v;
	}

	void addNumber(Double d) {
		val = d.toString();
	}

	void addFuncDef(FunctionDefinition f) {
		func = f;
	}

	void addExpression(Expression e) {
		exp = e;
	}

	String getValue() throws Exception {
		String ret = "0";
		Double tmp = 0.0;
		if (var != null) {
			tmp = var.getValue();
			ret = tmp.toString();
		} else if (func != null) {

			ret = func.execute();
		} else if (exp != null) {
			ret = exp.getValue();
		} else {
			ret = val;
		}
		return ret;
	}

}
