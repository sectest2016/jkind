package jkind.util;

import static java.util.stream.Collectors.joining;

import java.util.List;

import jkind.lustre.values.Value;

public class FunctionTableRow {
	private final List<Value> inputs;
	private final Value output;

	public FunctionTableRow(List<Value> inputs, Value output) {
		this.inputs = Util.safeList(inputs);
		this.output = output;
	}

	public List<Value> getInputs() {
		return inputs;
	}

	public Value getOutput() {
		return output;
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
	
	@Override
	public String toString(){
		StringBuilder sb = new StringBuilder();
		sb.append(inputs.stream().map(val -> val.toString()).collect(joining(", ")));
		sb.append(", " + output);
		return sb.toString();
	}
}
