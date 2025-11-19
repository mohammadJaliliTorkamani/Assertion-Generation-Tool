package org.example;

public class JaccardAverageScore {
    private double similarity;
    private String referenceSummary;
    private String candidateSummary;

    public JaccardAverageScore() {
    }

    public JaccardAverageScore(double similarity, String referenceSummary, String candidateSummary) {
        this.similarity = similarity;
        this.referenceSummary = referenceSummary;
        this.candidateSummary = candidateSummary;
    }

    public JaccardAverageScore(double similarity) {
        this.similarity = similarity;
    }

    public String getCandidateSummary() {
        return candidateSummary;
    }

    public void setCandidateSummary(String candidateSummary) {
        this.candidateSummary = candidateSummary;
    }

    public String getReferenceSummary() {
        return referenceSummary;
    }

    public void setReferenceSummary(String referenceSummary) {
        this.referenceSummary = referenceSummary;
    }

    public double getSimilarity() {
        return similarity;
    }

    public void setSimilarity(double similarity) {
        this.similarity = similarity;
    }
}
