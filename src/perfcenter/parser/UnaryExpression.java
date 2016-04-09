package perfcenter.parser;

public class UnaryExpression {
	PrimaryExpression elm;
	boolean isNegate = false;

	void addPrimaryExpression(PrimaryExpression e) {
		elm = e;
	}

	void addNegate() {
		isNegate = true;
	}

	String getValue() throws Exception {
		if (isNegate == true) {

			String var = elm.getValue();
			if (var.contains("true") || var.contains("false"))
				return var;

			Double val = 0.0;
			if (var.contains("+-")) {
				String[] temp = var.split("\\+-");
				val = Double.valueOf(temp[0].trim()).doubleValue();
				val = val * -1;
				var = val.toString() + "+-" + temp[1];
			} else {
				val = Double.valueOf(var.trim()).doubleValue();
				val = val * -1;
				var = val.toString();
			}
			return var;
		} else
			return elm.getValue();
	}

}
