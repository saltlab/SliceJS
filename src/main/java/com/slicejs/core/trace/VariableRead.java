package com.slicejs.core.trace;

public class VariableRead extends RWOperation {
	private String value;
	private String definingFunction;
	public String type = "Read";

	public String getValue() {
		return value;
	}

	public void setValue(String o) {
		value = o;
	}
	
	public String getDefiningFunction() {
		return definingFunction;
	}

	public void setDefiningFunction(String o) {
		definingFunction = o;
	}
}
