package org.example;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.stmt.AssertStmt;
import com.github.javaparser.printer.configuration.Indentation;
import com.github.javaparser.printer.configuration.PrettyPrinterConfiguration;
import org.example.call_graph.RepoJavaParser;

import javax.annotation.Nullable;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

public class AssertionFeature {
    private AssertStmt assertion;
    private int lineNumber;
    private SyntacticCorrectObject syntacticallyCorrect;
    private StaticSemanticCorrectObject staticSemanticallyCorrect;
    private DynamicSemanticCorrectObject dynamicSemanticCorrect;
    private Boolean picked;

    public AssertionFeature(AssertStmt assertStmt, int lineNumber) {
        this.assertion = assertStmt;
        this.lineNumber = lineNumber;
    }

    public AssertionFeature(String assertion, int lineNumber, Parser parser) {
        //use the parser to create Assertions in a JavaParser-recognizable mode instead of string in AssertionFeature
        try {
            this.assertion = insertAssertionAndRetrieveIt(assertion, lineNumber, Parser.addCurlyBraces(parser.getRecord().printPureMethod()), parser);
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
        //
        this.lineNumber = lineNumber;
        this.syntacticallyCorrect = null;
        this.staticSemanticallyCorrect = null;
        this.dynamicSemanticCorrect = null;
    }

    public static int getNumberOfAssertionsCoveredByAtLeastOneUnitTest(List<AssertionFeature> assertionFeatures) {
        int counter = 0;

        for (AssertionFeature assertionFeature : assertionFeatures) {
            if (assertionFeature.dynamicSemanticCorrect != null && assertionFeature.dynamicSemanticCorrect.getUnitTextExecutionResults().stream().anyMatch(UnitTestExecutionResult::isCovered))
                counter++;
        }
        return counter;
    }

    /**
     * Puts the assertions in the BEGINNING of the corresponding line, so that everything in that line would be shifted down
     * @param assertionsMap
     * @param prunedMethod
     * @return
     */
    public static String insertAssertions(Map<Integer, List<AssertStmt>> assertionsMap, String prunedMethod) {
        String[] lines = prunedMethod.split("\n");
        StringBuilder modifiedMethod = new StringBuilder();
        for (int lineNumber = 1; lineNumber <= lines.length; lineNumber++) {
            if (assertionsMap.containsKey(lineNumber)) {
                for (AssertStmt assertion : assertionsMap.get(lineNumber)) {
                    modifiedMethod.append(assertion.toString()).append("\n");
                }
            }
            modifiedMethod.append(lines[lineNumber - 1]).append("\n");
        }

        return modifiedMethod.toString();
    }

    public static String embedAssertionAndReturnMethod(String method, List<AssertionFeature> assertionFeatures) throws Exception {
        Map<Integer, List<AssertStmt>> assertionsMap = new HashMap<>();

        for (AssertionFeature feature : assertionFeatures) {
            assertionsMap.computeIfAbsent(feature.getLineNumber(), k -> new LinkedList<>())
                    .add(feature.getAssertion());
        }

        String methodWithCurlyBraces = Parser.addCurlyBraces(method);
        return Parser.addCurlyBraces(insertAssertions(assertionsMap, methodWithCurlyBraces));
    }

    public static double computeNumberOfSyntacticallyCorrectAssertions(List<AssertionFeature> assertionFeatures) {
        return assertionFeatures.stream().filter(feature -> feature.syntacticallyCorrect != null && feature.syntacticallyCorrect.isCorrect()).count();
    }

    public static double computeNumberOfStaticSemanticallyCorrectAssertions(List<AssertionFeature> assertionFeatures) {
        return assertionFeatures.stream().filter(feature -> feature.syntacticallyCorrect != null && feature.staticSemanticallyCorrect != null && feature.syntacticallyCorrect.isCorrect() && feature.staticSemanticallyCorrect.isCorrect()).count();
    }

    public static double computeNumberOfDynamicSemanticallyCorrectAssertions(List<AssertionFeature> assertionFeatures) {
        return assertionFeatures.stream().filter(feature -> feature.syntacticallyCorrect != null && feature.staticSemanticallyCorrect != null && feature.dynamicSemanticCorrect != null && feature.syntacticallyCorrect.isCorrect() && feature.staticSemanticallyCorrect.isCorrect() && feature.dynamicSemanticCorrect.isCorrect()).count();
    }

    public static int getNumberOfTests(List<AssertionFeature> assertionFeatures) {
        return assertionFeatures.stream().filter(feature -> feature.dynamicSemanticCorrect != null)
                .mapToInt(feature -> feature.dynamicSemanticCorrect.getUnitTextExecutionResults().size())
                .sum();
    }

    public static int getNumberOfPassedTests(List<AssertionFeature> assertionFeatures) {
        int counter = 0;
        for (AssertionFeature assertionFeature : assertionFeatures) {
            if (assertionFeature.dynamicSemanticCorrect != null) {
                for (UnitTestExecutionResult unitTestExecutionResult : assertionFeature.dynamicSemanticCorrect.getUnitTextExecutionResults()) {
                    if (unitTestExecutionResult.isPassed())
                        counter++;
                }
            }
        }
        return counter;
    }

    private AssertStmt insertAssertionAndRetrieveIt(String assertionStr, int lineNumberToInsertAt, String prunedMethodWithCurlyBraces, Parser parser) throws Exception {
        String[] lines = prunedMethodWithCurlyBraces.split("\n");
        StringBuilder modifiedMethod = new StringBuilder();
        for (int lineNumber = 1; lineNumber <= lines.length; lineNumber++) {
            if (lineNumber == lineNumberToInsertAt) {
                modifiedMethod.append(assertionStr).append("\n");
            }
            modifiedMethod.append(lines[lineNumber - 1]).append("\n");
        }

        String methodWithAssertionInserted = modifiedMethod.toString();
        MethodDeclaration methodDeclarationWithAssertionInserted = StaticJavaParser.parseMethodDeclaration(methodWithAssertionInserted);

        PrettyPrinterConfiguration configuration = new PrettyPrinterConfiguration();
        configuration.setIndentType(Indentation.IndentType.TABS);
        configuration.setIndentSize(1);

        String originalContent = parser.getCompilationUnit(true).toString(configuration);

        Path filePath = Paths.get(parser.getRecord().getFullMethodPath(true));
        CompilationUnit cu = RepoJavaParser.getInstance(Paths.get(parser.getRecord().getTempRepoPath(), parser.getRecord().getMiddleModulePath()).toString()).parse(filePath.toFile()).getResult().get();

//            // Find the class in the file
        ClassOrInterfaceDeclaration classDecl = cu.findAll(ClassOrInterfaceDeclaration.class).stream().filter(cls -> cls.getNameAsString().equals(parser.getRecord().getClassName(true))).findFirst().orElseThrow(() -> new RuntimeException("Class not found: " + parser.getRecord().getClassName(false)));

//            // Find the method in the class
        classDecl.findAll(MethodDeclaration.class).stream().filter(method -> method.getNameAsString().equals(parser.getRecord().getName()) && method.getDeclarationAsString(true, true, true).trim().equals(parser.getRecord().getSignature())).findFirst().orElseThrow(() -> new RuntimeException("Method not found: " + parser.getRecord().getName())).replace(methodDeclarationWithAssertionInserted);

        String newContent = cu.toString(configuration);
        Utils.writeToFile(parser.getRecord().getFullMethodPath(true), newContent);
        try {
            MethodDeclaration methodDeclaration = classDecl.findAll(MethodDeclaration.class).stream().filter(method -> method.getNameAsString().equals(parser.getRecord().getName()) && method.getDeclarationAsString(true, true, true).trim().equals(parser.getRecord().getSignature())).findFirst().orElseThrow(() -> new RuntimeException("Method not found: " + parser.getRecord().getName()));
            return methodDeclaration.findAll(AssertStmt.class).stream().findFirst().orElseThrow(() -> new RuntimeException("Assertion not found during retrieval: " + parser.getRecord().getName()));
        } finally {
            Utils.writeToFile(parser.getRecord().getFullMethodPath(true), originalContent);
        }
    }

    public Boolean isPicked() {
        return picked;
    }

    public void setPicked(Boolean picked) {
        this.picked = picked;
    }

    public String embedAssertionAndReturnMethod(Record record) throws Exception {
        Map<Integer, List<AssertStmt>> assertionsMap = new HashMap<>();
        List<AssertStmt> assertions = new LinkedList<>();
        System.out.println("Adding assertion: " + assertion);
        assertions.add(assertion);
        assertionsMap.put(lineNumber, assertions);
        String methodWithCurlyBraces = record.printPureMethod();

        return Parser.addCurlyBraces(insertAssertions(assertionsMap, methodWithCurlyBraces));
    }

    public int getLineNumber() {
        return lineNumber;
    }

    public DynamicSemanticCorrectObject getDynamicSemanticalCorrectness() {
        return dynamicSemanticCorrect;
    }

    public void setDynamicSemanticCorrect(DynamicSemanticCorrectObject dynamicSemanticCorrect) {
        this.dynamicSemanticCorrect = dynamicSemanticCorrect;
    }

    public StaticSemanticCorrectObject getStaticSemanticalCorrectness() {
        return staticSemanticallyCorrect;
    }

    public void setStaticSemanticallyCorrect(@Nullable StaticSemanticCorrectObject staticSemanticallyCorrect) {
        this.staticSemanticallyCorrect = staticSemanticallyCorrect;
    }

    public SyntacticCorrectObject getSyntacticalCorrectness() {
        return syntacticallyCorrect;
    }

    public void setSyntacticallyCorrect(SyntacticCorrectObject syntacticallyCorrect) {
        this.syntacticallyCorrect = syntacticallyCorrect;
    }

    public AssertStmt getAssertion() {
        return assertion;
    }

    public void setAssertion(AssertStmt assertion) {
        this.assertion = assertion;
    }

    public boolean isImperfect() {
        SyntacticCorrectObject syntacticCorrect = getSyntacticalCorrectness();
        StaticSemanticCorrectObject staticSemanticalCorrectness = getStaticSemanticalCorrectness();
        DynamicSemanticCorrectObject dynamicSemanticalCorrectness = getDynamicSemanticalCorrectness();
        return syntacticCorrect == null || staticSemanticalCorrectness == null || dynamicSemanticalCorrectness == null ||
                !syntacticCorrect.isCorrect() || !staticSemanticalCorrectness.isCorrect() || !dynamicSemanticalCorrectness.isCorrect();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AssertionFeature that = (AssertionFeature) o;
        return lineNumber == that.lineNumber && Objects.equals(assertion, that.assertion);
    }

    @Override
    public int hashCode() {
        return Objects.hash(assertion, lineNumber);
    }
}