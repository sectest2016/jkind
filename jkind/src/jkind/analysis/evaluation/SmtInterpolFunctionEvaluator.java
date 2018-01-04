package jkind.analysis.evaluation;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import de.uni_freiburg.informatik.ultimate.logic.Model;
import de.uni_freiburg.informatik.ultimate.logic.Script;
import de.uni_freiburg.informatik.ultimate.logic.Term;
import jkind.lustre.Equation;
import jkind.lustre.Function;
import jkind.lustre.FunctionCallExpr;
import jkind.lustre.IdExpr;
import jkind.lustre.Node;
import jkind.lustre.Type;
import jkind.lustre.values.BooleanValue;
import jkind.lustre.values.IntegerValue;
import jkind.lustre.values.RealValue;
import jkind.lustre.values.Value;
import jkind.solvers.smtinterpol.SmtInterpolUtil;
import jkind.util.SexpUtil;
import jkind.util.StreamIndex;

public class SmtInterpolFunctionEvaluator extends FunctionEvaluator {
	private final Model model;
	private final Script script;
	private final Map<String, Type> funcToOutputTypeMap = new HashMap<>();

	public SmtInterpolFunctionEvaluator(Script script, Model model, Node node, List<Function> functions, int length) {
		super(node, functions, length);
		this.model = model;
		this.script = script;
		for (Function func : functions) {
			funcToOutputTypeMap.put(func.id, func.outputs.get(0).type);
		}
	}

	@Override
	public Value visit(IdExpr e) {
		Equation eq = idToEqMap.get(e);
		if (eq != null) {
			return eq.expr.accept(this);
		}

		String name = "$" + e.id + StreamIndex.getSuffix(currentDepth);
		Term evaluated = model.evaluate(script.term(name));
		Value value = SmtInterpolUtil.getValue(evaluated, idToTypeMap.get(e.id));
		return value;
	}

	@Override
	public Value visit(FunctionCallExpr e) {

		Term[] args = new Term[e.args.size()];
		List<Value> inputs = new ArrayList<>();
		for (int i = 0; i < e.args.size(); i++) {
			Value val = e.args.get(i).accept(this);
			inputs.add(val);
			args[i] = model.evaluate(valueToTerm(val));
		}

		Term evaluated = model.evaluate(script.term(SexpUtil.encodeFunction(e.function).toString(), args));
		Value val = SmtInterpolUtil.getValue(evaluated, funcToOutputTypeMap.get(e.function));
		addFuncRow(e.function, inputs, val);

		return val;
	}

	private Term valueToTerm(Value value) {
		if (value instanceof BooleanValue) {
			return script.term(value.toString());
		} else if (value instanceof RealValue) {
			RealValue realVal = (RealValue) value;
			return script.decimal(realVal.value.toBigDecimal(0));
		} else if (value instanceof IntegerValue) {
			IntegerValue intValue = (IntegerValue) value;
			return script.numeral(intValue.value);
		}
		throw new IllegalArgumentException("Did not expect value of type " + value.getClass());

	}

}
