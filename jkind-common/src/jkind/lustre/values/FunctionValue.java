package jkind.lustre.values;

import jkind.lustre.BinaryOp;
import jkind.lustre.UnaryOp;
import jkind.sexp.Sexp;

public class FunctionValue extends Value {
	public final String name;
	public final Sexp body;

	public FunctionValue(String name, Sexp body) {
		this.body = body;
		this.name = name;
	}

	@Override
	public Value applyBinaryOp(BinaryOp op, Value right) {
		return null;
	}

	@Override
	public Value applyUnaryOp(UnaryOp op) {
		return null;
	}
}
