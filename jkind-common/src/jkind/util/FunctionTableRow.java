package jkind.util;

import java.util.List;

import jkind.lustre.values.Value;

public class FunctionTableRow {
	private final List<Value> inputs;

	public FunctionTableRow(List<Value> inputs) {
		this.inputs = Util.safeList(inputs);
	}

	public List<Value> getInputs() {
		return inputs;
	}

	@Override
	public int hashCode() {
		return inputs.hashCode();
	}

	@Override
	public boolean equals(Object o) {
		if (o instanceof FunctionTableRow) {
			FunctionTableRow row = (FunctionTableRow) o;
			return row.inputs.equals(inputs);
		}
		return false;
	}
}
