package jkind.util;

import java.util.List;

import jkind.lustre.values.Value;
import jkind.util.Util;

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
		for(Value val : inputs){
			sb.append(String.format("%-10s", val.toString() + " "));
		}
		sb.append("| ");	
		sb.append(String.format("%-10s", output.toString()));
		return sb.toString();
	}

}
