package org.example;

public class RougeLAverageScore {
    private double rouge_L_averageF;
    private double rouge_L_averageP;
    private double rouge_L_averageR;
    private String referenceSummary;
    private String candidateSummary;

    public RougeLAverageScore(double rouge_L_averageF, double rouge_L_averageP, double rouge_L_averageR,
                              String referenceSummary, String candidateSummary) {
        this.rouge_L_averageF = rouge_L_averageF;
        this.rouge_L_averageP = rouge_L_averageP;
        this.rouge_L_averageR = rouge_L_averageR;
        this.referenceSummary = referenceSummary;
        this.candidateSummary = candidateSummary;
    }

    public RougeLAverageScore(double rouge_L_averageF, double rouge_L_averageP, double rouge_L_averageR) {
        this.rouge_L_averageF = rouge_L_averageF;
        this.rouge_L_averageP = rouge_L_averageP;
        this.rouge_L_averageR = rouge_L_averageR;
    }

    public RougeLAverageScore() {
    }

    public double getRouge_L_averageF() {
        return rouge_L_averageF;
    }

    public void setRouge_L_averageF(double rouge_L_averageF) {
        this.rouge_L_averageF = rouge_L_averageF;
    }

    public double getRouge_L_averageP() {
        return rouge_L_averageP;
    }

    public void setRouge_L_averageP(double rouge_L_averageP) {
        this.rouge_L_averageP = rouge_L_averageP;
    }

    public double getRouge_L_averageR() {
        return rouge_L_averageR;
    }

    public void setRouge_L_averageR(double rouge_L_averageR) {
        this.rouge_L_averageR = rouge_L_averageR;
    }

    public String getReferenceSummary() {
        return referenceSummary;
    }

    public void setReferenceSummary(String referenceSummary) {
        this.referenceSummary = referenceSummary;
    }

    public String getCandidateSummary() {
        return candidateSummary;
    }

    public void setCandidateSummary(String candidateSummary) {
        this.candidateSummary = candidateSummary;
    }
}
