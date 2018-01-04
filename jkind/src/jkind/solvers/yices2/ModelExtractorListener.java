package jkind.solvers.yices2;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.antlr.v4.runtime.tree.TerminalNode;

import jkind.lustre.Function;
import jkind.lustre.Type;
import jkind.lustre.values.BooleanValue;
import jkind.lustre.values.FunctionValue;
import jkind.lustre.values.IntegerValue;
import jkind.lustre.values.RealValue;
import jkind.lustre.values.Value;
import jkind.sexp.Cons;
import jkind.sexp.Sexp;
import jkind.sexp.Symbol;
import jkind.solvers.yices.YicesModel;
import jkind.solvers.yices2.Yices2Parser.AliasContext;
import jkind.solvers.yices2.Yices2Parser.FunctionContext;
import jkind.solvers.yices2.Yices2Parser.FunctionTypeContext;
import jkind.solvers.yices2.Yices2Parser.FunctionValueContext;
import jkind.solvers.yices2.Yices2Parser.IntegerContext;
import jkind.solvers.yices2.Yices2Parser.IntegerNumericContext;
import jkind.solvers.yices2.Yices2Parser.NegativeIntegerContext;
import jkind.solvers.yices2.Yices2Parser.NumericContext;
import jkind.solvers.yices2.Yices2Parser.PositiveIntegerContext;
import jkind.solvers.yices2.Yices2Parser.QuotientContext;
import jkind.solvers.yices2.Yices2Parser.QuotientNumericContext;
import jkind.solvers.yices2.Yices2Parser.ValueContext;
import jkind.solvers.yices2.Yices2Parser.VariableContext;
import jkind.util.BigFraction;

public class ModelExtractorListener extends Yices2BaseListener {
	private final YicesModel model;
	private static final String fnArgPrefix = "x";
	
	public ModelExtractorListener(Map<String, Type> varTypes, List<Function> functions) {
		this.model = new YicesModel(varTypes, functions);
	}

	public YicesModel getModel() {
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

	private Sexp sexpInteger(IntegerContext ctx) {
		if (ctx instanceof PositiveIntegerContext) {
			return new Symbol(ctx.getText());
		} else if (ctx instanceof NegativeIntegerContext) {
			return new Cons("-", new Symbol("0"), new Symbol(((NegativeIntegerContext)ctx).INT().getText()));
		} else
			throw new RuntimeException("Unknown integer context");
	}

	private BigFraction quotient(QuotientContext ctx) {
		IntegerNumericContext num = (IntegerNumericContext)ctx.numeric(0);
		IntegerNumericContext den = (IntegerNumericContext)ctx.numeric(1);
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

	private Sexp numericToSexp(NumericContext ctx) {
		if (ctx instanceof IntegerNumericContext) {
			IntegerNumericContext ictx = (IntegerNumericContext) ctx;
			return sexpInteger(ictx.integer());
		} else if (ctx instanceof QuotientNumericContext) {
			QuotientNumericContext qctx = (QuotientNumericContext) ctx;
			return new Symbol(numericToSexp(qctx.quotient().numeric(0)).toString()
					+ "/" + numericToSexp(qctx.quotient().numeric(1)).toString());
		} else {
			throw new IllegalArgumentException();
		}
	}
	
	private static Sexp fnValsToITE(List<Sexp> inputVals, Sexp outputVal, Sexp elseExpr) {
		Sexp ands = null;
		int i = 0;
		for (Sexp inputVal : inputVals) {
			Sexp eq = new Cons("=", new Symbol(fnArgPrefix + i++), inputVal);
			if (ands == null) {
				ands = eq;
			} else {
				ands = new Cons("and", ands, eq);
			}
		}
		return new Cons("ite", ands, outputVal, elseExpr);
	}
	
	private static Symbol type(String s) {
		if (s.equals("int"))
			return new Symbol("Int");
		else if (s.equals("bool"))
			return new Symbol("Bool");
		else if (s.equals("real"))
			return new Symbol("Real");
		else
			throw new IllegalArgumentException(s);
	}
	
	@Override
	public void enterFunction(FunctionContext ctx) {
		String id = ctx.ID().getText();

		List<Sexp> argDefs = new ArrayList<>();
		
		FunctionTypeContext tctx = ctx.functionType();
		for (int i = 0; i < (tctx.type().size() - 1); i++) {
			argDefs.add(new Cons(fnArgPrefix + i, type(tctx.type(i).getText())));
		}

		Sexp body = null;
		
		for (FunctionValueContext fctx : ctx.functionValue()) {
			List<Sexp> inputs = new ArrayList<>(); 
			
			for (NumericContext nctx : fctx.numeric()) {
				inputs.add(numericToSexp(nctx));
			}
			//NumericContext
			Sexp output = new Symbol(value(fctx.value()).toString());
			if (body == null)
				body = output;
			body = fnValsToITE(inputs, output, body);
		}
		Sexp args = new Cons(argDefs);
		body = new Cons(args, body);
		
		FunctionValue fn = new FunctionValue(id, body); 
		model.addValue(id, fn);
	}
	
}