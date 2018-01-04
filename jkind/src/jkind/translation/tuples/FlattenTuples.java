package jkind.translation.tuples;

import jkind.lustre.Node;
import jkind.lustre.Program;
import jkind.lustre.builders.ProgramBuilder;

/**
 * Flatten all tuple expressions via expansion.
 * 
 * Assumption: All node calls have been inlined.
 */
public class FlattenTuples {
	public static Program program(Program program) {
		Node node = program.getMainNode();
		node = LiftTuples.node(node);
		node = FlattenTupleComparisons.node(node);
		node = FlattenTupleAssignments.node(node);
		return new ProgramBuilder(program).clearNodes().addNode(node).build();
	}
}
