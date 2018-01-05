package jkind.solvers;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import jkind.lustre.Function;
import jkind.lustre.Type;
import jkind.lustre.values.Value;
import jkind.util.FunctionTable;
import jkind.util.SexpUtil;
import jkind.util.StreamIndex;

public abstract class Model {
	protected Map<String, Type> varTypes;
	protected Map<String, FunctionTable> functionTables = new HashMap<>();

	public Model(Map<String, Type> varTypes, List<Function> functions) {
		this.varTypes = Collections.unmodifiableMap(new HashMap<>(varTypes));
		for (Function fn : functions) {
			String encoded = SexpUtil.encodeFunction(fn.id);
			functionTables.put(encoded, new FunctionTable(fn.id, fn.inputs, fn.outputs.get(0)));
		}
	}

	public abstract Value getValue(String name);

	public abstract Set<String> getVariableNames();

	public Value getValue(StreamIndex si) {
		return getValue(si.getEncoded().str);
	}

	public void addFunctionTables(Map<String, FunctionTable> functionTables) {
		this.functionTables.putAll(functionTables);
	}

	public Map<String, FunctionTable> getFunctionTables() {
		return functionTables;
	}

	public FunctionTable getFunctionTable(String name) {
		return functionTables.get(name);
	}
}
