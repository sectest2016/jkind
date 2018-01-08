package jkind.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

import jkind.lustre.VarDecl;
import jkind.lustre.values.Value;

public class FunctionTable {
	private final String name;
	private final List<VarDecl> inputs;
	private final VarDecl output;
	private final SortedMap<FunctionTableRow, Value> rows = new TreeMap<>();

	public FunctionTable(String name, List<VarDecl> inputs, VarDecl output) {
		this.name = name;
		this.inputs = inputs;
		this.output = output;
	}

	public void addRow(List<Value> inputValues, Value outputValue) {
		rows.put(makeRow(inputValues), Util.promoteIfNeeded(outputValue, output.type));
	}

	private FunctionTableRow makeRow(List<Value> inputValues) {
		List<Value> typeCorrectInputs = new ArrayList<>();
		for (int i = 0; i < inputValues.size(); i++) {
			typeCorrectInputs.add(Util.promoteIfNeeded(inputValues.get(i), inputs.get(i).type));
		}
		return new FunctionTableRow(typeCorrectInputs);
	}

	public List<VarDecl> getInputs() {
		return inputs;
	}

	public VarDecl getOutput() {
		return output;
	}

	public String getName() {
		return name;
	}
	
	public Map<FunctionTableRow, Value> getBody() {
		return Collections.unmodifiableMap(rows);
	}

	public Value lookup(List<Value> inputValues) {
		if (inputs.size() != inputValues.size()) {
			throw new IllegalArgumentException();
		}
		return rows.get(makeRow(inputValues));
	}

	public FunctionTable copy() {
		FunctionTable copy = new FunctionTable(name, inputs, output);
		copy.rows.putAll(rows);
		return copy;
	}
}
