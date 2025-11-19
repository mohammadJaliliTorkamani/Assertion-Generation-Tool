package org.example;

import org.example.call_graph.CallGraphNode;

import java.util.Objects;

public class UnitTestExecutionResult {
    private final ComponentResponse componentResponse;
    private boolean passed;
    private String message;
    private String testName;
    private String testPath;
    private boolean covered;
    private boolean directlyCallsTargetMethod;

    public UnitTestExecutionResult(ComponentResponse componentResponse, String message, CallGraphNode test, boolean directlyCallsTargetMethod) {
        this.componentResponse = componentResponse;
        this.passed = componentResponse != null && componentResponse.isOK();
        this.message = message;
        this.testName = test.getMethodDeclaration().getNameAsString();
        this.testPath = test.getPath().toString();
        this.covered = componentResponse != null && hasFlag(componentResponse.getMessage(), Constants.ASSERTRON_COVERAGE_FLAG_MESSAGE);
        this.directlyCallsTargetMethod = directlyCallsTargetMethod;
    }

    public boolean isDirectlyCallsTargetMethod() {
        return directlyCallsTargetMethod;
    }

    public void setDirectlyCallsTargetMethod(boolean directlyCallsTargetMethod) {
        this.directlyCallsTargetMethod = directlyCallsTargetMethod;
    }

    private boolean hasFlag(String message, String messageToFind) {
        return message != null && message.contains(messageToFind);
    }

    public boolean isCovered() {
        return covered;
    }

    public void setCovered(boolean covered) {
        this.covered = covered;
    }

    public String getTestName() {
        return testName;
    }

    public String getTestPath() {
        return testPath;
    }

    public ComponentResponse getComponentResponse() {
        return componentResponse;
    }

    public boolean isPassed() {
        return passed;
    }

    public void setPassed(boolean passed) {
        this.passed = passed;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        UnitTestExecutionResult that = (UnitTestExecutionResult) o;
        return passed == that.passed && Objects.equals(message, that.message) && Objects.equals(testName, that.testName) && Objects.equals(testPath, that.testPath);
    }

    @Override
    public int hashCode() {
        return Objects.hash(passed, message, testName, testPath);
    }
}
