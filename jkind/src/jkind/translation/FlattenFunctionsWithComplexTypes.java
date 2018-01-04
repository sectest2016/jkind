package jkind.translation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import jkind.lustre.ArrayAccessExpr;
import jkind.lustre.ArrayType;
import jkind.lustre.Expr;
import jkind.lustre.Function;
import jkind.lustre.FunctionCallExpr;
import jkind.lustre.Program;
import jkind.lustre.RecordAccessExpr;
import jkind.lustre.RecordType;
import jkind.lustre.TupleExpr;
import jkind.lustre.Type;
import jkind.lustre.VarDecl;
import jkind.lustre.builders.ProgramBuilder;
import jkind.lustre.visitors.TypeAwareAstMapVisitor;
import jkind.util.Util;

public class FlattenFunctionsWithComplexTypes extends TypeAwareAstMapVisitor {
	public static final String functionRecordDelimeter = "~r~";
	public static final String functionArrayDelimeter = "~a~";
	public static final String functionTupleDelimeter = "~t~";

	public FlattenFunctionsWithComplexTypes(Program program) {
		super(program);
		this.functionMap = Util.getFunctionTable(program.functions);
	}

	private final Map<String, Function> functionMap;
	
	public static Program program(Program program) {

		// this flattens all the record arguments
		program = new FlattenFunctionsWithComplexTypes(program).visit(program);

		// if the function has a record type return value then make copies for
		// the arguments
		List<Function> newFuncs = new ArrayList<>();
		for (Function func : program.functions) {
			// If the function returns a tuple, we use the return-value id
			// in the name.
			boolean useReturnId = func.outputs.size() > 1;
			for (VarDecl returnVar: func.outputs) {
				if (returnVar.type instanceof RecordType) {
					List<VarDecl> flatVars = flattenVar(returnVar);
					for (VarDecl flatVar : flatVars) {
						String name = flatVar.id.replace(".", functionRecordDelimeter);
						name = name.replaceAll("\\[([^]]+)\\]", functionArrayDelimeter + "$1");
						if (useReturnId)
							name = functionTupleDelimeter + name;
						else
							name = name.replaceFirst("[^~]*" + functionRecordDelimeter, functionRecordDelimeter);
						name = func.id + name;
						newFuncs.add(new Function(name, func.inputs, Collections.singletonList(flatVar)));
					}
				}
				else if (returnVar.type instanceof ArrayType) {
					List<VarDecl> flatVars = flattenVar(returnVar);
					for (VarDecl flatVar : flatVars) {
						String name = flatVar.id.replaceAll("\\[([^]]+)\\]", functionArrayDelimeter + "$1");
						name = name.replace(".", functionRecordDelimeter);
						if (useReturnId)
							name = functionTupleDelimeter + name;
						else
							name = name.replaceFirst("[^~]*" + functionArrayDelimeter, functionArrayDelimeter);
						name = func.id + name;
						newFuncs.add(new Function(name, func.inputs, Collections.singletonList(flatVar)));
					}
				}
				// Regardless of the above cases, ensure there's an entry for the tuple function
				// itself.
				if (useReturnId) {
					String name = returnVar.id;
					name = functionTupleDelimeter + name;
					name = func.id + name;
					newFuncs.add(new Function(name, func.inputs, Collections.singletonList(returnVar)));
				}
			}
			newFuncs.add(func);

		}
		return new ProgramBuilder(program).clearFunctions().addFunctions(newFuncs).build();
	}

	@Override
	public Function visit(Function e) {
		List<VarDecl> inputs = flattenVars(e.inputs);
		List<VarDecl> outputs = visitVarDecls(e.outputs);
		return new Function(e.location, e.id, inputs, outputs);
	}

	@Override
	public Expr visit(FunctionCallExpr expr) {
		Function func = functionMap.get(expr.function);
		if (func == null || func.outputs.size() == 1) {
			List<Expr> args = visitExprs(expr.args);
			return new FunctionCallExpr(expr.function, flattenAllAccesses(args));
		}
		else {
			ArrayList<FunctionCallExpr> elements = new ArrayList<>();
			for (VarDecl output : func.outputs) {
				String name = expr.function + functionTupleDelimeter + output.id; 
				List<Expr> args = visitExprs(expr.args);
				elements.add(new FunctionCallExpr(name, flattenAllAccesses(args)));
			}
			TupleExpr tuple = new TupleExpr(elements);
			return tuple;
		}
		
		
	}

	
	
	public List<Expr> flattenAllAccesses(List<Expr> exprs) {
		List<Expr> accesses = new ArrayList<>();
		for (Expr expr : exprs) {
			accesses.addAll(flattenAccesses(expr));
		}
		return accesses;
	}

	public List<Expr> flattenAccesses(Expr expr) {
		List<Expr> accesses = new ArrayList<>();
		Type type = getType(expr);

		if (type instanceof RecordType) {
			RecordType recType = (RecordType) type;
			for (Entry<String, Type> entry : recType.fields.entrySet()) {
				Expr newExpr = new RecordAccessExpr(expr, entry.getKey());
				accesses.addAll(flattenAccesses(newExpr));
			}
		} else if (type instanceof ArrayType) {
			ArrayType arrType = (ArrayType) type;
			for (Integer n = 0; n < arrType.size; n++) {
				Expr newExpr = new ArrayAccessExpr(expr, n);
				accesses.addAll(flattenAccesses(newExpr));
			}
		} else {
			accesses.add(expr);
		}
		return accesses;

	}

	public static List<VarDecl> flattenVars(List<VarDecl> vars) {
		List<VarDecl> flats = new ArrayList<>();
		for (VarDecl var : vars) {
			flats.addAll(flattenVar(var));
		}
		return flats;
	}

	public static List<VarDecl> flattenVar(VarDecl var) {
		List<VarDecl> flats = new ArrayList<>();
		Type type = var.type;
		if (type instanceof RecordType) {
			RecordType recType = (RecordType) type;
			for (Entry<String, Type> entry : recType.fields.entrySet()) {
				List<VarDecl> subFlats = flattenVar(new VarDecl(entry.getKey(), entry.getValue()));
				for (VarDecl subVar : subFlats) {
					flats.add(new VarDecl(var.id + "." + subVar.id, subVar.type));
				}
			}
		} else if (type instanceof ArrayType) {
			ArrayType recType = (ArrayType) type;
			for (Integer n = 0; n < recType.size; n++) {				
				String subName = n.toString(); // String.format("sub_%d",	n);
				List<VarDecl> subFlats = flattenVar(new VarDecl(subName, recType.base));
				for (VarDecl subVar : subFlats) {
					flats.add(new VarDecl(var.id + "[" + subVar.id + "]", subVar.type));
				}
			}
		} else {
			flats.add(var);
		}
		return flats;
	}

}
