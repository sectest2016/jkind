package jkind.util;

import static java.util.stream.Collectors.joining;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import jkind.lustre.VarDecl;
import jkind.lustre.values.Value;

public class FunctionTable {
	private final String name;
	private final Set<FunctionTableRow> rows = new HashSet<>();
	private final List<VarDecl> inputs;
	private final VarDecl output;

	public FunctionTable(String name, List<VarDecl> inputs, VarDecl output) {
		this.name = name;
		this.inputs = inputs;
		this.output = output;
	}

	public void addRow(FunctionTableRow row) {
		rows.add(row);
	}

	public List<VarDecl> getInputs() {
		return inputs;
	}

	public VarDecl getOutput() {
		return output;
	}

	public Set<FunctionTableRow> getRows() {
		return rows;
	}

	public String getName() {
		return name;
	}
	
	public Value lookup(List<Value> inputValues) {
		if (inputs.size() != inputValues.size()) {
			throw new IllegalArgumentException();
		}
		
		for (FunctionTableRow row : rows) {
			if (row.getInputs().equals(inputValues)) {
				return row.getOutput();
			}
		}
		return null;
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append(inputs.stream().map(vd -> vd.id).collect(joining(", ")));
		sb.append(", " + name);
		sb.append("\n");
		for (FunctionTableRow row : rows) {
			sb.append(row);
			sb.append("\n");
		}
		return sb.toString();
	}

}
