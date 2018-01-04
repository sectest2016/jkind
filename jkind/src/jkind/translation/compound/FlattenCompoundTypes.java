package jkind.translation.compound;

import jkind.lustre.Node;
import jkind.lustre.Program;
import jkind.lustre.builders.ProgramBuilder;

/**
 * Flatten arrays and records to scalars
 * 
 * Assumption: All node calls have been inlined.
 */
public class FlattenCompoundTypes {
	public static Program program(Program program) {
		Node node = program.getMainNode();
		node = RemoveNonConstantArrayIndices.node(node, program);
		node = RemoveArrayUpdates.node(node, program);
		node = RemoveRecordUpdates.node(node, program);
		node = FlattenCompoundComparisons.node(node, program);
		node = FlattenCompoundVariables.node(node);
		program = new ProgramBuilder(program).clearNodes().addNode(node).build();
		program = FlattenCompoundExpressions.program(program);
		return program;
	}
}
