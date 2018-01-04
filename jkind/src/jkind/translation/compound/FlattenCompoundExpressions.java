package jkind.translation.compound;

import java.util.ArrayDeque;
import java.util.Deque;

import jkind.lustre.ArrayAccessExpr;
import jkind.lustre.ArrayExpr;
import jkind.lustre.Expr;
import jkind.lustre.FunctionCallExpr;
import jkind.lustre.IdExpr;
import jkind.lustre.IfThenElseExpr;
import jkind.lustre.IntExpr;
import jkind.lustre.Node;
import jkind.lustre.Program;
import jkind.lustre.RecordAccessExpr;
import jkind.lustre.RecordExpr;
import jkind.lustre.builders.ProgramBuilder;
import jkind.lustre.visitors.AstMapVisitor;
import jkind.translation.FlattenFunctionsWithComplexTypes;

/**
 * Flatten array and record expressions to scalars variables
 * 
 * Assumption: All node calls have been inlined.
 * 
 * Assumption: All array indices are integer literals
 * 
 * Assumption: All array updates are removed
 */
public class FlattenCompoundExpressions extends AstMapVisitor {
	public static Program program(Program program) {
		Node node = new FlattenCompoundExpressions().visit(program.getMainNode());
		return new ProgramBuilder(program).clearNodes().addNode(node).build();
	}

	private final Deque<Access> accesses = new ArrayDeque<>();

	@Override
	public Expr visit(ArrayAccessExpr e) {
		IntExpr intExpr = (IntExpr) e.index;
		accesses.push(new ArrayAccess(intExpr.value));
		Expr result = e.array.accept(this);
		accesses.pop();
		return result;
	}

	@Override
	public Expr visit(ArrayExpr e) {
		if (accesses.isEmpty()) {
			return super.visit(e);
		} else {
			ArrayAccess arrayAccess = (ArrayAccess) accesses.pop();
			Expr result = e.elements.get(arrayAccess.index.intValue()).accept(this);
			accesses.push(arrayAccess);
			return result;
		}
	}

	@Override
	public Expr visit(RecordAccessExpr e) {
		accesses.push(new RecordAccess(e.field));
		Expr result = e.record.accept(this);
		accesses.pop();
		return result;
	}

	@Override
	public Expr visit(RecordExpr e) {
		if (accesses.isEmpty()) {
			return super.visit(e);
		} else {
			RecordAccess recordAccess = (RecordAccess) accesses.pop();
			Expr result = e.fields.get(recordAccess.field).accept(this);
			accesses.push(recordAccess);
			return result;
		}
	}

	@Override
	public Expr visit(IdExpr e) {
		if (!accesses.isEmpty()) {
			throw new IllegalArgumentException();
		}
		return e;
	}

	@Override
	public Expr visit(FunctionCallExpr e) {
		int todo_replace_this_garbage;
		if (accesses.isEmpty()) {
			return super.visit(e);
		} else {
			Deque<Access> tempAccesses = new ArrayDeque<>();
			StringBuilder access = new StringBuilder();
			while (!accesses.isEmpty()) {
				Access tmp = accesses.peek();
				tempAccesses.push(tmp);
				if (tmp instanceof RecordAccess) {
					access.append(((RecordAccess) accesses.pop()).toString());
				} else if (tmp instanceof ArrayAccess) {
					access.append(((ArrayAccess) accesses.pop()).toString());
				}
			}
			String funcName = e.function + access.toString()
					.replace(".", FlattenFunctionsWithComplexTypes.functionRecordDelimeter)
					.replaceAll("\\[([^]]+)\\]", FlattenFunctionsWithComplexTypes.functionArrayDelimeter + "$1");
			Expr result = new FunctionCallExpr(funcName, visitExprs(e.args));
			while (!tempAccesses.isEmpty()) {
				accesses.push(tempAccesses.pop());
			}
			return result;
		}
	}

	@Override
	public Expr visit(IfThenElseExpr e) {
		Expr cond = e.cond.accept(new FlattenCompoundExpressions());
		Expr thenExpr = e.thenExpr.accept(this);
		Expr elseExpr = e.elseExpr.accept(this);
		return new IfThenElseExpr(cond, thenExpr, elseExpr);
	}
}
