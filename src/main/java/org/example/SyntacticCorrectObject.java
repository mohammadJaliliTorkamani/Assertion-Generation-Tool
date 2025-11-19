package org.example;

import java.util.Objects;

public class SyntacticCorrectObject {
    private boolean correct;
    private String message;

    public SyntacticCorrectObject(boolean correct, String message) {
        this.correct = correct;
        this.message = message;
    }

    public SyntacticCorrectObject() {
        this.message = null;
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
        SyntacticCorrectObject that = (SyntacticCorrectObject) o;
        return correct == that.correct && Objects.equals(message, that.message);
    }

    @Override
    public int hashCode() {
        return Objects.hash(correct, message);
    }
}
