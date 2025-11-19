package org.example;

public class PredictedAssertion {
    private String assertion;
    private int lineNumber;

    public PredictedAssertion(String assertion, int lineNumber) {
        this.assertion = assertion;
        this.lineNumber = lineNumber;
    }

    public String getAssertion() {
        return assertion;
    }

    public void setAssertion(String assertion) {
        this.assertion = assertion;
    }

    public int getLineNumber() {
        return lineNumber;
    }

    public void setLineNumber(int lineNumber) {
        this.lineNumber = lineNumber;
    }
}
