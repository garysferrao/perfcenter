package perfcenter.parser;

public class AdditiveExpression {
	MultiplicativeExpression exp1;
	AdditiveExpression exp2;
	String operation;

	void addAdditiveExpression(AdditiveExpression e) {
		exp2 = e;
	}

	void addMultiplicativeExpression(MultiplicativeExpression t) {
		exp1 = t;
	}

	void addOperation(String op) {
		operation = op;
	}

	String getValue() throws Exception {
		Double val = 0.0;
		String ret = "0";
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
				if (operation.compareTo("+") == 0) {
					val = Double.valueOf(ret.trim()).doubleValue() + Double.valueOf(tmp.trim()).doubleValue();
					ret = val.toString();
				} else {
					val = Double.valueOf(ret.trim()).doubleValue() - Double.valueOf(tmp.trim()).doubleValue();
					ret = val.toString();
				}
			}
		}
		return ret;
	}
}
