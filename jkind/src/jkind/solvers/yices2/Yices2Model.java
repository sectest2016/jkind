package jkind.solvers.yices2;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jkind.lustre.Function;
import jkind.lustre.Type;
import jkind.lustre.values.Value;
import jkind.solvers.yices.YicesModel;

public class Yices2Model extends YicesModel {
	private final Map<String, Value> functionDefaultValue = new HashMap<>();

	public Yices2Model(Map<String, Type> varTypes, List<Function> functions) {
		super(varTypes, functions);
	}
	
	public void setFunctionDefaultValue(String name, Value value) {
		functionDefaultValue.put(name, value);
	}

	@Override
	public Value evaluateFunction(String name, List<Value> inputs) {
		name = getAlias(name);
		Value value = functionTables.get(name).lookup(inputs);
		if (value == null) {
			value = functionDefaultValue.get(name);
		}
		return value;
	}
}
