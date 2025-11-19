package org.example;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;

public class LLM_InputContent {
    private String system;
    private List<String> assistant;
    private List<String> user;

    public LLM_InputContent(List<String> user, String system, List<String> assistant) {
        this.system = system;
        this.assistant = assistant;
        this.user = user;
    }

    public LLM_InputContent(List<String> user, String system) {
        this(user, system, new LinkedList<>());
    }

    public LLM_InputContent(List<String> user) {
        this(user, null, new LinkedList<>());
    }

    public List<String> getAssistant() {
        return assistant;
    }

    public void setAssistant(List<String> assistant) {
        this.assistant = assistant;
    }

    public String getSystem() {
        return system;
    }

    public void setSystem(String system) {
        this.system = system;
    }

    public List<String> getUser() {
        return user;
    }

    public void setUser(List<String> user) {
        this.user = user;
    }

    @Override
    public String toString() {
        Iterator<String> userIterator = user.iterator();
        Iterator<String> assistantIterator = assistant.iterator();
        String user = "";
        StringBuilder assistant = new StringBuilder();

        while (userIterator.hasNext()) {
            String userNext = userIterator.next();
            user = user + userNext + (userIterator.hasNext() ? " , " : "");
        }

        while (assistantIterator.hasNext()) {
            String assistantNext = assistantIterator.next();
            if (assistantNext != null)
                assistant.append(assistantNext).append(assistantIterator.hasNext() ? " , " : "");

        }

        return String.format("User input: %s%n%nSystem input: %s%n%nAssistant input: %s%n%n", user, system, assistant.toString());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        LLM_InputContent that = (LLM_InputContent) o;
        return
                Objects.equals(system, that.system) && Objects.equals(user, that.user) && Objects.equals(assistant, that.assistant);
    }

    @Override
    public int hashCode() {
        return Objects.hash(system, user, assistant);
    }
}
