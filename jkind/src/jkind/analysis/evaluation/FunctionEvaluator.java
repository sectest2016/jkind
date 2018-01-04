package jkind.analysis.evaluation;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import jkind.lustre.Equation;
import jkind.lustre.Expr;
import jkind.lustre.Function;
import jkind.lustre.IdExpr;
import jkind.lustre.Node;
import jkind.lustre.Type;
import jkind.lustre.UnaryExpr;
import jkind.lustre.UnaryOp;
import jkind.lustre.VarDecl;
import jkind.lustre.values.Value;
import jkind.lustre.visitors.Evaluator;
import jkind.translation.FlattenFunctionsWithComplexTypes;
import jkind.util.FunctionTable;
import jkind.util.FunctionTableRow;

public abstract class FunctionEvaluator extends Evaluator {
	protected final Node node;
	protected final int length;
	private final Map<String, FunctionTable> funcTables = new HashMap<>();
	private final Map<String, Function> lustreFuncDefs = new HashMap<>();
	protected int currentDepth;
	protected final Map<IdExpr, Equation> idToEqMap = new HashMap<>();
	protected final Map<String, Type> idToTypeMap = new HashMap<>();

	public FunctionEvaluator(Node node, List<Function> funcs, int length) {
		this.node = node;
		this.length = length;

		for (Equation eq : node.equations) {
			if (eq.lhs.size() != 1) {
				throw new IllegalArgumentException("We assume single variable equations");
			}
			idToEqMap.put(eq.lhs.get(0), eq);
		}

		for (VarDecl var : node.inputs) {
			idToTypeMap.put(var.id, var.type);
		}
		for (VarDecl var : node.outputs) {
			idToTypeMap.put(var.id, var.type);
		}
		for (VarDecl var : node.locals) {
			idToTypeMap.put(var.id, var.type);
		}

		for (Function func : funcs) {
			lustreFuncDefs.put(func.id, func);
		}

	}

	public List<FunctionTable> evaluateFuncs() {
		currentDepth = length;
		// TODO: we could memoize function calls at earlier depths to speed
		// things up

		while (currentDepth >= 0) {
			for (Equation eq : node.equations) {
				eq.expr.accept(this);
			}
			for (Expr expr : node.assertions) {
				expr.accept(this);
			}
			currentDepth--;
		}

		List<FunctionTable> tables = new ArrayList<>();
		for (Entry<String, FunctionTable> entry : funcTables.entrySet()) {
			tables.add(entry.getValue());
		}

		return tables;
	}

	@Override
	public Value visit(UnaryExpr e) {
		if (e.op.equals(UnaryOp.PRE)) {
			currentDepth--;
			Value val = e.expr.accept(this);
			currentDepth++;
			return val;
		}
		return super.visit(e);
	}

	protected void addFuncRow(String funcName, List<Value> inputVals, Value outputVal) {
		FunctionTable table = funcTables.get(funcName);

		if (table == null) {
			Function func = lustreFuncDefs.get(funcName);
			if (func.outputs.size() != 1) {
				throw new IllegalArgumentException();
			}
			table = new FunctionTable(
					funcName.replace(FlattenFunctionsWithComplexTypes.functionRecordDelimeter, ".")
						.replaceAll(FlattenFunctionsWithComplexTypes.functionArrayDelimeter + "([0-9]+)", "[$1]")
						.replaceAll(FlattenFunctionsWithComplexTypes.functionTupleDelimeter + "(.*)", "($1)"),
					func.inputs,func.outputs.get(0));
			funcTables.put(funcName, table);
		}

		FunctionTableRow row = new FunctionTableRow(inputVals, outputVal);
		table.addRow(row);
	}
}
