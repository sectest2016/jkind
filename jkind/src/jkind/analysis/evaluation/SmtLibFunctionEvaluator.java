package jkind.analysis.evaluation;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import jkind.lustre.Equation;
import jkind.lustre.Expr;
import jkind.lustre.Function;
import jkind.lustre.FunctionCallExpr;
import jkind.lustre.IdExpr;
import jkind.lustre.Node;
import jkind.lustre.values.FunctionValue;
import jkind.lustre.values.Value;
import jkind.lustre.visitors.ExprIterVisitor;
import jkind.sexp.Cons;
import jkind.sexp.Sexp;
import jkind.solvers.Model;
import jkind.solvers.ModelEvaluator;
import jkind.solvers.SimpleModel;
import jkind.solvers.smtlib2.SexpEvaluator;
import jkind.util.SexpUtil;

public class SmtLibFunctionEvaluator extends FunctionEvaluator {
	private final Model normalModel;
	private final Map<String, Sexp> funcBodies;

	public SmtLibFunctionEvaluator(Node node, List<Function> funcs, Model model, int length) {
		super(node, funcs, length);
		this.normalModel = model;
		this.funcBodies = cacheFuncBodies(node);
	}

	private Map<String, Sexp> cacheFuncBodies(Node node) {
		Map<String, Sexp> bodies = new HashMap<>();
		Set<String> names = new HashSet<>();

		ExprIterVisitor funcFinder = new ExprIterVisitor() {
			@Override
			public Void visit(FunctionCallExpr e) {
				names.add(e.function);
				visitExprs(e.args);
				return null;
			}
		};

		for (Equation eq : node.equations) {
			eq.expr.accept(funcFinder);
		}
		for (Expr expr : node.assertions) {
			expr.accept(funcFinder);
		}

		for (String name : names) {
			Value val = normalModel.getValue(SexpUtil.encodeFunction(name).toString());
			if (val != null) { // if null then the cex did not report a model
								// for the function
				if (val instanceof FunctionValue)
					bodies.put(name, ((FunctionValue) val).body);
				else
					throw new RuntimeException("Non-function type added to model for " + name);
			}
		}

		return bodies;
	}

	@Override
	public Value visit(IdExpr e) {
		return e.accept(new ModelEvaluator(normalModel, currentDepth));
	}

	@Override
	public Value visit(FunctionCallExpr e) {
		SimpleModel evalulationModel = new SimpleModel();
		List<Value> inputValues = new ArrayList<>();
		for (Expr expr : e.args) {
			inputValues.add(expr.accept(this));
		}
		Sexp funcSexp = funcBodies.get(e.function);
		if (funcSexp == null) {
			// the model did not return a body for this function
			return null;
		}

		Sexp funcSym;
		if (funcSexp instanceof Cons) {
			Cons body = (Cons) funcSexp;
			// the first part of the cons contains the args
			if (body.head instanceof Cons) {
				Cons argsExpr = (Cons) body.head;

				for (int i = 0; i < inputValues.size(); i++) {
					Cons argExpr;
					if (i == 0) {
						argExpr = (Cons) argsExpr.head;
					} else {
						argExpr = (Cons) argsExpr.args.get(i - 1);
					}
					evalulationModel.putValue(argExpr.head.toString(), inputValues.get(i));
				}
				funcSym = body.args.get(0);
			} else {
				funcSym = funcSexp;
			}
		} else {
			funcSym = funcSexp;
		}

		SexpEvaluator evaluator = new SexpEvaluator(evalulationModel);
		Value val = evaluator.eval(funcSym);
		addFuncRow(e.function, inputValues, val);

		return val;
	}

}
