package org.example;

public class Metrics {
    private ScoresPack scoresPack;

    private DualMetrics variableLevelMetrics;
    private DualMetrics constantLevelMetrics;
    private DualMetrics methodLevelMetrics;
    private DualMetrics operatorLevelMetrics;
    private ACDT ACDT;

    private double UTP;
    private double averageLength_generated;//of assertions
    private double averageLength_groundtruth;//of assertions
    private Accuracy syntacticCorrectnessScore;
    private Accuracy staticSemanticCorrectnessScore;
    private Accuracy dynamicSemanticCorrectnessScore;

    public ScoresPack getScoresPack() {
        return scoresPack;
    }

    public double getAverageLength_generated() {
        return averageLength_generated;
    }

    public void setAverageLength_generated(double averageLength_generated) {
        this.averageLength_generated = averageLength_generated;
    }

    public double getAverageLength_groundtruth() {
        return averageLength_groundtruth;
    }

    public void setAverageLength_groundtruth(double averageLength_groundtruth) {
        this.averageLength_groundtruth = averageLength_groundtruth;
    }

    public RougeLAverageScore getTextLeveLRougeLScore() {
        return scoresPack == null ? null : scoresPack.getRougeLAverageScore();
    }

    public void setTextLeveLRougeLScore(RougeLAverageScore textLeveLRougeLScore) {
        if (scoresPack == null) {
            ScoresPack scoresPack1 = new ScoresPack();
            scoresPack1.setRougeLAverageScore(textLeveLRougeLScore);
            scoresPack = scoresPack1;
        } else
            scoresPack.setRougeLAverageScore(textLeveLRougeLScore);
    }

    public JaccardAverageScore getTokenLevelJaccardScores() {
        return scoresPack == null ? null : scoresPack.getJaccardAverageScore();
    }

    public void setTokenLevelJaccardScores(JaccardAverageScore tokenLevelJaccardScores) {
        if (scoresPack == null) {
            ScoresPack scoresPack1 = new ScoresPack();
            scoresPack1.setJaccardAverageScore(tokenLevelJaccardScores);
            scoresPack = scoresPack1;
        } else
            scoresPack.setJaccardAverageScore(tokenLevelJaccardScores);
    }

    public DualMetrics getVariableLevelMetrics() {
        return variableLevelMetrics;
    }

    public void setVariableLevelMetrics(DualMetrics variableLevelMetrics) {
        this.variableLevelMetrics = variableLevelMetrics;
    }

    public DualMetrics getConstantLevelMetrics() {
        return constantLevelMetrics;
    }

    public void setConstantLevelMetrics(DualMetrics constantLevelMetrics) {
        this.constantLevelMetrics = constantLevelMetrics;
    }

    public DualMetrics getMethodLevelMetrics() {
        return methodLevelMetrics;
    }

    public void setMethodLevelMetrics(DualMetrics methodLevelMetrics) {
        this.methodLevelMetrics = methodLevelMetrics;
    }

    public DualMetrics getOperatorLevelMetrics() {
        return operatorLevelMetrics;
    }

    public void setOperatorLevelMetrics(DualMetrics operatorLevelMetrics) {
        this.operatorLevelMetrics = operatorLevelMetrics;
    }

    public Accuracy getSyntacticCorrectnessScore() {
        return syntacticCorrectnessScore;
    }

    public void setSyntacticCorrectnessScore(Accuracy syntacticCorrectnessScore) {
        this.syntacticCorrectnessScore = syntacticCorrectnessScore;
    }

    public Accuracy getStaticSemanticCorrectnessScore() {
        return staticSemanticCorrectnessScore;
    }

    public void setStaticSemanticCorrectnessScore(Accuracy staticSemanticCorrectnessScore) {
        this.staticSemanticCorrectnessScore = staticSemanticCorrectnessScore;
    }

    public Accuracy getDynamicSemanticCorrectnessScore() {
        return dynamicSemanticCorrectnessScore;
    }

    public void setDynamicSemanticCorrectnessScore(Accuracy dynamicSemanticCorrectnessScore) {
        this.dynamicSemanticCorrectnessScore = dynamicSemanticCorrectnessScore;
    }

    public double getUTP() {
        return UTP;
    }

    public void setUTP(double UTP) {
        this.UTP = UTP;
    }

    public org.example.ACDT getACDT() {
        return ACDT;
    }

    public void setACDT(org.example.ACDT ACDT) {
        this.ACDT = ACDT;
    }
}
