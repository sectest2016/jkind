package jkind.translation;

import jkind.lustre.Program;
import jkind.translation.compound.FlattenCompoundTypes;
import jkind.translation.tuples.FlattenTuples;

public class Translate {
	public static Program translate(Program program) {
		program = InlineEnumValues.program(program);
		program = InlineUserTypes.program(program);
		program = InlineConstants.program(program);
		program = RemoveCondacts.program(program);
		program = FlattenFunctionsWithComplexTypes.program(program);
		program = InlineNodeCalls.program(program);
		program = FlattenTuples.program(program);
		program = FlattenCompoundTypes.program(program);
		program = FlattenPres.program(program);
		return program;
	}
}
