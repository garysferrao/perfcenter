package perfcenter.parser;

public class EqualityExpression {
	EqualityExpression exp2;
	RelationalExpression exp1;
	String operation;

	void addRelationalExpression(RelationalExpression e) throws Exception {
		exp1 = e;
	}

	void addEqualityExpression(EqualityExpression t) {
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
				if (operation.compareTo("==") == 0) {
					if (ret.compareTo(tmp) == 0) {
						ret = "true";
					} else {
						ret = "false";
					}

				} else if (operation.compareTo("!=") == 0) {
					if (ret.compareTo(tmp) == 0) {
						ret = "false";
					} else {
						ret = "true";
					}
				}
			}
		}
		return ret;
	}
}
