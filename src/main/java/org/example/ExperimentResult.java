package org.example;

import java.util.List;

public class ExperimentResult {
    private int size;
    private String timeStamp;
    private String experiment;

    private LLM_Config llmConfig;

    private double numberOfAssertionFulRecord;
    private double numberOfAssertionLessRecord;

    private double totalNumberOfMavenRecords;
    private double totalNumberOfGradleRecords;

    private double NumberOfRecordsWithSyntacticallyIncorrectInferences;

    private double NumberOfRecordsWithStaticSemanticallyIncorrectInferences;//for those whose ground truth has assertions, and after inference, they are syntactically correct but are static semantically wrong (for any reason), leading to the compilation failure
    private double NumberOfRecordsWithStaticSemanticallyIncorrectInferences_UNREACHABLE;//for those whose ground truth has assertions, and after inference, they are syntactically correct but their inferred assertion line number makes it unreachable, leading to the compilation failure
    private double NumberOfRecordsWithStaticSemanticallyIncorrectInferences_OTHER_FAILURES;//for those whose ground truth has assertions, and after inference, they are syntactically correct but their compilation is failed due to anything except unreachable statement, leading to the compilation failure

    private double NumberOfRecordsWithFailedUnitTestInferences;

    private double totalNumberOfInferredAssertions;

    private double totalNumberOfPickedAssertions;

    private double totalNumberOfExecutedUnitTests_covered;
    private double totalNumberOfExecutedUnitTests_uncovered;
    private double totalNumberOfPassedUnitTests_covered;
    private double totalNumberOfPassedUnitTests_uncovered;
    private double totalNumberOfFailedUnitTests_covered;
    private double totalNumberOfFailedUnitTests_uncovered;

    private double syntacticErrorWeightedAverage;
    private double staticSemanticErrorWeightedAverage;
    private double dynamicSemanticErrorWeightedAverage;

    private double averageUTP;

    private double averageOfAverageLengthOfAssertions_predicted;
    private double averageOfAverageLengthOfAssertions_gt;
    private double averageGenerationTime;
    private double averageACDT;

    private ScoresPack averageScorePack;

    private DualMetrics variableLevelDualMetric;
    private DualMetrics methodLevelDualMetric;
    private DualMetrics constantLevelDualMetric;
    private DualMetrics operatorLevelDualMetric;

    public ExperimentResult(List<EvaluationResult> evaluationResults) {
        this.size = evaluationResults.size();
    }

    public ExperimentResult(int size) {
        this.size = size;
    }

    public ExperimentResult() {
    }

    public String getTimeStamp() {
        return timeStamp;
    }

    public void setTimeStamp(String timeStamp) {
        this.timeStamp = timeStamp;
    }

    public LLM_Config getLlmConfig() {
        return llmConfig;
    }

    public void setLlmConfig(LLM_Config llmConfig) {
        this.llmConfig = llmConfig;
    }

    public double getNumberOfAssertionFulRecord() {
        return numberOfAssertionFulRecord;
    }

    public void setNumberOfAssertionFulRecord(double numberOfAssertionFulRecord) {
        this.numberOfAssertionFulRecord = numberOfAssertionFulRecord;
    }

    public double getNumberOfAssertionLessRecord() {
        return numberOfAssertionLessRecord;
    }

    public void setNumberOfAssertionLessRecord(double numberOfAssertionLessRecord) {
        this.numberOfAssertionLessRecord = numberOfAssertionLessRecord;
    }

    public double getTotalNumberOfMavenRecords() {
        return totalNumberOfMavenRecords;
    }

    public void setTotalNumberOfMavenRecords(double totalNumberOfMavenRecords) {
        this.totalNumberOfMavenRecords = totalNumberOfMavenRecords;
    }

    public double getTotalNumberOfGradleRecords() {
        return totalNumberOfGradleRecords;
    }

    public void setTotalNumberOfGradleRecords(double totalNumberOfGradleRecords) {
        this.totalNumberOfGradleRecords = totalNumberOfGradleRecords;
    }

    public double getNumberOfRecordsWithSyntacticallyIncorrectInferences() {
        return NumberOfRecordsWithSyntacticallyIncorrectInferences;
    }

    public void setNumberOfRecordsWithSyntacticallyIncorrectInferences(double numberOfRecordsWithSyntacticallyIncorrectInferences) {
        NumberOfRecordsWithSyntacticallyIncorrectInferences = numberOfRecordsWithSyntacticallyIncorrectInferences;
    }

    public double getNumberOfRecordsWithStaticSemanticallyIncorrectInferences() {
        return NumberOfRecordsWithStaticSemanticallyIncorrectInferences;
    }

    public void setNumberOfRecordsWithStaticSemanticallyIncorrectInferences(double numberOfRecordsWithStaticSemanticallyIncorrectInferences) {
        NumberOfRecordsWithStaticSemanticallyIncorrectInferences = numberOfRecordsWithStaticSemanticallyIncorrectInferences;
    }

    public double getNumberOfRecordsWithStaticSemanticallyIncorrectInferences_UNREACHABLE() {
        return NumberOfRecordsWithStaticSemanticallyIncorrectInferences_UNREACHABLE;
    }

    public void setNumberOfRecordsWithStaticSemanticallyIncorrectInferences_UNREACHABLE(double numberOfRecordsWithStaticSemanticallyIncorrectInferences_UNREACHABLE) {
        NumberOfRecordsWithStaticSemanticallyIncorrectInferences_UNREACHABLE = numberOfRecordsWithStaticSemanticallyIncorrectInferences_UNREACHABLE;
    }

    public double getNumberOfRecordsWithStaticSemanticallyIncorrectInferences_OTHER_FAILURES() {
        return NumberOfRecordsWithStaticSemanticallyIncorrectInferences_OTHER_FAILURES;
    }

    public void setNumberOfRecordsWithStaticSemanticallyIncorrectInferences_OTHER_FAILURES(double numberOfRecordsWithStaticSemanticallyIncorrectInferences_OTHER_FAILURES) {
        NumberOfRecordsWithStaticSemanticallyIncorrectInferences_OTHER_FAILURES = numberOfRecordsWithStaticSemanticallyIncorrectInferences_OTHER_FAILURES;
    }

    public double getNumberOfRecordsWithFailedUnitTestInferences() {
        return NumberOfRecordsWithFailedUnitTestInferences;
    }

    public void setNumberOfRecordsWithFailedUnitTestInferences(double numberOfRecordsWithFailedUnitTestInferences) {
        NumberOfRecordsWithFailedUnitTestInferences = numberOfRecordsWithFailedUnitTestInferences;
    }

    public void increaseNumberOfRecordsWithFailedUnitTestInferences() {
        NumberOfRecordsWithFailedUnitTestInferences++;
    }

    public double getTotalNumberOfInferredAssertions() {
        return totalNumberOfInferredAssertions;
    }

    public void setTotalNumberOfInferredAssertions(double totalNumberOfInferredAssertions) {
        this.totalNumberOfInferredAssertions = totalNumberOfInferredAssertions;
    }

    public double getTotΩalNumberOfExecutedUnitTests_uncovered() {
        return totalNumberOfExecutedUnitTests_uncovered;
    }

    public double getTotalNumberOfPassedUnitTests_uncovered() {
        return totalNumberOfPassedUnitTests_uncovered;
    }

    public void setTotalNumberOfPassedUnitTests_uncovered(double totalNumberOfPassedUnitTests_uncovered) {
        this.totalNumberOfPassedUnitTests_uncovered = totalNumberOfPassedUnitTests_uncovered;
    }

    public double getTotalNumberOfFailedUnitTests_covered() {
        return totalNumberOfFailedUnitTests_covered;
    }

    public void setTotalNumberOfFailedUnitTests_covered(double totalNumberOfFailedUnitTests_covered) {
        this.totalNumberOfFailedUnitTests_covered = totalNumberOfFailedUnitTests_covered;
    }

    public double getTotalNumberOfFailedUnitTests_uncovered() {
        return totalNumberOfFailedUnitTests_uncovered;
    }

    public void setTotalNumberOfFailedUnitTests_uncovered(double totalNumberOfFailedUnitTests_uncovered) {
        this.totalNumberOfFailedUnitTests_uncovered = totalNumberOfFailedUnitTests_uncovered;
    }

    public double getTotalNumberOfExecutedUnitTests_covered() {
        return totalNumberOfExecutedUnitTests_covered;
    }

    public void setTotalNumberOfExecutedUnitTests_covered(double totalNumberOfExecutedUnitTests_covered) {
        this.totalNumberOfExecutedUnitTests_covered = totalNumberOfExecutedUnitTests_covered;
    }

    public double getTotalNumberOfExecutedUnitTests_uncovered() {
        return totalNumberOfExecutedUnitTests_uncovered;
    }

    public void setTotalNumberOfExecutedUnitTests_uncovered(double totalNumberOfExecutedUnitTests_uncovered) {
        this.totalNumberOfExecutedUnitTests_uncovered = totalNumberOfExecutedUnitTests_uncovered;
    }

    public double getTotalNumberOfPassedUnitTests_covered() {
        return totalNumberOfPassedUnitTests_covered;
    }

    public void setTotalNumberOfPassedUnitTests_covered(double totalNumberOfPassedUnitTests_covered) {
        this.totalNumberOfPassedUnitTests_covered = totalNumberOfPassedUnitTests_covered;
    }

    public double getAverageOfAverageLengthOfAssertions_gt() {
        return averageOfAverageLengthOfAssertions_gt;
    }

    public void setAverageOfAverageLengthOfAssertions_gt(double averageOfAverageLengthOfAssertions_gt) {
        this.averageOfAverageLengthOfAssertions_gt = averageOfAverageLengthOfAssertions_gt;
    }

    public double getAverageOfAverageLengthOfAssertions_predicted() {
        return averageOfAverageLengthOfAssertions_predicted;
    }

    public void setAverageOfAverageLengthOfAssertions_predicted(double averageOfAverageLengthOfAssertions_predicted) {
        this.averageOfAverageLengthOfAssertions_predicted = averageOfAverageLengthOfAssertions_predicted;
    }

    public double getAverageGenerationTime() {
        return averageGenerationTime;
    }

    public void setAverageGenerationTime(double averageGenerationTime) {
        this.averageGenerationTime = averageGenerationTime;
    }

    public double getAverageACDT() {
        return averageACDT;
    }

    public void setAverageACDT(double averageACDT) {
        this.averageACDT = averageACDT;
    }

    public ScoresPack getAverageScorePack() {
        return averageScorePack;
    }

    public void setAverageScorePack(ScoresPack averageScorePack) {
        this.averageScorePack = averageScorePack;
    }

    public DualMetrics getVariableLevelDualMetric() {
        return variableLevelDualMetric;
    }

    public void setVariableLevelDualMetric(DualMetrics variableLevelDualMetric) {
        this.variableLevelDualMetric = variableLevelDualMetric;
    }

    public DualMetrics getMethodLevelDualMetric() {
        return methodLevelDualMetric ;
    }

    public void setMethodLevelDualMetric(DualMetrics methodLevelDualMetric) {
        this.methodLevelDualMetric = methodLevelDualMetric;
    }

    public DualMetrics getConstantLevelDualMetric() {
        return constantLevelDualMetric;
    }

    public void setConstantLevelDualMetric(DualMetrics constantLevelDualMetric) {
        this.constantLevelDualMetric = constantLevelDualMetric;
    }

    public DualMetrics getOperatorLevelDualMetric() {
        return operatorLevelDualMetric;
    }

    public void setOperatorLevelDualMetric(DualMetrics operatorLevelDualMetric) {
        this.operatorLevelDualMetric = operatorLevelDualMetric;
    }

    public void increaseNumberOfAssertionFulRecord() {
        numberOfAssertionFulRecord++;
    }

    public void increaseNumberOfAssertionLessRecord() {
        numberOfAssertionLessRecord++;
    }

    public void increaseNumberOfRecordsWithSyntacticallyIncorrectInferences() {
        NumberOfRecordsWithSyntacticallyIncorrectInferences++;
    }

    public void increaseNumberOfRecordsWithFailedUnitTestInferences(double counter) {
        NumberOfRecordsWithFailedUnitTestInferences += counter;
    }

    public void increaseTotalNumberOfInferredAssertions(double counter) {
        totalNumberOfInferredAssertions += counter;
    }

    public void increaseTotalNumberOfPickedAssertions(double counter) {
        totalNumberOfPickedAssertions += counter;
    }

    public void increaseTotalNumberOfMavenRecords() {
        totalNumberOfMavenRecords++;
    }

    public void increaseTotalNumberOfGradleRecords() {
        totalNumberOfGradleRecords++;
    }

    public void increaseNumberOfRecordsWithStaticSemanticallyIncorrectInferences() {
        NumberOfRecordsWithStaticSemanticallyIncorrectInferences++;
    }

    public void increaseNumberOfRecordsWithStaticSemanticallyIncorrectInferences_UNREACHABLE() {
        NumberOfRecordsWithStaticSemanticallyIncorrectInferences_UNREACHABLE++;
    }

    public void increaseNumberOfRecordsWithStaticSemanticallyIncorrectInferences_OTHER_FAILURES() {
        NumberOfRecordsWithStaticSemanticallyIncorrectInferences_OTHER_FAILURES++;
    }

    public int getSize() {
        return size;
    }

    public void setSize(int size) {
        this.size = size;
    }

    public String getExperiment() {
        return experiment;
    }

    public void setExperiment(String experiment) {
        this.experiment = experiment;
    }

    public void increaseTotalNumberOfFailedUnitTests_uncovered(double counter) {
        this.totalNumberOfFailedUnitTests_uncovered += counter;
    }

    public void increaseTotalNumberOfFailedUnitTests_covered(double counter) {
        this.totalNumberOfFailedUnitTests_covered += counter;
    }

    public void increaseTotalNumberOfPassedUnitTests_uncovered(double counter) {
        this.totalNumberOfPassedUnitTests_uncovered += counter;
    }

    public void increaseTotalNumberOfPassedUnitTests_covered(double counter) {
        this.totalNumberOfPassedUnitTests_covered += counter;
    }

    public void increaseTotalNumberOfExecutedUnitTests_uncovered(double counter) {
        this.totalNumberOfExecutedUnitTests_uncovered += counter;
    }

    public void increaseTotalNumberOfExecutedUnitTests_covered(double counter) {
        this.totalNumberOfExecutedUnitTests_covered += counter;
    }

    public double getTotalNumberOfPickedAssertions() {
        return totalNumberOfPickedAssertions;
    }

    public void setTotalNumberOfPickedAssertions(double totalNumberOfPickedAssertions) {
        this.totalNumberOfPickedAssertions = totalNumberOfPickedAssertions;
    }

    public double getSyntacticErrorWeightedAverage() {
        return syntacticErrorWeightedAverage;
    }

    public void setSyntacticErrorWeightedAverage(double syntacticErrorWeightedAverage) {
        this.syntacticErrorWeightedAverage = syntacticErrorWeightedAverage;
    }

    public double getStaticSemanticErrorWeightedAverage() {
        return staticSemanticErrorWeightedAverage;
    }

    public void setStaticSemanticErrorWeightedAverage(double staticSemanticErrorWeightedAverage) {
        this.staticSemanticErrorWeightedAverage = staticSemanticErrorWeightedAverage;
    }

    public double getDynamicSemanticErrorWeightedAverage() {
        return dynamicSemanticErrorWeightedAverage;
    }

    public void setDynamicSemanticErrorWeightedAverage(double dynamicSemanticErrorWeightedAverage) {
        this.dynamicSemanticErrorWeightedAverage = dynamicSemanticErrorWeightedAverage;
    }

    public double getAverageUTP() {
        return averageUTP;
    }

    public void setAverageUTP(double averageUTP) {
        this.averageUTP = averageUTP;
    }
}
