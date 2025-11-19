package org.example;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.expr.*;
import com.github.javaparser.ast.stmt.AssertStmt;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MetricsCalculator {

    private final String scriptPythonFile;
    private String referenceSummaryPath;
    private String candidateSummaryPath;

    public MetricsCalculator(String scriptPythonFile) {
        this.scriptPythonFile = scriptPythonFile;
    }

    public Metrics calculateWithAssertionFeatures(List<AssertionFeature> groundTruthAssertionFeatures, String repoPath, String experimentsDirName, String dateTimeDirName, String modelDirName,
                                                  String experimentName, String experimentCounter, Record record, Set<AssertStmt> groundTruthAssertions, List<AssertionFeature> assertionFeatures) throws Exception {
        this.candidateSummaryPath = Paths.get(repoPath, experimentsDirName, dateTimeDirName, modelDirName, experimentName, experimentCounter, Constants.CANDIDATE_SUMMARY_FILE).toString();
        this.referenceSummaryPath = Paths.get(repoPath, experimentsDirName, dateTimeDirName, modelDirName, experimentName, experimentCounter, Constants.REFERENCE_SUMMARY_FILE).toString();

        Metrics metrics = new Metrics();

        if (!record.hasAnyAssertions()) {
            metrics.setACDT(null);
            metrics.setTokenLevelJaccardScores(null);
            metrics.setTextLeveLRougeLScore(null);
            metrics.setVariableLevelMetrics(null);
            metrics.setOperatorLevelMetrics(null);
            metrics.setMethodLevelMetrics(null);
            metrics.setConstantLevelMetrics(null);
            metrics.setAverageLength_groundtruth(-1);
        }

        if (record.hasAnyAssertions())
            metrics.setAverageLength_groundtruth(Utils.computeAverageLength(groundTruthAssertionFeatures.stream().map(AssertionFeature::getAssertion).map(assertStmt -> StaticJavaParser.parseStatement(assertStmt.toString()).toString()).toList()));

        //average Length
        metrics.setAverageLength_generated(Utils.computeAverageLength(assertionFeatures.stream().map(AssertionFeature::getAssertion).map(assertStmt -> StaticJavaParser.parseStatement(assertStmt.toString()).toString()).toList()));

        if (record.hasAnyAssertions()) {
            //Rouge-L & Jaccard
            List<String> toCalculateScoreReformattedAssertionFeatures = assertionFeatures.stream().map(AssertionFeature::getAssertion).map(assertStmt -> StaticJavaParser.parseStatement(assertStmt.toString()).toString()).toList();
            if (toCalculateScoreReformattedAssertionFeatures.isEmpty()) {
                metrics.setTextLeveLRougeLScore(null);
                metrics.setTokenLevelJaccardScores(null);
            } else {
                if (prepareScores(record, toCalculateScoreReformattedAssertionFeatures, referenceSummaryPath, candidateSummaryPath)) {
                    ScoresPack scoresPack = evaluateScores(referenceSummaryPath, candidateSummaryPath);
                    metrics.setTextLeveLRougeLScore(scoresPack.getRougeLAverageScore());
                    metrics.setTokenLevelJaccardScores(scoresPack.getJaccardAverageScore());
                } else
                    assert false;
            }
            //------ ACDT
            int numberOfAssertionsCoveredDuringTesting_predicted = AssertionFeature.getNumberOfAssertionsCoveredByAtLeastOneUnitTest(assertionFeatures);
            int totalNumberOfGeneratedAssertions_predicted = assertionFeatures.size();
            int numberOfAssertionsCoveredDuringTesting_gt = AssertionFeature.getNumberOfAssertionsCoveredByAtLeastOneUnitTest(groundTruthAssertionFeatures);
            int totalNumberOfGeneratedAssertions_gt = groundTruthAssertionFeatures.size();

            metrics.setACDT(new ACDT(totalNumberOfGeneratedAssertions_predicted == 0 ? 0 : numberOfAssertionsCoveredDuringTesting_predicted * 1.0 / totalNumberOfGeneratedAssertions_predicted, totalNumberOfGeneratedAssertions_gt == 0 ? 0 : numberOfAssertionsCoveredDuringTesting_gt * 1.0 / totalNumberOfGeneratedAssertions_gt));
            //

            //Dual Metrics
            List<DualMetrics> variableLevelDualMetrics = new LinkedList<>();
            List<DualMetrics> methodLevelDualMetrics = new LinkedList<>();
            List<DualMetrics> constantLevelDualMetrics = new LinkedList<>();
            List<DualMetrics> operatorLevelDualMetrics = new LinkedList<>();

            System.out.println("Assertion features size: " + assertionFeatures.size());
            System.out.println("Ground Truth features size: " + groundTruthAssertionFeatures.size());
            for (AssertionFeature assertionFeature : assertionFeatures) {
                for (AssertStmt groundTruthAssertion : groundTruthAssertions) {
                    variableLevelDualMetrics.add(calculateSimilarity(extractVariables(assertionFeature.getAssertion()), extractVariables(groundTruthAssertion)));
                    methodLevelDualMetrics.add(calculateSimilarity(extractMethods(assertionFeature.getAssertion()), extractMethods(groundTruthAssertion)));
                    constantLevelDualMetrics.add(calculateSimilarity(extractConstants(assertionFeature.getAssertion()), extractConstants(groundTruthAssertion)));
                    operatorLevelDualMetrics.add(calculateSimilarity(extractOperators(assertionFeature.getAssertion()), extractOperators(groundTruthAssertion)));
                }
            }

            DualMetrics averagedVariableLevelDualMetric = DualMetrics.calculateAverageDualMetric(variableLevelDualMetrics);
            metrics.setVariableLevelMetrics(averagedVariableLevelDualMetric);
            DualMetrics averagedMethodLevelDualMetric = DualMetrics.calculateAverageDualMetric(methodLevelDualMetrics);
            metrics.setMethodLevelMetrics(averagedMethodLevelDualMetric);
            DualMetrics averagedConstantLevelDualMetric = DualMetrics.calculateAverageDualMetric(constantLevelDualMetrics);
            metrics.setConstantLevelMetrics(averagedConstantLevelDualMetric);
            DualMetrics averagedOperatorLevelDualMetric = DualMetrics.calculateAverageDualMetric(operatorLevelDualMetrics);
            metrics.setOperatorLevelMetrics(averagedOperatorLevelDualMetric);
            //
        }

        //syntactic, static semantic, dynamic semantic
        Accuracy syntacticAccuracy = new Accuracy(assertionFeatures.isEmpty() ? 0 : assertionFeatures.size(),
                assertionFeatures.isEmpty() ? 0 : AssertionFeature.computeNumberOfSyntacticallyCorrectAssertions(assertionFeatures) / assertionFeatures.size());

        Accuracy staticSemanticAccuracy = new Accuracy(assertionFeatures.isEmpty() ? 0 : assertionFeatures.size(),
                assertionFeatures.isEmpty() ? 0 : AssertionFeature.computeNumberOfStaticSemanticallyCorrectAssertions(assertionFeatures) / assertionFeatures.size());

        Accuracy dynamicSemanticAccuracy = new Accuracy(assertionFeatures.isEmpty() ? 0 : assertionFeatures.size(),
                assertionFeatures.isEmpty() ? 0 : AssertionFeature.computeNumberOfDynamicSemanticallyCorrectAssertions(assertionFeatures) / assertionFeatures.size());

        metrics.setSyntacticCorrectnessScore(syntacticAccuracy);
        metrics.setStaticSemanticCorrectnessScore(staticSemanticAccuracy);
        metrics.setDynamicSemanticCorrectnessScore(dynamicSemanticAccuracy);


        //----- UTP -----
        int numberOfExecutedTests = AssertionFeature.getNumberOfTests(assertionFeatures);
        int numberOfPassedTests = AssertionFeature.getNumberOfPassedTests(assertionFeatures);
        metrics.setUTP(numberOfExecutedTests == 0 ? 0 : numberOfPassedTests * 1.0 / numberOfExecutedTests);

        return metrics;
    }

    private List<String> extractVariables(AssertStmt assertion) {
        List<String> variables = new ArrayList<>();
        assertion.walk(NameExpr.class, nameExpr -> variables.add(nameExpr.getNameAsString()));

        return variables;
    }

    private List<String> extractMethods(AssertStmt assertion) {
        List<String> methods = new ArrayList<>();
        assertion.walk(MethodCallExpr.class, nameExpr -> {
            methods.add(nameExpr.getNameAsString());
        });

        return methods;
    }

    private List<String> extractConstants(AssertStmt assertion) {
        List<String> constants = new ArrayList<>();
        assertion.walk(LiteralExpr.class, nameExpr -> {
            constants.add(nameExpr.toString());
        });

        return constants;
    }

    private List<String> extractOperators(AssertStmt assertion) {
        List<String> operators = new ArrayList<>();

        assertion.walk(BinaryExpr.class, bin -> {
            operators.add(bin.getOperator().asString());
        });

        assertion.walk(UnaryExpr.class, un -> {
            operators.add(un.getOperator().asString());
        });


        return operators;
    }

    private DualMetrics calculateSimilarity(List<String> predictedElements, List<String> groundTruthElements) {
        System.out.println("Calculating similarity: " + predictedElements.size() + " | " + groundTruthElements.size());
        DualMetrics metrics = new DualMetrics();
        metrics.setCR(new CardinalityRatio(new HashSet<>(predictedElements), new HashSet<>(groundTruthElements)));
        metrics.setCSI(new CoverageSurplusIndex(new HashSet<>(predictedElements), new HashSet<>(groundTruthElements)));
        return metrics;
    }

    private boolean prepareScores(Record record, List<String> newMethodAssertions, String referenceSummaryPath, String candidateSummaryPath) throws Exception {
        Set<AssertStmt> originalAssertions = record.extractAssertions();
        StringBuilder originalAssertionsStr = new StringBuilder();
        StringBuilder newMethodAssertionsStr = new StringBuilder();

        originalAssertions.forEach(assertion -> originalAssertionsStr.append(StaticJavaParser.parseStatement(assertion.toString()).toString()).append("\n"));
        newMethodAssertions.forEach(assertion -> newMethodAssertionsStr.append(assertion).append("\n"));

        return createFile(referenceSummaryPath, originalAssertionsStr.toString()) &&
                createFile(candidateSummaryPath, newMethodAssertionsStr.toString());
    }

    private boolean createFile(String path, String content) throws Exception {
        File file = new File(path);
        if (file.exists()) {
            if (!file.delete())
                return false;
        }
        if (file.createNewFile()) {
            FileWriter fileWriter = new FileWriter(path, false);
            fileWriter.write(content.trim());
            fileWriter.close();
            return true;
        }

        return false;
    }

    private ScoresPack evaluateScores(String referenceSummaryPath, String candidateSummaryPath) throws Exception {
        ScoresPack scoresPack = new ScoresPack();
        List<String> originalAssertions = Files.readAllLines(Path.of(referenceSummaryPath));
        List<String> newMethodAssertions = Files.readAllLines(Path.of(candidateSummaryPath));
        StringBuilder originalAssertionsStr = new StringBuilder();
        StringBuilder newMethodAssertionsStr = new StringBuilder();

        originalAssertions.forEach(assertion -> originalAssertionsStr.append(assertion).append("\n"));
        newMethodAssertions.forEach(assertion -> newMethodAssertionsStr.append(assertion).append("\n"));

        String command = "source ./models/env/bin/activate && python3" + " " + scriptPythonFile + " " + referenceSummaryPath + " " + candidateSummaryPath;
        String[] terminalCommand = {"/bin/bash", "-c", command};
        ProcessBuilder processBuilder = new ProcessBuilder(terminalCommand);
        processBuilder.directory(new File(Paths.get(Constants.PROJECT_ROOT_DIR, Constants.PYTHON_SCRIPT_DIR).toString()));
        processBuilder.environment().putAll(System.getenv());
        processBuilder.redirectErrorStream(true);
        Process process = processBuilder.start();
        InputStream inputStream = process.getInputStream();
        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
        String line;
        while ((line = reader.readLine()) != null) {
            Pattern pattern = Pattern.compile(
                    "\\{'rouge':\\s*\\{'rougeL':\\s*\\{'F':\\s*(\\d+\\.\\d+),\\s*'P':\\s*(\\d+\\.\\d+),\\s*'R':\\s*(\\d+\\.\\d+)\\}\\},\\s*'jaccard':\\s*\\{'similarity':\\s*(\\d+\\.\\d+)\\}\\}"
            );
            Matcher matcher = pattern.matcher(line.trim());
            if (matcher.find()) {
                String candidateSummary = newMethodAssertionsStr.toString().trim();
                String referenceSummary = originalAssertionsStr.toString().trim();

                RougeLAverageScore rougeLAverageScore = new RougeLAverageScore();
                JaccardAverageScore jaccardAverageScore = new JaccardAverageScore();


                rougeLAverageScore.setCandidateSummary(candidateSummary);
                jaccardAverageScore.setCandidateSummary(candidateSummary);

                rougeLAverageScore.setReferenceSummary(referenceSummary);
                jaccardAverageScore.setReferenceSummary(referenceSummary);

                rougeLAverageScore.setRouge_L_averageF(Double.parseDouble(matcher.group(1)));
                rougeLAverageScore.setRouge_L_averageP(Double.parseDouble(matcher.group(2)));
                rougeLAverageScore.setRouge_L_averageR(Double.parseDouble(matcher.group(3)));

                jaccardAverageScore.setSimilarity(Double.parseDouble(matcher.group(4)));

                scoresPack.setRougeLAverageScore(rougeLAverageScore);
                scoresPack.setJaccardAverageScore(jaccardAverageScore);
                break;
            } else
                System.out.println("Matcher not found!");
        }

        int exitCode = process.waitFor();

        return scoresPack;
    }
}