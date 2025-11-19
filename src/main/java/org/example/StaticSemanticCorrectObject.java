package org.example;

import java.util.Objects;

public class StaticSemanticCorrectObject {
    private final ComponentResponse componentResponse;
    private boolean correct; //if the compilation error is successful
    private String message;
    private boolean reachable; //if either no compilation error, or the error message has nothing related to unreachable statement

    public StaticSemanticCorrectObject(ComponentResponse componentResponse, String message, boolean correct, boolean reachable) {
        this.componentResponse = componentResponse;
        this.correct = correct;
        this.message = message;
        this.reachable = reachable;

    }

    public ComponentResponse getComponentResponse() {
        return componentResponse;
    }

    public boolean isReachable() {
        return reachable;
    }

    public void setReachable(boolean reachable) {
        this.reachable = reachable;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public boolean isCorrect() {
        return correct;
    }

    public void setCorrect(boolean correct) {
        this.correct = correct;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        StaticSemanticCorrectObject that = (StaticSemanticCorrectObject) o;
        return correct == that.correct && Objects.equals(message, that.message) && reachable == that.reachable;
    }

    @Override
    public int hashCode() {
        return Objects.hash(correct, message);
    }
}
