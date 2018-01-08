package jkind.util;

import java.util.List;

import jkind.lustre.values.BooleanValue;
import jkind.lustre.values.IntegerValue;
import jkind.lustre.values.RealValue;
import jkind.lustre.values.Value;

public class FunctionTableRow implements Comparable<FunctionTableRow> {
	private final List<Value> inputs;

	public FunctionTableRow(List<Value> inputs) {
		this.inputs = Util.safeList(inputs);
	}

	public List<Value> getInputs() {
		return inputs;
	}

	@Override
	public int hashCode() {
		return inputs.hashCode();
	}

	@Override
	public boolean equals(Object o) {
		if (o instanceof FunctionTableRow) {
			FunctionTableRow row = (FunctionTableRow) o;
			return row.inputs.equals(inputs);
		}
		return false;
	}

	@Override
	public int compareTo(FunctionTableRow other) {
		for (int i = 0; i < inputs.size(); i++) {
			Value x = inputs.get(i);
			Value y = other.inputs.get(i);
			int r = compareValues(x, y);
			if (r != 0) {
				return r;
			}
		}

		return 0;
	}

	private int compareValues(Value x, Value y) {
		if (x instanceof BooleanValue && y instanceof BooleanValue) {
			BooleanValue xbv = (BooleanValue) x;
			BooleanValue ybv = (BooleanValue) y;
			return Boolean.compare(xbv.value, ybv.value);
		} else if (x instanceof IntegerValue && y instanceof IntegerValue) {
			IntegerValue xiv = (IntegerValue) x;
			IntegerValue yiv = (IntegerValue) y;
			return xiv.value.compareTo(yiv.value);
		} else if (x instanceof RealValue && y instanceof RealValue) {
			RealValue xrv = (RealValue) x;
			RealValue yrv = (RealValue) y;
			return xrv.value.compareTo(yrv.value);
		} else {
			throw new IllegalArgumentException();
		}
	}
}
