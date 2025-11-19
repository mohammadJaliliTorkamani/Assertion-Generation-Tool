package org.example;

public class TestResult {
    private int testsRun;
    private int failures;
    private int errors;
    private int passed;

    public TestResult() {
    }

    public void increaseTestsRun() {
        this.testsRun++;
    }

    public void increaseFailures() {
        this.failures++;
    }

    public void increaseErrors() {
        this.errors++;
    }

    public void increasePassed() {
        this.passed++;
    }

    public int getTestsRun() {
        return testsRun;
    }

    public void setTestsRun(int testsRun) {
        this.testsRun = testsRun;
    }

    public int getFailures() {
        return failures;
    }

    public void setFailures(int failures) {
        this.failures = failures;
    }

    public int getErrors() {
        return errors;
    }

    public void setErrors(int errors) {
        this.errors = errors;
    }

    public int getPassed() {
        return passed;
    }

    public void setPassed(int passed) {
        this.passed = passed;
    }
}