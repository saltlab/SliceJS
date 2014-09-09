package com.slicejs.core.trace;

public class PropertyRead extends RWOperation {
    private String property;
    private boolean functionFlag;
    public String type = "Read";

    public String getProperty() {
        return property;
    }

    public void setProperty(String o) {
        property = o;
    }

    public boolean getFunctionFlag() {
        return functionFlag;
    }

    public void setFunctionFlag(boolean o) {
        functionFlag = o;
    }
}
