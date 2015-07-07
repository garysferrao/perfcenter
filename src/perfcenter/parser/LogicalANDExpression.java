package perfcenter.parser;

public class LogicalANDExpression {
	EqualityExpression exp1;
	LogicalANDExpression exp2;
	String operation;

	void addEqualityExpression(EqualityExpression e) throws Exception {
		exp1 = e;
	}

	void addLogicalANDExpression(LogicalANDExpression t) {
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
				if (operation.compareTo("&&") == 0) {
					if (ret.compareTo(tmp) == 0) {
						if (ret.compareTo("true") == 0)
							ret = "true";
						else
							ret = "false";
					} else {
						ret = "false";
					}
				}
			}
		}
		return ret;
	}
}
