package jkind.lustre.visitors;

import jkind.lustre.Expr;
import jkind.lustre.Node;
import jkind.lustre.Program;
import jkind.lustre.Type;

public class TypeAwareAstMapVisitor extends AstMapVisitor {
	protected TypeReconstructor typeReconstructor;

	public TypeAwareAstMapVisitor(Program program) {
		typeReconstructor = new TypeReconstructor(program);
	}

	protected Type getType(Expr e) {
		return e.accept(typeReconstructor);
	}

	@Override
	public Program visit(Program e) {
		return super.visit(e);
	}

	@Override
	public Node visit(Node e) {
		typeReconstructor.setNodeContext(e);
		return super.visit(e);
	}
}
