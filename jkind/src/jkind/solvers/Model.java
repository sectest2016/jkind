package jkind.solvers;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import jkind.lustre.Function;
import jkind.lustre.Type;
import jkind.lustre.values.Value;
import jkind.slicing.Dependency;
import jkind.slicing.DependencySet;
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

	public void addFunctionTable(String encoded, FunctionTable table) {
		functionTables.put(encoded, table);
	}

	public Collection<FunctionTable> getFunctionTables() {
		return functionTables.values();
	}

	public FunctionTable getFunctionTable(String name) {
		return functionTables.get(name);
	}

	public Value evaluateFunction(String name, List<Value> inputs) {
		return functionTables.get(name).lookup(inputs);
	}

	public Model slice(DependencySet keep) {
		SimpleModel sliced = new SimpleModel();
		for (String var : getVariableNames()) {
			StreamIndex si = StreamIndex.decode(var);
			if (si != null && keep.contains(Dependency.variable(si.getStream()))) {
				sliced.putValue(si, getValue(var));
			}
		}
		
		for (Entry<String, FunctionTable> entry : functionTables.entrySet()) {
			String encoded = entry.getKey();
			FunctionTable table = entry.getValue();
			
			if (keep.contains(Dependency.function(table.getName()))) {
				sliced.addFunctionTable(encoded, table);
			}
		}

		return sliced;
	}
}
