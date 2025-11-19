package org.example;

import java.util.Map;

public class MethodSpecificInfo {
    private String summary;
    private Map<String, String> dependenciesSummaries;

    public MethodSpecificInfo(String summary, String ioDescription, Map<String, String> dependenciesSummaries) {
        this.summary = summary;
        this.dependenciesSummaries = dependenciesSummaries;
    }

    public Map<String, String> getDependenciesSummaries() {
        return dependenciesSummaries;
    }

    public void setDependenciesSummaries(Map<String, String> dependenciesSummaries) {
        this.dependenciesSummaries = dependenciesSummaries;
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public String getInvokedMethodsDescriptionAsString() {
        if (dependenciesSummaries == null || dependenciesSummaries.isEmpty())
            return null;

        StringBuilder invokedMethodsDescriptionStr = new StringBuilder();
        for (Map.Entry<String, String> entry : dependenciesSummaries.entrySet()) {
            if (entry.getValue() != null)
                invokedMethodsDescriptionStr.append(" - ").append("' ").append(entry.getKey()).append(" '").append(" method: ")
                        .append(entry.getValue()).append(entry.getValue().endsWith(".") ? " " : ". ").append("\n");
        }

        return invokedMethodsDescriptionStr.isEmpty() ? null : invokedMethodsDescriptionStr.toString();
    }

    @Override
    public String toString() {
        return "MethodSpecificInfo{" +
                "summary='" + summary + '\'' +
                ", dependenciesSummaries=" + dependenciesSummaries +
                '}';
    }
}
