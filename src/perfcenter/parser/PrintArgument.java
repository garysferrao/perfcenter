package perfcenter.parser;

import org.apache.log4j.Logger;

public class PrintArgument {
	String message_;
	Expression aexp_;

	public int lineno;
	Logger logger = Logger.getLogger("PrintArgument");

	public void addMessage(String message) {
		message_ = message.replace("\"", "");
	}

	public void addExpression(Expression a) {
		aexp_ = a;
	}

	public void print() throws Exception {
		if (message_ != null) {
			System.out.print(message_);
		} else if (aexp_ != null) {
			System.out.print(aexp_.getValue());
		}
	}
}
