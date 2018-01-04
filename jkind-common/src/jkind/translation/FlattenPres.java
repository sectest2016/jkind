package jkind.translation;

import java.util.ArrayList;
import java.util.List;

import jkind.lustre.Equation;
import jkind.lustre.Expr;
import jkind.lustre.IdExpr;
import jkind.lustre.Node;
import jkind.lustre.Program;
import jkind.lustre.UnaryExpr;
import jkind.lustre.UnaryOp;
import jkind.lustre.VarDecl;
import jkind.lustre.builders.NodeBuilder;
import jkind.lustre.builders.ProgramBuilder;
import jkind.lustre.visitors.TypeAwareAstMapVisitor;

/**
 * Remove all occurrences of temporal operators ('pre' and '->') underneath
 * 'pre' operators. This is done by introducing new local variables.
 */
public class FlattenPres extends TypeAwareAstMapVisitor {
	public FlattenPres(Program program) {
		super(program);
	}

	public static Program program(Program program) {
		Node node = new FlattenPres(program).visit(program.getMainNode());
		return new ProgramBuilder(program).clearNodes().addNode(node).build();
	}

	private List<VarDecl> newLocals = new ArrayList<>();
	private List<Equation> newEquations = new ArrayList<>();
	private int counter = 0;

	@Override
	public Node visit(Node e) {
		NodeBuilder builder = new NodeBuilder(super.visit(e));
		builder.addLocals(newLocals);
		builder.addEquations(newEquations);
		return builder.build();
	}

	private IdExpr getFreshId() {
		return new IdExpr("~flatten" + counter++);
	}

	@Override
	public Expr visit(UnaryExpr e) {
		if (e.op == UnaryOp.PRE && ContainsTemporalOperator.check(e.expr)) {
			Expr nested = e.expr.accept(this);
			IdExpr id = getFreshId();
			newLocals.add(new VarDecl(id.id, getType(e.expr)));
			newEquations.add(new Equation(id, nested));
			return new UnaryExpr(e.op, id);
		} else {
			return super.visit(e);
		}
	}
}
