package perfcenter.parser;

import perfcenter.baseclass.ModelParameters;

/**
 * 
 * @author akhila
 * 
 */
public class Statement {
	SetStmt set;
	ForEachStmt festmt;
	ForStmt fstmt;
	PrintStmt pstmt;
	IfStmt ifstmt;
	WhileStmt wstmt;
	Expression exp;
	boolean breakstmt = false;

	void addSetStmt(SetStmt s) {
		set = s;
	}

	void addBreakStmt() {
		breakstmt = true;
	}

	void addForEachStmt(ForEachStmt f) {
		festmt = f;
	}

	void addForStmt(ForStmt f) {
		fstmt = f;
	}

	void addPrintStmt(PrintStmt p) {
		pstmt = p;
	}

	void addIfStmt(IfStmt i) {
		ifstmt = i;
	}

	void addWhileStmt(WhileStmt w) {
		wstmt = w;
	}

	void addExpression(Expression e) {
		exp = e;
	}

	void execute() throws Exception {
		if (set != null) {
			set.execute();
		} else if (fstmt != null) {
			fstmt.execute();
		} else if (festmt != null) {
			System.out.println("arate1: " + ModelParameters.getArrivalRate());
			festmt.execute();
			System.out.println("arate3: " + ModelParameters.getArrivalRate());
		} else if (pstmt != null) {
			pstmt.execute();
		} else if (ifstmt != null) {
			ifstmt.execute();
		} else if (wstmt != null) {
			wstmt.execute();
		} else if (exp != null) {
			exp.getValue();
		} else if (breakstmt == true)
			throw new Exception("breakpoint");
	}
}
