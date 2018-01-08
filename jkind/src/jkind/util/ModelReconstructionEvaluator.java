package jkind.util;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jkind.JKindException;
import jkind.lustre.BinaryExpr;
import jkind.lustre.BinaryOp;
import jkind.lustre.Equation;
import jkind.lustre.Expr;
import jkind.lustre.FunctionCallExpr;
import jkind.lustre.IdExpr;
import jkind.lustre.Type;
import jkind.lustre.UnaryExpr;
import jkind.lustre.UnaryOp;
import jkind.lustre.values.BooleanValue;
import jkind.lustre.values.Value;
import jkind.lustre.visitors.Evaluator;
import jkind.slicing.Dependency;
import jkind.slicing.DependencySet;
import jkind.slicing.DependencyType;
import jkind.solvers.Model;
import jkind.solvers.SimpleModel;
import jkind.translation.Specification;

public class ModelReconstructionEvaluator extends Evaluator {
	public static Model reconstruct(Specification spec, Model model, String property, int k, boolean concrete) {
		ModelReconstructionEvaluator eval = new ModelReconstructionEvaluator(spec, model, concrete);
		eval.reconstructValues(property, k);
		return eval.model;
	}

	private final Specification spec;
	private final Model originalModel;
	private final SimpleModel model;
	private final boolean concrete;

	private final Map<String, Expr> equations = new HashMap<>();

	private int step;

	private ModelReconstructionEvaluator(Specification spec, Model originalModel, boolean concrete) {
		this.spec = spec;
		this.originalModel = originalModel;
		this.model = new SimpleModel(spec.functions);
		this.concrete = concrete;

		for (Equation eq : spec.node.equations) {
			equations.put(eq.lhs.get(0).id, eq.expr);
		}
	}

	private void reconstructValues(String property, int k) {
		DependencySet dependencies = spec.dependencyMap.get(property);
		for (step = 0; step < k; step++) {
			for (Dependency dependency : dependencies) {
				if (dependency.type == DependencyType.VARIABLE) {
					eval(new IdExpr(dependency.name));
				}
			}
			for (Expr assertion : spec.node.assertions) {
				BooleanValue bv = (BooleanValue) eval(assertion);
				if (!bv.value) {
					throw new JKindException("Unable to reconstruct counterexample: assertion became false");
				}
			}
		}

		// Check property is still falsified
		step = k - 1;
		BooleanValue bv = (BooleanValue) eval(new IdExpr(property));
		if (bv.value) {
			throw new JKindException("Unable to reconstruct counterexample: property became true");
		}
	}

	@Override
	public Value visit(IdExpr e) {
		StreamIndex si = new StreamIndex(e.id, step);

		Value value = model.getValue(si);
		if (value != null) {
			return value;
		}

		Expr expr = equations.get(e.id);
		if (expr == null) {
			// Input variable
			value = originalModel.getValue(si);
			if (value == null) {
				value = getDefaultValue(si);
			}
		} else {
			// Equation variable
			if (step < 0) {
				value = originalModel.getValue(si);
				if (value == null) {
					value = getDefaultValue(si);
				}
			} else {
				value = eval(expr);
				if (value == null) {
					throw new JKindException("Unable to reconstruct counterexample: evaluation failed");
				}
			}
		}

		model.putValue(si, value);
		return value;
	}

	private Value getDefaultValue(StreamIndex si) {
		return Util.getDefaultValue(spec.typeMap.get(si.getStream()));
	}

	@Override
	public Value visit(BinaryExpr e) {
		if (e.op == BinaryOp.ARROW) {
			if (!concrete) {
				// Inductive counterexamples never reach the true initial step
				return e.right.accept(this);
			}

			if (step == 0) {
				return e.left.accept(this);
			} else {
				return e.right.accept(this);
			}
		} else {
			return super.visit(e);
		}
	}

	@Override
	public Value visit(UnaryExpr e) {
		if (e.op == UnaryOp.PRE) {
			step--;
			Value value = e.expr.accept(this);
			step++;
			return value;
		} else {
			return super.visit(e);
		}
	}

	@Override
	public Value visit(FunctionCallExpr e) {
		String name = SexpUtil.encodeFunction(e.function);
		List<Value> inputs = visitExprs(e.args);
		
		Value output = model.evaluateFunction(name, inputs);
		if (output == null) {
			output = originalModel.evaluateFunction(name, inputs);
			if (output == null) {
				Type outputType = model.getFunctionTable(name).getOutput().type;
				output = Util.getDefaultValue(outputType);
			}

			model.getFunctionTable(name).addRow(inputs, output);
		}

		return output;
	}
}
