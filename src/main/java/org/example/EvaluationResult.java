package org.example;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.stmt.AssertStmt;
import com.github.javaparser.printer.configuration.Indentation;
import com.github.javaparser.printer.configuration.PrettyPrinterConfiguration;
import org.example.call_graph.CallGraphNode;
import org.example.call_graph.RepoJavaParser;
import org.example.checker.DynamicSemanticChecker;
import org.example.checker.StaticSemanticChecker;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

public class EvaluationResult {

    private static int counter = 0;
    private final int id;

    private transient final Parser parser;
    private Metrics metrics;
    private long generationTime;

    private List<AssertionFeature> predictedAssertionFeatures;
    private List<AssertionFeature> groundTruthAssertionFeatures;
    private boolean productionRecord;
    private String className;
    private String buildTool;
    private String name;
    private String path;
    private String repoPath;
    private String signature;
    private String repoName;
    private String packageName;
    private int startLine;
    private String middleModulePath;
    private int endLine;
    private String tempRepoPath;
    private boolean hasGroundTruthAssertions;
    private String log;
    private String tag;
    private LLM_InputContent command;
    private String rawResponse;
    private String groundTruthAssertions;
    private String augmentedMethod;

    public EvaluationResult(Parser parser) {
        this.parser = parser;
        this.id = counter++;
    }

    public static int getCounter() {
        return counter;
    }

    public int getId() {
        return id;
    }

    public boolean isProductionRecord() {
        return productionRecord;
    }

    public String getClassName() {
        return className;
    }

    public String getName() {
        return name;
    }

    public String getPath() {
        return path;
    }

    public String getRepoPath() {
        return repoPath;
    }

    public String getSignature() {
        return signature;
    }

    public String getRepoName() {
        return repoName;
    }

    public String getPackageName() {
        return packageName;
    }

    public int getStartLine() {
        return startLine;
    }

    public String getMiddleModulePath() {
        return middleModulePath;
    }

    public int getEndLine() {
        return endLine;
    }

    public String getTempRepoPath() {
        return tempRepoPath;
    }

    public boolean isHasGroundTruthAssertions() {
        return hasGroundTruthAssertions;
    }

    public String getLog() {
        return log;
    }

    public String getTag() {
        return tag;
    }

    public String getRawResponse() {
        return rawResponse;
    }

    public boolean isSyntacticallyWrong_predicted() {
        if(predictedAssertionFeatures == null)
            return false;
        for (AssertionFeature feature : predictedAssertionFeatures)
            if (!feature.getSyntacticalCorrectness().isCorrect())
                return true;
        return false;
    }

    public StaticSemanticChecker.FailureType isStaticSemanticallyWrong_predicted() {
        if(predictedAssertionFeatures == null)
            return null;
        for (AssertionFeature feature : predictedAssertionFeatures)
            if (feature.getStaticSemanticalCorrectness() != null && !feature.getStaticSemanticalCorrectness().isCorrect()) {
                return !feature.getStaticSemanticalCorrectness().isReachable() ? StaticSemanticChecker.FailureType.UNREACHABLE_STATEMENT : StaticSemanticChecker.FailureType.OTHER_FAILURE_TYPES;
            }
        return null;
    }

    public boolean isDynamicSemanticallyWrong_predicted() {
        if(predictedAssertionFeatures == null)
            return false;
        for (AssertionFeature feature : predictedAssertionFeatures)
            if (feature.getDynamicSemanticalCorrectness() != null && !feature.getDynamicSemanticalCorrectness().isCorrect()) {
                return true;
            }
        return false;
    }

    public boolean isDynamicSemanticallyUninitialized_predicted() {//due to being syntactically or static semantically wrong
        for (AssertionFeature feature : predictedAssertionFeatures)
            if (feature.getDynamicSemanticalCorrectness() == null) {
                return true;
            }
        return false;
    }

    public String getGroundTruthAssertions() {
        return groundTruthAssertions;
    }

    public void setGroundTruthAssertions(String groundTruthAssertions) {
        this.groundTruthAssertions = groundTruthAssertions;
    }

    public List<AssertionFeature> getGroundTruthAssertionFeatures() {
        return groundTruthAssertionFeatures;
    }

    public void setGroundTruthAssertionFeatures(Parser parser) {
        System.out.println("Setting ground truth assertion features...");
        List<AssertionFeature> assertionFeatures = new LinkedList<>();

        //1. EXTRACT ORIGINAL ASSERTIONS (Note that we have to calculate the relative line number, and also we must consider that while printing the method, the empty linen numbers are removed! so we should first print the method without empty line numbers and then, calculate the line number of the original assertions + considering the curly braces before determining the line numbers
        String tidyMethod = null;
        try {
            MethodDeclaration originalMessyMethod = parser.getRecord().findResolvedMethodDeclaration();//having empty lines, maybe not having curly braces, etc.
            MethodDeclaration havingNonEmptyLinesMessyMethod = Parser.removeEmptyLinesFromMethod(originalMessyMethod);
            tidyMethod = Parser.addCurlyBraces(Parser.printMethod(havingNonEmptyLinesMessyMethod, true, false, true));

            CompilationUnit cu0 = StaticJavaParser.parse("class Temp { " + tidyMethod + " }");

            // Find the method declaration
            MethodDeclaration method = cu0.findFirst(MethodDeclaration.class).get();

            // Iterate through the statements in the method body

            //the followinh approach is wrong since it should calculate the relative line number not absolute
//            method.findAll(AssertStmt.class).forEach(assertStmt -> {
//                assertionFeatures.add(new AssertionFeature(assertStmt, assertStmt.getRange().get().begin.line));
//            });

            //correct approach for relative line number calculation:
            int visitedAssertions = 0;
            for (AssertStmt assertStmt : method.findAll(AssertStmt.class)) {
                visitedAssertions++;
                if(visitedAssertions==1)
                    assertionFeatures.add(new AssertionFeature(assertStmt, assertStmt.getRange().get().begin.line));
                else
                    assertionFeatures.add(new AssertionFeature(assertStmt, assertStmt.getRange().get().begin.line - (visitedAssertions-1)));
            }
            //

        } catch (Exception e) {
            e.printStackTrace();
            assert false;
        }

        //
        for (AssertionFeature originalAssertionFeature : assertionFeatures) {
            try {
                PrettyPrinterConfiguration configuration = new PrettyPrinterConfiguration();
                configuration.setIndentType(Indentation.IndentType.TABS);
                configuration.setIndentSize(1);

                List<AssertionFeature> featureList = new LinkedList<>();
                featureList.add(originalAssertionFeature);

                MethodDeclaration augmentedMethod = StaticJavaParser.parseMethodDeclaration(AssertionFeature.embedAssertionAndReturnMethod(Parser.printMethod(StaticJavaParser.parseMethodDeclaration(tidyMethod), false, true, true), featureList));
                MethodDeclaration augmentedFlaggedMethod = StaticJavaParser.parseMethodDeclaration(DynamicSemanticChecker.addAssertronFlag(augmentedMethod));

                String originalContent = parser.getCompilationUnit(true).toString(configuration);

                Path filePath = Paths.get(parser.getRecord().getFullMethodPath(true));
                CompilationUnit cu = RepoJavaParser.getInstance(Paths.get(parser.getRecord().getTempRepoPath(), parser.getRecord().getMiddleModulePath()).toString()).parse(filePath.toFile()).getResult().get();

                // Find the class in the file
                ClassOrInterfaceDeclaration classDecl = cu.findAll(ClassOrInterfaceDeclaration.class).stream().filter(cls -> cls.getNameAsString().equals(parser.getRecord().getClassName(true))).findFirst().orElseThrow(() -> new RuntimeException("Class not found: " + parser.getRecord().getClassName(false)));

                // Find the method in the class
                classDecl.findAll(MethodDeclaration.class).stream().filter(method -> method.getNameAsString().equals(parser.getRecord().getName()) && method.getDeclarationAsString(true, true, true).trim().equals(parser.getRecord().getSignature())).findFirst().orElseThrow(() -> new RuntimeException("Method not found: " + parser.getRecord().getName())).replace(augmentedFlaggedMethod);

                String newContent = cu.toString(configuration);

                Utils.writeToFile(parser.getRecord().getFullMethodPath(true), newContent);

                //1. replace
                //2. compile temp repo
                //3. revert in temp repo

                try {
                    List<UnitTestExecutionResult> unitTests = new LinkedList<>();
                    System.out.println("Unit tests size: " + parser.getCallerUnitTests().size());
                    for (Pair<CallGraphNode, Boolean> test : parser.getCallerUnitTests()) {
                        System.out.println("Running test: " + test.getFirst().getMethodDeclaration().getName() + " " + (test.getSecond() ? "directly" : "indirectly") + " calling!");
                        UnitTestExecutionResult testResult = DynamicSemanticChecker.runTest(parser, test);
                        System.out.println("Test result: " + (testResult.isPassed() ? "Passed" : "Failed") + " | " + (testResult.isCovered() ? " Covered" : "Not covered"));
                        unitTests.add(testResult);
                    }
                    originalAssertionFeature.setDynamicSemanticCorrect(new DynamicSemanticCorrectObject(unitTests));
                } finally {
                    Utils.writeToFile(parser.getRecord().getFullMethodPath(true), originalContent);
                }
            } catch (Exception e) {
                e.printStackTrace();
                assert false;
            }
        }
        //5. SET ASSERTION FEATURES (only dynamic semantic matters for getting whether they are covered (used in evaluation). syntactic and static semantic will be definitely true even if we were to check.
        this.groundTruthAssertionFeatures = assertionFeatures;
    }

    public Metrics getMetrics() {
        return metrics;
    }

    public void setMetrics(Metrics metrics) {
        this.metrics = metrics;
    }

    public long getGenerationTime() {
        return generationTime;
    }

    public void setGenerationTime(long generationTime) {
        this.generationTime = generationTime;
    }

    public Parser getParser() {
        return parser;
    }

    public void setRawResponse(String rawResponse) {
        this.rawResponse = rawResponse;
    }

    public String getAugmentedMethod() {
        return augmentedMethod;
    }

    public void setAugmentedMethod(String augmentedMethod) {
        this.augmentedMethod = augmentedMethod;
    }

    public LLM_InputContent getCommand() {
        return command;
    }

    public void setCommand(LLM_InputContent command) {
        this.command = command;
    }

    public String getBuildTool() {
        return buildTool;
    }

    public void initializeFromParser(Parser parser) {
        this.className = parser.getRecord().getClassName(false);
        this.name = parser.getRecord().getName();
        this.path = parser.getRecord().getPath();
        this.repoPath = parser.getRecord().getRepoPath();
        this.buildTool = BuildToolModel.hasBuildGradle(repoPath) ? "Gradle" : (BuildToolModel.hasPomXml(repoPath) ? "Maven" : null);
        this.signature = parser.getRecord().getSignature();
        this.repoName = parser.getRecord().getRepoName();
        this.packageName = parser.getRecord().getPackageName();
        this.startLine = parser.getRecord().getStartLine();
        this.endLine = parser.getRecord().getEndLine();
        this.tempRepoPath = parser.getRecord().getTempRepoPath();
        this.middleModulePath = parser.getRecord().getMiddleModulePath();
        this.productionRecord = parser.getRecord().isProductionRecord();
    }

    public void setHasGroundTruthAssertions(boolean hasGroundTruthAssertions) {
        this.hasGroundTruthAssertions = hasGroundTruthAssertions;
    }

    public boolean hasGroundTruthAssertions() {
        return hasGroundTruthAssertions;
    }

    public void setTag(String tag) {
        this.tag = tag;
    }

    public void setLog(String log) {
        this.log = log;

    }

    public List<AssertionFeature> getPredictedAssertionFeatures() {
        return predictedAssertionFeatures;
    }

    public void setPredictedAssertionFeatures(List<AssertionFeature> predictedAssertionFeatures) {
        this.predictedAssertionFeatures = predictedAssertionFeatures == null ? new ArrayList<>() : predictedAssertionFeatures;
//        TestResult testResult = new TestResult();
//        assertionFeatures
//                .stream()
//                .filter(AssertionFeature::isPicked)
//                .forEach(feature -> {
//                    List<UnitTextExecutionResult> unitTextExecutionResults = feature.getDynamicSemanticalCorrectness().getUnitTextExecutionResults();
//                    unitTextExecutionResults
//                            .forEach(result -> {
//                                testResult.increaseTestsRun();
//
//                                if (result.getMessage() != null)
//                                    testResult.increaseErrors();
//
//                                if (result.isPassed())
//                                    testResult.increasePassed();
//                                else
//                                    testResult.increaseFailures();
//                            });
//                });
//
//        setAfterPickTestResult(testResult);
    }

    public List<AssertionFeature> getPickedAssertionFeatures() {
        if(predictedAssertionFeatures==null)
            return new LinkedList<>();
        return predictedAssertionFeatures.stream().filter(AssertionFeature::isPicked).collect(Collectors.toList());
    }

    public int getTotalNumberOfExecutedUnitTests(boolean assertionIsCovered) {
        if (predictedAssertionFeatures == null)
            return 0;

        int counter = 0;
        for (AssertionFeature assertionFeature : predictedAssertionFeatures) {
            if (assertionFeature.getDynamicSemanticalCorrectness() != null)
                counter += assertionFeature.getDynamicSemanticalCorrectness().getNumberOfUniqueTestExecutionResults(assertionIsCovered);
        }
        return counter;
    }

    public int getTotalNumberOfPassedUnitTests(boolean assertionIsCovered) {
        if (predictedAssertionFeatures == null)
            return 0;

        int counter = 0;
        for (AssertionFeature assertionFeature : predictedAssertionFeatures) {
            if (assertionFeature.getDynamicSemanticalCorrectness() != null)
                counter += assertionFeature.getDynamicSemanticalCorrectness().getNumberOfUniquePassedTestExecutionResults(assertionIsCovered);
        }
        return counter;
    }

    public int getTotalNumberOfFailedUnitTests(boolean assertionIsCovered) {
        if (predictedAssertionFeatures == null)
            return 0;

        int counter = 0;
        for (AssertionFeature assertionFeature : predictedAssertionFeatures) {
            if (assertionFeature.getDynamicSemanticalCorrectness() != null)
                counter += assertionFeature.getDynamicSemanticalCorrectness().getNumberOfUniqueFailedTestExecutionResults(assertionIsCovered);
        }
        return counter;
    }

    public int getTotalNumberOfErroneousUnitTests(boolean assertionIsCovered) {
        if (predictedAssertionFeatures == null)
            return 0;

        int counter = 0;
        for (AssertionFeature assertionFeature : predictedAssertionFeatures) {
            if (assertionFeature.getDynamicSemanticalCorrectness() != null)
                counter += assertionFeature.getDynamicSemanticalCorrectness().getNumberOfUniqueErroneousTestExecutionResults(assertionIsCovered);
        }
        return counter;
    }

    public int getTotalNumberOfSyntacticallyIncorrectInferenceRecords() {
        if (predictedAssertionFeatures == null)
            return 0;

        for (AssertionFeature assertionFeature : predictedAssertionFeatures) {
            if (!assertionFeature.getSyntacticalCorrectness().isCorrect())
                return 1;
        }
        return 0;
    }

    public int getTotalNumberOfStaticallySemanticallyIncorrectInferenceRecords() {
        if (predictedAssertionFeatures == null)
            return 0;

        for (AssertionFeature assertionFeature : predictedAssertionFeatures) {
            if (assertionFeature.getSyntacticalCorrectness().isCorrect() &&
                    !assertionFeature.getStaticSemanticalCorrectness().isCorrect())
                return 1;
        }
        return 0;
    }

    public int getTotalNumberOfIndirectDynamicallyIncorrectInferenceRecords() {
        if (predictedAssertionFeatures == null)
            return 0;

        for (AssertionFeature assertionFeature : predictedAssertionFeatures) {
            if (assertionFeature.getSyntacticalCorrectness().isCorrect() &&
                    assertionFeature.getStaticSemanticalCorrectness().isCorrect() &&
                    !assertionFeature.getDynamicSemanticalCorrectness().isCorrect())
                return 1;
        }
        return 0;
    }

    public int getTotalNumberOfStaticallySemanticallyIncorrect_unreachable_InferenceRecords() {
        if (predictedAssertionFeatures == null)
            return 0;

        for (AssertionFeature assertionFeature : predictedAssertionFeatures) {
            if (assertionFeature.getSyntacticalCorrectness().isCorrect() &&
                    !assertionFeature.getStaticSemanticalCorrectness().isCorrect() &&
                    !assertionFeature.getStaticSemanticalCorrectness().isReachable())
                return 1;
        }
        return 0;
    }

    public int getTotalNumberOfStaticallySemanticallyIncorrect_other_failures_InferenceRecords() {
        if (predictedAssertionFeatures == null)
            return 0;

        for (AssertionFeature assertionFeature : predictedAssertionFeatures) {
            if (assertionFeature.getSyntacticalCorrectness().isCorrect() &&
                    !assertionFeature.getStaticSemanticalCorrectness().isCorrect() &&
                    assertionFeature.getStaticSemanticalCorrectness().isReachable())
                return 1;
        }
        return 0;
    }

    public Pair<Pair<Integer, Pair<Integer, Integer>>, Pair<Integer, Pair<Integer, Integer>>> getTestExecutionStatistics() {
        if(predictedAssertionFeatures == null)
            return null;
        //for those assertions that are dynamically something to say!, extract statistics
        List<AssertionFeature> dynamicallyNotNullAssertions = getDynamicallyNotNullAssertions(predictedAssertionFeatures);
        if (dynamicallyNotNullAssertions == null || dynamicallyNotNullAssertions.isEmpty())
            return null;

        int sumOfExecuted_covered = 0;
        int sumOfPassed_covered = 0;
        int sumOfFailed_covered = 0;

        int sumOfExecuted_uncovered = 0;
        int sumOfPassed_uncovered = 0;
        int sumOfFailed_uncovered = 0;


        for (AssertionFeature assertionFeature : dynamicallyNotNullAssertions) {
            List<UnitTestExecutionResult> unitTestExecutionResults = assertionFeature.getDynamicSemanticalCorrectness().getUnitTextExecutionResults();

            for (UnitTestExecutionResult unitTestExecutionResult : unitTestExecutionResults) {
                if (unitTestExecutionResult.isCovered()) {
                    sumOfExecuted_covered++;
                    if (unitTestExecutionResult.isPassed())
                        sumOfPassed_covered++;
                    else
                        sumOfFailed_covered++;
                } else {
                    sumOfExecuted_uncovered++;
                    if (unitTestExecutionResult.isPassed())
                        sumOfPassed_uncovered++;
                    else
                        sumOfFailed_uncovered++;
                }
            }

        }
        return Pair.of(Pair.of(sumOfExecuted_covered, Pair.of(sumOfPassed_covered, sumOfFailed_covered)),
                Pair.of(sumOfExecuted_uncovered, Pair.of(sumOfPassed_uncovered, sumOfFailed_uncovered)));
    }

    private List<AssertionFeature> getDynamicallyNotNullAssertions(List<AssertionFeature> predictedAssertionFeatures) {
        if(predictedAssertionFeatures == null)
            return null;

        return predictedAssertionFeatures
                .stream()
                .filter(assertionFeature -> assertionFeature.getDynamicSemanticalCorrectness() != null)
                .toList();
    }
}
