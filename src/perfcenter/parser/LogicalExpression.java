package perfcenter.parser;

public class LogicalExpression {
	LogicalExpression exp2;
	LogicalANDExpression exp1;
	String operation;

	void addLogicalANDExpression(LogicalANDExpression e) throws Exception {
		exp1 = e;

	}

	void addLogicalExpression(LogicalExpression t) {
		exp2 = t;
	}

	void addOperation(String op) {
		operation = op;
	}

	String getValue() throws Exception {
		String ret = "false";
		if (exp1 != null) {
			ret = exp1.getValue();
			if (exp2 != null) {
				String tmp = exp2.getValue();
				if (operation.compareTo("||") == 0) {
					if ((ret.compareTo("true") == 0) || (tmp.compareTo("true") == 0)) {
						ret = "true";
					} else {
						ret = "false";
					}
				}
			}
		}
		return ret;
	}
}
