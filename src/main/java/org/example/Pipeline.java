package org.example;

import com.google.gson.Gson;
import org.example.call_graph.CallGraphNode;
import org.example.checker.CheckerManager;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.example.Constants.*;

public class Pipeline {
    private PreEvaluator preEvaluator;
    private RepoStorage repoStorage;
    private SimilarRecordsExtractor similarRecordsExtractor;
    private Inference inference;
    private MethodSpecificInfoExtractor methodSpecificInfoExtractor;
    private LLMResponseExtractor llmResponseExtractor;
    private MetricsCalculator metricsCalculator;
    private Evaluator evaluator;
    private String evaluationDb;
    private String fslDb;
    private String outputFile;
    private String averageOutputFile;
    private Integer executionRepeatCounter;
    private EvaluationDataset evaluationDataset;

    public Pipeline setPreEvaluator(PreEvaluator preEvaluator) {
        this.preEvaluator = preEvaluator;
        return this;
    }

    public Pipeline setRepoStorage(RepoStorage repoStorage) {
        this.repoStorage = repoStorage;
        return this;
    }

    public Pipeline setSimilarRecordsExtractor(SimilarRecordsExtractor similarRecordsExtractor) {
        this.similarRecordsExtractor = similarRecordsExtractor;
        return this;
    }

    public Pipeline setInference(Inference inference) {
        this.inference = inference;
        return this;
    }

    public Pipeline setMethodSpecificInfoExtractor(MethodSpecificInfoExtractor methodSpecificInfoExtractor) {
        this.methodSpecificInfoExtractor = methodSpecificInfoExtractor;
        return this;
    }

    public Pipeline setLLMResponseExtractor(LLMResponseExtractor llmResponseExtractor) {
        this.llmResponseExtractor = llmResponseExtractor;
        return this;
    }

    public Pipeline setScoreEvaluator(MetricsCalculator metricsCalculator) {
        this.metricsCalculator = metricsCalculator;
        return this;
    }

    public Pipeline setEvaluator(Evaluator evaluator) {
        this.evaluator = evaluator;
        return this;
    }

    public void execute(String evaluationDb, String fslDb, String outputFile, String averageOutputFile) {
        this.evaluationDb = evaluationDb;
        this.fslDb = fslDb;
        this.outputFile = outputFile;
        this.averageOutputFile = averageOutputFile;
        preInitialize();
        _execute();
    }

    private void _execute() {
        System.out.println("===> Datasets loading...");

        EvaluationDataset evaluationDataset = Utils.loadDataset(evaluationDb, EvaluationDataset.class);
        assert evaluationDataset != null;

        this.evaluationDataset = evaluationDataset;
        System.out.println("Final Evaluation dataset size: " + this.evaluationDataset.getRecordCount());

        FSLDataset fslDataset = Utils.loadDataset(fslDb, FSLDataset.class);
        assert fslDataset != null;

        if (fslDataset.hasIncompleteEmbeddingVectors()) {
            System.out.println("Incomplete Embedding Vectors loaded. Trying to recalculate embedding vectors");
            fslDataset.calculateEmbeddingVectors(inference);
            fslDataset.restoreDataset();
            fslDataset = Utils.loadDataset(fslDb, FSLDataset.class);
            assert fslDataset != null;
        }

        fslDataset.setRecords(fslDataset.getRecordsAsList().stream().filter(record -> record.getEmbeddingVector() != null && !record.getEmbeddingVector().isEmpty()).toList());

        assert !fslDataset.hasIncompleteEmbeddingVectors();

        System.out.println("Final FSL dataset size: " + fslDataset.getRecordCount());
        System.out.println();

        similarRecordsExtractor.setFSLDataset(fslDataset);

        for (Constants.LLM llm : Constants.LLMs_ID) {
            this.inference.getConfig().setModel(llm);
            assert Utils.createDirIfNotExists(Paths.get(this.repoStorage.getProjectRootDir(), this.repoStorage.getExperimentsDirectoryName(), this.repoStorage.getExperimentsDateTime(), llm.getName()).toString());

            for (Constants.Experiment_ID experimentId : Constants.EXPERIMENTS_ID) {
                assert Utils.createDirIfNotExists(Paths.get(this.repoStorage.getProjectRootDir(), this.repoStorage.getExperimentsDirectoryName(), this.repoStorage.getExperimentsDateTime(), llm.getName(), experimentId.name()).toString());

                List<Path> outputPaths = new LinkedList<>();
                for (int counter = 1; counter <= this.executionRepeatCounter; counter++) {
                    Path path = Paths.get(this.repoStorage.getProjectRootDir(), this.repoStorage.getExperimentsDirectoryName(), this.repoStorage.getExperimentsDateTime(), llm.getName(), experimentId.name(), Integer.toString(counter));
                    outputPaths.add(path);
                    assert Utils.createDirIfNotExists(path.toString());
                    assert Utils.createDirIfNotExists(Paths.get(this.repoStorage.getProjectRootDir(), this.repoStorage.getExperimentsDirectoryName(), this.repoStorage.getExperimentsDateTime(), llm.getName(), experimentId.name(), Integer.toString(counter), this.repoStorage.getRepositoriesDirectoryName()).toString());

                    System.out.println("\n===> Executing experiment '" + experimentId + "' using LLM '" + llm.getCompletionModelName() + " and " + llm.getEmbeddingModelName() + "' on counter (" + counter + ") :");
                    System.out.println("=============================");
                    System.out.println();

                    executeExperiment(experimentId, counter);
                    System.out.println();
                    Utils.deleteFile(path, CANDIDATE_SUMMARY_FILE, REFERENCE_SUMMARY_FILE);
                }
                generateAverageStatisticsForRandomnessControl(outputPaths);
            }
        }
    }

    /**
     * calculates average for everything (don't mislead with variable names in the generated report!)
     *
     * @param outputPaths
     */
    public void generateAverageStatisticsForRandomnessControl(List<Path> outputPaths) {
        if (outputPaths.isEmpty())
            return;

        ExperimentResult averageExperimentResult = new ExperimentResult();

        List experimentResults = outputPaths.stream().map(
                path -> {
                    try {
                        return new Gson().fromJson(new FileReader(Paths.get(path.toString(), outputFile).toString()), ExperimentResult.class);
                    } catch (FileNotFoundException e) {
                        throw new RuntimeException(e);
                    }
                }
        ).toList();

        averageExperimentResult.setSize((int) experimentResults.stream().mapToDouble(value -> ((ExperimentResult) value).getSize()).average().orElseThrow(() -> new RuntimeException("No average experiment found")));
        averageExperimentResult.setTimeStamp(Utils.getDateTime(DATE_TIME_FORMAT));
        averageExperimentResult.setExperiment(((ExperimentResult) experimentResults.get(0)).getExperiment());
        averageExperimentResult.setLlmConfig(((ExperimentResult) experimentResults.get(0)).getLlmConfig());

        averageExperimentResult.setNumberOfAssertionFulRecord(experimentResults.stream().mapToDouble(value -> ((ExperimentResult) value).getNumberOfAssertionFulRecord()).average().orElseThrow(() -> new RuntimeException("No average experiment found")));
        averageExperimentResult.setNumberOfAssertionLessRecord(experimentResults.stream().mapToDouble(value -> ((ExperimentResult) value).getNumberOfAssertionLessRecord()).average().orElseThrow(() -> new RuntimeException("No average experiment found")));

        averageExperimentResult.setTotalNumberOfMavenRecords(experimentResults.stream().mapToDouble(value -> ((ExperimentResult) value).getTotalNumberOfMavenRecords()).average().orElseThrow(() -> new RuntimeException("No average experiment found")));
        averageExperimentResult.setTotalNumberOfGradleRecords(experimentResults.stream().mapToDouble(value -> ((ExperimentResult) value).getTotalNumberOfGradleRecords()).average().orElseThrow(() -> new RuntimeException("No average experiment found")));

        averageExperimentResult.setNumberOfRecordsWithSyntacticallyIncorrectInferences(experimentResults.stream().mapToDouble(value -> ((ExperimentResult) value).getNumberOfRecordsWithSyntacticallyIncorrectInferences()).average().orElseThrow(() -> new RuntimeException("No average experiment found")));

        averageExperimentResult.setNumberOfRecordsWithStaticSemanticallyIncorrectInferences(experimentResults.stream().mapToDouble(value -> ((ExperimentResult) value).getNumberOfRecordsWithStaticSemanticallyIncorrectInferences()).average().orElseThrow(() -> new RuntimeException("No average experiment found")));
        averageExperimentResult.setNumberOfRecordsWithStaticSemanticallyIncorrectInferences_UNREACHABLE(experimentResults.stream().mapToDouble(value -> ((ExperimentResult) value).getNumberOfRecordsWithStaticSemanticallyIncorrectInferences_UNREACHABLE()).average().orElseThrow(() -> new RuntimeException("No average experiment found")));
        averageExperimentResult.setNumberOfRecordsWithStaticSemanticallyIncorrectInferences_OTHER_FAILURES(experimentResults.stream().mapToDouble(value -> ((ExperimentResult) value).getNumberOfRecordsWithStaticSemanticallyIncorrectInferences_OTHER_FAILURES()).average().orElseThrow(() -> new RuntimeException("No average experiment found")));

        averageExperimentResult.setNumberOfRecordsWithFailedUnitTestInferences(experimentResults.stream().mapToDouble(value -> ((ExperimentResult) value).getNumberOfRecordsWithFailedUnitTestInferences()).average().orElseThrow(() -> new RuntimeException("No average experiment found")));

        averageExperimentResult.setSyntacticErrorWeightedAverage(experimentResults.stream().mapToDouble(value -> ((ExperimentResult) value).getSyntacticErrorWeightedAverage()).average().orElseThrow(() -> new RuntimeException("No average experiment found")));

        averageExperimentResult.setStaticSemanticErrorWeightedAverage(experimentResults.stream().mapToDouble(value -> ((ExperimentResult) value).getStaticSemanticErrorWeightedAverage()).average().orElseThrow(() -> new RuntimeException("No average experiment found")));

        averageExperimentResult.setDynamicSemanticErrorWeightedAverage(experimentResults.stream().mapToDouble(value -> ((ExperimentResult) value).getDynamicSemanticErrorWeightedAverage()).average().orElseThrow(() -> new RuntimeException("No average experiment found")));

        averageExperimentResult.setTotalNumberOfInferredAssertions(experimentResults.stream().mapToDouble(value -> ((ExperimentResult) value).getTotalNumberOfInferredAssertions()).average().orElseThrow(() -> new RuntimeException("No average experiment found")));

        averageExperimentResult.setTotalNumberOfPickedAssertions(experimentResults.stream().mapToDouble(value -> ((ExperimentResult) value).getTotalNumberOfPickedAssertions()).average().orElseThrow(() -> new RuntimeException("No average experiment found")));

        averageExperimentResult.setTotalNumberOfExecutedUnitTests_covered(experimentResults.stream().mapToDouble(value -> ((ExperimentResult) value).getTotalNumberOfExecutedUnitTests_covered()).average().orElseThrow(() -> new RuntimeException("No average experiment found")));
        averageExperimentResult.setTotalNumberOfExecutedUnitTests_uncovered(experimentResults.stream().mapToDouble(value -> ((ExperimentResult) value).getTotalNumberOfExecutedUnitTests_uncovered()).average().orElseThrow(() -> new RuntimeException("No average experiment found")));
        averageExperimentResult.setTotalNumberOfPassedUnitTests_covered(experimentResults.stream().mapToDouble(value -> ((ExperimentResult) value).getTotalNumberOfPassedUnitTests_covered()).average().orElseThrow(() -> new RuntimeException("No average experiment found")));
        averageExperimentResult.setTotalNumberOfPassedUnitTests_uncovered(experimentResults.stream().mapToDouble(value -> ((ExperimentResult) value).getTotalNumberOfPassedUnitTests_uncovered()).average().orElseThrow(() -> new RuntimeException("No average experiment found")));
        averageExperimentResult.setTotalNumberOfFailedUnitTests_covered(experimentResults.stream().mapToDouble(value -> ((ExperimentResult) value).getTotalNumberOfFailedUnitTests_covered()).average().orElseThrow(() -> new RuntimeException("No average experiment found")));
        averageExperimentResult.setTotalNumberOfFailedUnitTests_uncovered(experimentResults.stream().mapToDouble(value -> ((ExperimentResult) value).getTotalNumberOfFailedUnitTests_uncovered()).average().orElseThrow(() -> new RuntimeException("No average experiment found")));

        averageExperimentResult.setAverageUTP(experimentResults.stream().mapToDouble(value -> ((ExperimentResult) value).getAverageUTP()).average().orElseThrow(() -> new RuntimeException("No average experiment found")));

        averageExperimentResult.setAverageOfAverageLengthOfAssertions_predicted(experimentResults.stream().mapToDouble(value -> ((ExperimentResult) value).getAverageOfAverageLengthOfAssertions_predicted()).average().orElseThrow(() -> new RuntimeException("No average experiment found")));
        averageExperimentResult.setAverageOfAverageLengthOfAssertions_gt(experimentResults.stream().mapToDouble(value -> ((ExperimentResult) value).getAverageOfAverageLengthOfAssertions_gt()).average().orElseThrow(() -> new RuntimeException("No average experiment found")));
        averageExperimentResult.setAverageGenerationTime(experimentResults.stream().mapToDouble(value -> ((ExperimentResult) value).getAverageGenerationTime()).average().orElseThrow(() -> new RuntimeException("No average experiment found")));
        averageExperimentResult.setAverageACDT(experimentResults.stream().mapToDouble(value -> ((ExperimentResult) value).getAverageACDT()).average().orElseThrow(() -> new RuntimeException("No average experiment found")));

        double rouge_F = experimentResults.stream().mapToDouble(value -> ((ExperimentResult) value).getAverageScorePack().getRougeLAverageScore().getRouge_L_averageF()).average().orElseThrow(() -> new RuntimeException("No average experiment found"));
        double rouge_P = experimentResults.stream().mapToDouble(value -> ((ExperimentResult) value).getAverageScorePack().getRougeLAverageScore().getRouge_L_averageP()).average().orElseThrow(() -> new RuntimeException("No average experiment found"));
        double rouge_R = experimentResults.stream().mapToDouble(value -> ((ExperimentResult) value).getAverageScorePack().getRougeLAverageScore().getRouge_L_averageR()).average().orElseThrow(() -> new RuntimeException("No average experiment found"));

        double jaccard_similarity = experimentResults.stream().mapToDouble(value -> ((ExperimentResult) value).getAverageScorePack().getJaccardAverageScore().getSimilarity()).average().orElseThrow(() -> new RuntimeException("No average experiment found"));

        averageExperimentResult.setAverageScorePack(new ScoresPack(new RougeLAverageScore(rouge_F, rouge_P, rouge_R), new JaccardAverageScore(jaccard_similarity)));

        double methodLevel_CR = experimentResults.stream().mapToDouble(value -> ((ExperimentResult) value).getMethodLevelDualMetric().getCR().getScore()).average().orElseThrow(() -> new RuntimeException("No average experiment found"));
        double methodLevel_CSI = experimentResults.stream().mapToDouble(value -> ((ExperimentResult) value).getMethodLevelDualMetric().getCSI().getCSI_Score()).average().orElseThrow(() -> new RuntimeException("No average experiment found"));

        averageExperimentResult.setMethodLevelDualMetric(new DualMetrics(new CardinalityRatio(methodLevel_CR), new CoverageSurplusIndex(methodLevel_CSI)));

        double variableLevel_CR = experimentResults.stream().mapToDouble(value -> ((ExperimentResult) value).getVariableLevelDualMetric().getCR().getScore()).average().orElseThrow(() -> new RuntimeException("No average experiment found"));
        double variableLevel_CSI = experimentResults.stream().mapToDouble(value -> ((ExperimentResult) value).getVariableLevelDualMetric().getCSI().getCSI_Score()).average().orElseThrow(() -> new RuntimeException("No average experiment found"));

        averageExperimentResult.setVariableLevelDualMetric(new DualMetrics(new CardinalityRatio(variableLevel_CR), new CoverageSurplusIndex(variableLevel_CSI)));

        double constantLevel_CR = experimentResults.stream().mapToDouble(value -> ((ExperimentResult) value).getConstantLevelDualMetric().getCR().getScore()).average().orElseThrow(() -> new RuntimeException("No average experiment found"));
        double constantLevel_CSI = experimentResults.stream().mapToDouble(value -> ((ExperimentResult) value).getConstantLevelDualMetric().getCSI().getCSI_Score()).average().orElseThrow(() -> new RuntimeException("No average experiment found"));

        averageExperimentResult.setConstantLevelDualMetric(new DualMetrics(new CardinalityRatio(constantLevel_CR), new CoverageSurplusIndex(constantLevel_CSI)));

        double operatorLevel_CR = experimentResults.stream().mapToDouble(value -> ((ExperimentResult) value).getOperatorLevelDualMetric().getCR().getScore()).average().orElseThrow(() -> new RuntimeException("No average experiment found"));
        double operatorLevel_CSI = experimentResults.stream().mapToDouble(value -> ((ExperimentResult) value).getOperatorLevelDualMetric().getCSI().getCSI_Score()).average().orElseThrow(() -> new RuntimeException("No average experiment found"));

        averageExperimentResult.setOperatorLevelDualMetric(new DualMetrics(new CardinalityRatio(operatorLevel_CR), new CoverageSurplusIndex(operatorLevel_CSI)));

        String path = Paths.get(outputPaths.get(0).getParent().toString(), averageOutputFile).toString();
        Utils.saveToJson(averageExperimentResult, path);
    }

    private void executeExperiment(Constants.Experiment_ID experimentId, int executionCounter) {
        Iterator<Record> evalIterator = evaluationDataset.getRecordsAsIterator();
        List<EvaluationResult> evaluationResults = new LinkedList<>();

        int counter = 0;
        while (evalIterator.hasNext()) {
            counter++;
            Map<String, String> similarMethods;
            Record record = evalIterator.next();
            Parser parser = new Parser(record);
            EvaluationResult result = new EvaluationResult(parser);

            record.print();
            result.initializeFromParser(parser);

            CallGraphNode recordNode = parser.computeCallGraph(true);
            try {
                if (!parser.hasCallGraph())
                    throw new Exception("CALL GRAPH IS EMPTY");

                if (!preEvaluator.evaluate(parser, true, true)) {
                    throw new Exception("PRE-EVALUATION FAILED");
                }

                record.setHasAnyAssertions(recordNode.hasAnyAssertion());
                result.setHasGroundTruthAssertions(recordNode.hasAnyAssertion());
                result.setGroundTruthAssertions(
                        createLineNumberAssertionsPack(
                                Parser.getCommentLessAndJavadocLessMethodDeclaration(
                                        parser.getRecord().findResolvedMethodDeclaration()
                                )
                        )
                );

                repoStorage.storeTemporarily(record, inference.getConfig().getModel().getName(), experimentId, executionCounter);

                result.setGroundTruthAssertionFeatures(parser);

                similarMethods = similarRecordsExtractor.getSimilarMethods(parser, experimentId, inference);

                //------
                long t1 = System.currentTimeMillis();

                MethodSpecificInfo methodSpecificInfo = methodSpecificInfoExtractor.extract(parser, inference);
                long t2 = System.currentTimeMillis();

                Pair<Pair<Pair<LLM_InputContent, String>, AssertionFeatureMap>, Long> timeAndInferenceResponse = inference(experimentId, similarMethods, parser, methodSpecificInfo);

                Pair<Pair<LLM_InputContent, String>, AssertionFeatureMap> inferenceResponse = timeAndInferenceResponse.getFirst();
                //------
                long totalInferenceTime = t2 - t1 + timeAndInferenceResponse.getSecond();

                result.setGenerationTime(totalInferenceTime);

                result.setCommand(inferenceResponse.getFirst().getFirst());

                result.setRawResponse(inferenceResponse.getFirst().getSecond());

                result.setPredictedAssertionFeatures(inferenceResponse.getSecond().getAssertionFeatures());

                result.setAugmentedMethod(parser.getMethodWithAssertionsEmbedded(inferenceResponse.getSecond().getAssertionFeatures().stream().filter(AssertionFeature::isPicked).collect(Collectors.toList())));

                result.setMetrics(metricsCalculator.calculateWithAssertionFeatures(
                        result.getGroundTruthAssertionFeatures(),
                        this.repoStorage.getProjectRootDir(),
                        this.repoStorage.getExperimentsDirectoryName(),
                        this.repoStorage.getExperimentsDateTime(),
                        inference.getConfig().getModel().getName(),
                        experimentId.name(),
                        Integer.toString(executionCounter),
                        record,
                        parser.getRecord().extractAssertions(),
                        inferenceResponse.getSecond().getAssertionFeatures()));

            } catch (Throwable e) {
                e.printStackTrace();
                result.setTag(e.getMessage());
                result.setLog(e.toString());
            } finally {
                String path = Paths.get(this.repoStorage.getProjectRootDir(), this.repoStorage.getExperimentsDirectoryName(), this.repoStorage.getExperimentsDateTime(), inference.getConfig().getModel().getName(), experimentId.name(), Integer.toString(executionCounter), "result_" + counter + ".json").toString();
                Utils.saveToJson(result, path);

                evaluationResults.add(result);
                Utils.deleteDirectory(record.getTempRepoPath());
            }
        }

        String path = Paths.get(this.repoStorage.getProjectRootDir(), this.repoStorage.getExperimentsDirectoryName(), this.repoStorage.getExperimentsDateTime(), inference.getConfig().getModel().getName(), experimentId.name(), Integer.toString(executionCounter), outputFile).toString();
        Utils.saveToJson(
                evaluator.evaluate(evaluationResults, repoStorage.getExperimentsDateTime(), experimentId, inference.getConfig()), path
        );
    }

    private String createLineNumberAssertionsPack(String originalMethod) throws Exception {
        List<Pair<Integer, String>> lineAssertionPack = Utils.extractAssertionsWithLineNumbers(originalMethod);
        StringBuilder pack = new StringBuilder();
        for (Pair<Integer, String> pair : lineAssertionPack) {
            pack
                    .append(Constants.LLM_ASSISTANT_DELIMITER[0])
                    .append("(")
                    .append(pair.getFirst())
                    .append(", ")
                    .append(pair.getSecond())
                    .append(")")
                    .append(Constants.LLM_ASSISTANT_DELIMITER[1])
                    .append("\n");
        }
        return pack.toString();
    }

    private Pair<Pair<Pair<LLM_InputContent, String>, AssertionFeatureMap>, Long> inference(Experiment_ID experimentId, Map<String, String> similarMethods, Parser parser, MethodSpecificInfo methodSpecificInfo) throws Exception {
        Record record = parser.getRecord();

        List<PredictedAssertion> predictedAssertions;
        AssertionFeatureMap predictedAssertionFeatureMap;
        Pair<LLM_InputContent, String> llmRawResponse;

        System.out.println("=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=");
        long t1 = System.currentTimeMillis();
        llmRawResponse = inference.inference(experimentId, similarMethods, record, methodSpecificInfo);
        long t2 = System.currentTimeMillis();
        predictedAssertions = llmResponseExtractor.extract(llmRawResponse.getSecond());
        System.out.println("Predicted assertion size: " + predictedAssertions.size());

        predictedAssertionFeatureMap = CheckerManager
                .getInstance()
                .clearCheckers()
                .syntacticChecker()
                .staticSemanticChecker()
                .indirectDynamicSemanticChecker()
                .check(parser, predictedAssertions);
        predictedAssertionFeatureMap.print();

        AssertionPicker.pickFrom(predictedAssertionFeatureMap);
        System.out.println("=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=");

        return Pair.of(Pair.of(llmRawResponse, predictedAssertionFeatureMap), t2 - t1);
    }

    public Pipeline setExecutionRepeatCounter(Integer executionRepeatCounter) {
        this.executionRepeatCounter = executionRepeatCounter;
        return this;
    }

    private void preInitialize() {
        System.out.println("Pre initializing...");
        Utils.configureJavaParser();
        System.out.println("Pipeline pre-initialized successfully!");
    }
}
