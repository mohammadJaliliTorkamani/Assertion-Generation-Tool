package org.example;

public class ACDT {
    private double predictedCoverageRatio;
    private double originalCoverageRatio;
    private double score;

    public ACDT(double predictedCoverageRatio, double originalCoverageRatio) {
        this.predictedCoverageRatio = predictedCoverageRatio;
        this.originalCoverageRatio = originalCoverageRatio;
        this.score = predictedCoverageRatio == 0 ? 0 : (originalCoverageRatio == 0 ? -1 : predictedCoverageRatio / originalCoverageRatio); //-1 means uninitialized
    }

    public double getScore() {
        return score;
    }

    public double getOriginalCoverageRatio() {
        return originalCoverageRatio;
    }

    public void setOriginalCoverageRatio(double originalCoverageRatio) {
        this.originalCoverageRatio = originalCoverageRatio;
    }

    public double getPredictedCoverageRatio() {
        return predictedCoverageRatio;
    }

    public void setPredictedCoverageRatio(double predictedCoverageRatio) {
        this.predictedCoverageRatio = predictedCoverageRatio;
    }
}
