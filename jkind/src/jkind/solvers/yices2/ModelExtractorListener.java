package jkind.solvers.yices2;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.antlr.v4.runtime.tree.TerminalNode;

import jkind.lustre.Function;
import jkind.lustre.NamedType;
import jkind.lustre.Type;
import jkind.lustre.VarDecl;
import jkind.lustre.values.BooleanValue;
import jkind.lustre.values.IntegerValue;
import jkind.lustre.values.RealValue;
import jkind.lustre.values.Value;
import jkind.solvers.yices2.Yices2Parser.AliasContext;
import jkind.solvers.yices2.Yices2Parser.FunctionContext;
import jkind.solvers.yices2.Yices2Parser.FunctionTypeContext;
import jkind.solvers.yices2.Yices2Parser.FunctionValueContext;
import jkind.solvers.yices2.Yices2Parser.IntegerContext;
import jkind.solvers.yices2.Yices2Parser.IntegerNumericContext;
import jkind.solvers.yices2.Yices2Parser.NegativeIntegerContext;
import jkind.solvers.yices2.Yices2Parser.PositiveIntegerContext;
import jkind.solvers.yices2.Yices2Parser.QuotientContext;
import jkind.solvers.yices2.Yices2Parser.QuotientNumericContext;
import jkind.solvers.yices2.Yices2Parser.TypeContext;
import jkind.solvers.yices2.Yices2Parser.ValueContext;
import jkind.solvers.yices2.Yices2Parser.VariableContext;
import jkind.util.BigFraction;
import jkind.util.FunctionTable;
import jkind.util.FunctionTableRow;

public class ModelExtractorListener extends Yices2BaseListener {
	private final Yices2Model model;

	public ModelExtractorListener(Map<String, Type> varTypes, List<Function> functions) {
		this.model = new Yices2Model(varTypes, functions);
	}

	public Yices2Model getModel() {
		return model;
	}

	@Override
	public void enterAlias(AliasContext ctx) {
		model.addAlias(ctx.ID(0).getText(), ctx.ID(1).getText());
	}

	@Override
	public void enterVariable(VariableContext ctx) {
		model.addValue(ctx.ID().getText(), value(ctx.value()));
	}

	private Value value(ValueContext ctx) {
		if (ctx.BOOL() instanceof TerminalNode) {
			return BooleanValue.fromBoolean(ctx.BOOL().getText().equals("true"));
		} else if (ctx.numeric() instanceof IntegerNumericContext) {
			IntegerNumericContext ictx = (IntegerNumericContext) ctx.numeric();
			return new IntegerValue(integer(ictx.integer()));
		} else if (ctx.numeric() instanceof QuotientNumericContext) {
			QuotientNumericContext qctx = (QuotientNumericContext) ctx.numeric();
			qctx.quotient().numeric(0);
			return new RealValue(quotient(qctx.quotient()));
		} else {
			throw new IllegalArgumentException();
		}
	}

	private BigFraction quotient(QuotientContext ctx) {
		IntegerNumericContext num = (IntegerNumericContext) ctx.numeric(0);
		IntegerNumericContext den = (IntegerNumericContext) ctx.numeric(1);
		return new BigFraction(integer(num.integer()), integer(den.integer()));
	}

	private BigInteger integer(IntegerContext ctx) {
		if (ctx instanceof PositiveIntegerContext) {
			PositiveIntegerContext pctx = (PositiveIntegerContext) ctx;
			return new BigInteger(pctx.INT().getText());
		} else if (ctx instanceof NegativeIntegerContext) {
			NegativeIntegerContext nctx = (NegativeIntegerContext) ctx;
			return new BigInteger(nctx.INT().getText()).negate();
		} else {
			throw new IllegalArgumentException();
		}
	}

	@Override
	public void enterFunction(FunctionContext ctx) {
		String name = ctx.ID().getText();

		FunctionTable table = model.getFunctionTable(name);
		if (table == null) {
			FunctionTypeContext typeCtx = ctx.functionType();
			int n = typeCtx.type().size() - 1;
			List<VarDecl> inputs = new ArrayList<>();
			for (int i = 0; i < n; i++) {
				inputs.add(new VarDecl("", type(typeCtx.type(i))));
			}
			VarDecl output = new VarDecl("", type(typeCtx.type(n)));
			
			table = new FunctionTable(name, inputs, output);
			model.addFunctionTable(name, table);
		}

		for (FunctionValueContext valueCtx : ctx.functionValue()) {
			int n = valueCtx.value().size() - 1;
			List<Value> inputs = new ArrayList<Value>();
			for (int i = 0; i < n; i++) {
				inputs.add(value(valueCtx.value(i)));
			}
			Value output = value(valueCtx.value(n));
			table.addRow(new FunctionTableRow(inputs, output));
		}

		if (ctx.defaultValue() != null) {
			Value defaultValue = value(ctx.defaultValue().value());
			model.setFunctionDefaultValue(name, defaultValue);
		}
	}

	private Type type(TypeContext ctx) {
		switch (ctx.getText()) {
		case "int":
			return NamedType.INT;
		case "real":
			return NamedType.REAL;
		case "bool":
			return NamedType.BOOL;
		default:
			throw new IllegalArgumentException("Unknown Yices2 type: " + ctx.getText());
		}
	}
}