package org.example;

import org.example.checker.StaticSemanticChecker;

import java.util.List;
import java.util.Objects;

public class Evaluator {
    /**
     * Calculates sum for 'number of bla bla' and average for rates and errors
     *
     * @param evaluationResults
     * @param timeStamp
     * @param experimentId
     * @param config
     * @return
     */
    public ExperimentResult evaluate(List<EvaluationResult> evaluationResults,
                                     String timeStamp, Constants.Experiment_ID experimentId, LLM_Config config) {

        System.out.println("Evaluating...");

        ExperimentResult experimentResult = new ExperimentResult(evaluationResults);

        experimentResult.setTimeStamp(timeStamp);
        experimentResult.setExperiment(experimentId.name());

        experimentResult.setLlmConfig(config);

        for (EvaluationResult evaluationResult : evaluationResults) {
            if (evaluationResult.hasGroundTruthAssertions())
                experimentResult.increaseNumberOfAssertionFulRecord();
            else
                experimentResult.increaseNumberOfAssertionLessRecord();
        }

        for (EvaluationResult evaluationResult : evaluationResults)
            if (evaluationResult.getBuildTool() != null)
                switch (evaluationResult.getBuildTool()) {
                    case "Gradle":
                        experimentResult.increaseTotalNumberOfGradleRecords();
                        break;
                    case "Maven":
                        experimentResult.increaseTotalNumberOfMavenRecords();
                        break;
                }

        for (EvaluationResult evaluationResult : evaluationResults)
            if (evaluationResult.isSyntacticallyWrong_predicted())
                experimentResult.increaseNumberOfRecordsWithSyntacticallyIncorrectInferences();


        for (EvaluationResult evaluationResult : evaluationResults) {
            StaticSemanticChecker.FailureType failureType = evaluationResult.isStaticSemanticallyWrong_predicted();
            if (failureType != null) {
                switch (failureType) {
                    case UNREACHABLE_STATEMENT ->
                            experimentResult.increaseNumberOfRecordsWithStaticSemanticallyIncorrectInferences_UNREACHABLE();
                    case OTHER_FAILURE_TYPES ->
                            experimentResult.increaseNumberOfRecordsWithStaticSemanticallyIncorrectInferences_OTHER_FAILURES();
                }
                experimentResult.increaseNumberOfRecordsWithStaticSemanticallyIncorrectInferences();
            }


        }

        for (EvaluationResult evaluationResult : evaluationResults) {
            if (evaluationResult.isDynamicSemanticallyWrong_predicted()) {
                experimentResult.increaseNumberOfRecordsWithFailedUnitTestInferences();
            }

        }

        for (EvaluationResult evaluationResult : evaluationResults) {
            if (evaluationResult.getPredictedAssertionFeatures() != null)
                experimentResult.increaseTotalNumberOfInferredAssertions(evaluationResult.getPredictedAssertionFeatures().size());

        }

        for (EvaluationResult evaluationResult : evaluationResults)
            if (evaluationResult.getPickedAssertionFeatures() != null)
                experimentResult.increaseTotalNumberOfPickedAssertions(evaluationResult.getPickedAssertionFeatures().size());


        for (EvaluationResult evaluationResult : evaluationResults) {
            Pair<Pair<Integer, Pair<Integer, Integer>>, Pair<Integer, Pair<Integer, Integer>>> executionResults = evaluationResult.getTestExecutionStatistics();
            if (executionResults != null) {//neither syntactically nor static semantically incorrect
                Pair<Integer, Pair<Integer, Integer>> coveredExecutionResults = executionResults.getFirst();
                Pair<Integer, Pair<Integer, Integer>> uncoveredExecutionResults = executionResults.getSecond();
                experimentResult.increaseTotalNumberOfExecutedUnitTests_covered(coveredExecutionResults.getFirst());
                experimentResult.increaseTotalNumberOfPassedUnitTests_covered(coveredExecutionResults.getSecond().getFirst());
                experimentResult.increaseTotalNumberOfFailedUnitTests_covered(coveredExecutionResults.getSecond().getSecond());
                experimentResult.increaseTotalNumberOfExecutedUnitTests_uncovered(uncoveredExecutionResults.getFirst());
                experimentResult.increaseTotalNumberOfPassedUnitTests_uncovered(uncoveredExecutionResults.getSecond().getFirst());
                experimentResult.increaseTotalNumberOfFailedUnitTests_uncovered(uncoveredExecutionResults.getSecond().getSecond());
            }
        }

        double syntactic_sumOfNominator = 0;
        double syntactic_sumOfDenominator = 0;

        double static_semantic_sumOfNominator = 0;
        double static_semantic_sumOfDenominator = 0;

        double dynamic_semantic_sumOfNominator = 0;
        double dynamic_semantic_sumOfDenominator = 0;

        for (EvaluationResult evaluationResult : evaluationResults) {
            syntactic_sumOfNominator += (evaluationResult.getMetrics().getSyntacticCorrectnessScore().getW() * evaluationResult.getMetrics().getSyntacticCorrectnessScore().getAccuracy());
            syntactic_sumOfDenominator += (evaluationResult.getMetrics().getSyntacticCorrectnessScore().getW());

            static_semantic_sumOfNominator += (evaluationResult.getMetrics().getStaticSemanticCorrectnessScore().getW() * evaluationResult.getMetrics().getStaticSemanticCorrectnessScore().getAccuracy());
            static_semantic_sumOfDenominator += (evaluationResult.getMetrics().getStaticSemanticCorrectnessScore().getW());

            dynamic_semantic_sumOfNominator += (evaluationResult.getMetrics().getDynamicSemanticCorrectnessScore().getW() * evaluationResult.getMetrics().getDynamicSemanticCorrectnessScore().getAccuracy());
            dynamic_semantic_sumOfDenominator += (evaluationResult.getMetrics().getDynamicSemanticCorrectnessScore().getW());
        }

        experimentResult.setSyntacticErrorWeightedAverage(syntactic_sumOfDenominator == 0 ? 0 : syntactic_sumOfNominator / syntactic_sumOfDenominator);

        experimentResult.setStaticSemanticErrorWeightedAverage(static_semantic_sumOfDenominator == 0 ? 0 : static_semantic_sumOfNominator / static_semantic_sumOfDenominator);

        experimentResult.setDynamicSemanticErrorWeightedAverage(dynamic_semantic_sumOfDenominator == 0 ? 0 : dynamic_semantic_sumOfNominator / dynamic_semantic_sumOfDenominator);


        experimentResult.setAverageUTP(evaluationResults.stream().mapToDouble(value -> value.getMetrics().getUTP()).average().orElse(0));

        experimentResult.setAverageOfAverageLengthOfAssertions_predicted(evaluationResults.stream().mapToDouble(evaluationResult -> evaluationResult.getMetrics().getAverageLength_generated()).filter(value -> value != -1).average().orElse(0));
        experimentResult.setAverageOfAverageLengthOfAssertions_gt(evaluationResults.stream().mapToDouble(evaluationResult -> evaluationResult.getMetrics().getAverageLength_groundtruth()).filter(value -> value != -1).average().orElse(0));
        experimentResult.setAverageGenerationTime(evaluationResults.stream().mapToLong(EvaluationResult::getGenerationTime).average().orElse(0));

        experimentResult.setAverageACDT(evaluationResults.stream().map(evaluationResult -> evaluationResult.getMetrics().getACDT()).filter(Objects::nonNull).mapToDouble(ACDT::getScore).average().orElse(0));

        double avgF = evaluationResults.stream()
                .map(EvaluationResult::getMetrics)
                .map(Metrics::getScoresPack)
                .filter(Objects::nonNull)
                .map(ScoresPack::getRougeLAverageScore)
                .filter(Objects::nonNull)
                .mapToDouble(RougeLAverageScore::getRouge_L_averageF)
                .average().orElse(0);

        double avgP = evaluationResults.stream()
                .map(EvaluationResult::getMetrics)
                .map(Metrics::getScoresPack)
                .filter(Objects::nonNull)
                .map(ScoresPack::getRougeLAverageScore)
                .filter(Objects::nonNull)
                .mapToDouble(RougeLAverageScore::getRouge_L_averageP)
                .average().orElse(0);

        double avgR = evaluationResults.stream()
                .map(EvaluationResult::getMetrics)
                .map(Metrics::getScoresPack)
                .filter(Objects::nonNull)
                .map(ScoresPack::getRougeLAverageScore)
                .filter(Objects::nonNull)
                .mapToDouble(RougeLAverageScore::getRouge_L_averageR)
                .average().orElse(0);

        double avgJaccard = evaluationResults.stream()
                .map(EvaluationResult::getMetrics)
                .map(Metrics::getScoresPack)
                .filter(Objects::nonNull)
                .map(ScoresPack::getJaccardAverageScore)
                .filter(Objects::nonNull)
                .mapToDouble(JaccardAverageScore::getSimilarity)
                .average().orElse(0);

        experimentResult.setAverageScorePack(new ScoresPack(new RougeLAverageScore(avgF, avgP, avgR), new JaccardAverageScore(avgJaccard)));

        double avgCR = evaluationResults.stream()
                .map(EvaluationResult::getMetrics)
                .map(Metrics::getMethodLevelMetrics)
                .filter(Objects::nonNull)
                .mapToDouble(dualMetrics -> dualMetrics.getCR().getScore())
                .filter(score -> score != -1)
                .average().orElse(0);

        double avgCSI = evaluationResults.stream()
                .map(EvaluationResult::getMetrics)
                .map(Metrics::getMethodLevelMetrics)
                .filter(Objects::nonNull)
                .mapToDouble(dualMetrics -> dualMetrics.getCSI().getCSI_Score())
                .filter(score -> score != -1)
                .average().orElse(0);

        experimentResult.setMethodLevelDualMetric(new DualMetrics(new CardinalityRatio(avgCR), new CoverageSurplusIndex(avgCSI)));

        avgCR = evaluationResults.stream()
                .map(EvaluationResult::getMetrics)
                .map(Metrics::getVariableLevelMetrics)
                .filter(Objects::nonNull)
                .mapToDouble(dualMetrics -> dualMetrics.getCR().getScore())
                .filter(score -> score != -1)
                .average().orElse(0);

        avgCSI = evaluationResults.stream()
                .map(EvaluationResult::getMetrics)
                .map(Metrics::getVariableLevelMetrics)
                .filter(Objects::nonNull)
                .mapToDouble(dualMetrics -> dualMetrics.getCSI().getCSI_Score())
                .filter(score -> score != -1)
                .average().orElse(0);

        experimentResult.setVariableLevelDualMetric(new DualMetrics(new CardinalityRatio(avgCR), new CoverageSurplusIndex(avgCSI)));

        avgCR = evaluationResults.stream()
                .map(EvaluationResult::getMetrics)
                .map(Metrics::getConstantLevelMetrics)
                .filter(Objects::nonNull)
                .mapToDouble(dualMetrics -> dualMetrics.getCR().getScore())
                .filter(score -> score != -1)
                .average().orElse(0);

        avgCSI = evaluationResults.stream()
                .map(EvaluationResult::getMetrics)
                .map(Metrics::getConstantLevelMetrics)
                .filter(Objects::nonNull)
                .mapToDouble(dualMetrics -> dualMetrics.getCSI().getCSI_Score())
                .filter(score -> score != -1)
                .average().orElse(0);

        experimentResult.setConstantLevelDualMetric(new DualMetrics(new CardinalityRatio(avgCR), new CoverageSurplusIndex(avgCSI)));

        avgCR = evaluationResults.stream()
                .map(EvaluationResult::getMetrics)
                .map(Metrics::getOperatorLevelMetrics)
                .filter(Objects::nonNull)
                .mapToDouble(dualMetrics -> dualMetrics.getCR().getScore())
                .filter(score -> score != -1)
                .average().orElse(0);

        avgCSI = evaluationResults.stream()
                .map(EvaluationResult::getMetrics)
                .map(Metrics::getOperatorLevelMetrics)
                .filter(Objects::nonNull)
                .mapToDouble(dualMetrics -> dualMetrics.getCSI().getCSI_Score())
                .filter(score -> score != -1)
                .average().orElse(0);

        experimentResult.setOperatorLevelDualMetric(new DualMetrics(new CardinalityRatio(avgCR), new CoverageSurplusIndex(avgCSI)));


        return experimentResult;
    }
}
