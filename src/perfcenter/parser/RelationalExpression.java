package perfcenter.parser;

public class RelationalExpression {
	AdditiveExpression exp1;
	RelationalExpression exp2;
	String operation;

	void addAdditiveExpression(AdditiveExpression e) throws Exception {
		exp1 = e;
	}

	void addRelationalExpression(RelationalExpression t) {
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
				if (ret.contains("+-")) {
					String[] tmp1 = ret.split("\\+-");
					ret = tmp1[0];
				}
				if (tmp.contains("+-")) {
					String[] tmp1 = tmp.split("\\+-");
					tmp = tmp1[0];
				}
				if (operation.compareTo("<") == 0) {
					if (Double.valueOf(ret.trim()).doubleValue() < Double.valueOf(tmp.trim()).doubleValue()) {
						ret = "true";
					} else {
						ret = "false";
					}
				} else if (operation.compareTo("<=") == 0) {
					if (Double.valueOf(ret.trim()).doubleValue() <= Double.valueOf(tmp.trim()).doubleValue()) {
						ret = "true";
					} else {
						ret = "false";
					}
				} else if (operation.compareTo(">") == 0) {
					if (Double.valueOf(ret.trim()).doubleValue() > Double.valueOf(tmp.trim()).doubleValue()) {
						ret = "true";
					} else {
						ret = "false";
					}
				} else if (operation.compareTo(">=") == 0) {
					if (Double.valueOf(ret.trim()).doubleValue() >= Double.valueOf(tmp.trim()).doubleValue()) {
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
