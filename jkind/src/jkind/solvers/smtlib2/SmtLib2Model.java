package jkind.solvers.smtlib2;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import jkind.lustre.Function;
import jkind.lustre.NamedType;
import jkind.lustre.Type;
import jkind.lustre.values.IntegerValue;
import jkind.lustre.values.RealValue;
import jkind.lustre.values.Value;
import jkind.sexp.Cons;
import jkind.sexp.Sexp;
import jkind.solvers.Model;
import jkind.util.BigFraction;
import jkind.util.Util;

public class SmtLib2Model extends Model {
	private final Map<String, Sexp> values = new HashMap<>();

	public SmtLib2Model(Map<String, Type> varTypes, List<Function> functions) {
		super(varTypes, functions);
	}

	public void addValue(String id, Sexp sexp) {
		values.put(id, sexp);
	}

	@Override
	public Value getValue(String name) {
		Sexp sexp = values.get(name);
		Type type = varTypes.get(name);
		if (sexp == null) {
			return Util.getDefaultValue(type);
		}
		Value value = new SexpEvaluator(this).eval(sexp);
		return promoteIfNeeded(value, type);
	}

	private Value promoteIfNeeded(Value value, Type type) {
		if (value instanceof IntegerValue && type == NamedType.REAL) {
			IntegerValue iv = (IntegerValue) value;
			return new RealValue(new BigFraction(iv.value));
		}
		return value;
	}

	@Override
	public Set<String> getVariableNames() {
		return values.keySet();
	}

	@Override
	public Value evaluateFunction(String name, List<Value> inputs) {
		// Function value may be stored in the tables
		Value parentResult = super.evaluateFunction(name, inputs);
		if (parentResult != null) {
			return parentResult;
		}
		
		// Function value will otherwise be computed from lambda s-expression
		Sexp lambda = values.get(name);
		if (lambda == null) {
			return null;
		}

		Cons cons = (Cons) lambda;
		Map<String, Value> env = zipMap(getArgNames(cons.args.get(0)), inputs);
		Sexp body = cons.args.get(1);
		return new SexpEvaluator(env::get).eval(body);
	}

	private List<String> getArgNames(Sexp sexp) {
		List<String> args = new ArrayList<>();
		Cons cons = (Cons) sexp;

		args.add(car(cons.head).toString());
		for (Sexp next : cons.args) {
			args.add(car(next).toString());
		}
		return args;
	}

	private Sexp car(Sexp sexp) {
		return ((Cons) sexp).head;
	}

	private <K, V> Map<K, V> zipMap(List<K> keys, List<V> values) {
		Map<K, V> result = new HashMap<>();
		for (int i = 0; i < keys.size(); i++) {
			result.put(keys.get(i), values.get(i));
		}
		return result;
	}
}
