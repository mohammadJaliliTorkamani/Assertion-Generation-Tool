package org.example.checker;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.expr.StringLiteralExpr;
import com.github.javaparser.ast.stmt.AssertStmt;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.stmt.ExpressionStmt;
import com.github.javaparser.printer.configuration.Indentation;
import com.github.javaparser.printer.configuration.PrettyPrinterConfiguration;
import org.example.*;
import org.example.call_graph.CallGraphNode;
import org.example.call_graph.RepoJavaParser;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedList;
import java.util.List;

import static org.example.Constants.REPO_JAVA_ADDRESS_MAP;

public class DynamicSemanticChecker implements Checker {

    public static String addAssertronFlag(MethodDeclaration augmentedMethod) throws Exception {
        MethodDeclaration _methodDeclaration = StaticJavaParser.parseMethodDeclaration(Parser.addCurlyBraces(augmentedMethod.toString()));
        // Check for assert statements in the method
        _methodDeclaration.findAll(AssertStmt.class).forEach(assertStmt -> {
            NodeList nodeList = new NodeList();
            nodeList.add(new StringLiteralExpr(Constants.ASSERTRON_COVERAGE_FLAG_MESSAGE));
            // Create a System.out.println statement before the assert
            ExpressionStmt soutStmt = new ExpressionStmt(
                    new MethodCallExpr(
                            new NameExpr("System.out"), // Access "System.out"
                            "println", // Call "println"
                            nodeList// Message to print
                    )
            );

            // Insert the System.out.println before the assert statement
            assertStmt.getParentNode().ifPresent(parent -> {
                if (parent instanceof BlockStmt) {
                    BlockStmt blockStmt = (BlockStmt) parent;
                    blockStmt.addStatement(blockStmt.getStatements().indexOf(assertStmt), soutStmt);
                }
            });
        });

        // Return the modified method as a string
        return _methodDeclaration.toString();
    }

    public static UnitTestExecutionResult runTest(Parser parser, Pair<CallGraphNode, Boolean> testNode) {
        BuildToolModel buildToolModel = BuildToolModel.getProjectBuildToolModel(parser.getRecord().getTempRepoPath());
        if (buildToolModel != null) {
            try {
                ComponentResponse componentResponse = buildToolModel.runTestCase(testNode.getFirst(), parser.findSelfGraphNodeInCallGraph().getMiddleModulePath().replace(File.separator, "."), REPO_JAVA_ADDRESS_MAP.getOrDefault(parser.getRecord().getRepoPath(), Constants.JAVA_HOME_VERSIONS[0]));
                return new UnitTestExecutionResult(componentResponse, null, testNode.getFirst(), testNode.getSecond());
            } catch (Exception e) {
                return new UnitTestExecutionResult(null, e.getMessage(), testNode.getFirst(), testNode.getSecond());
            }
        } else {
            return new UnitTestExecutionResult(null, "Project build tool is not supported by the tool so the test did not executed at all.", testNode.getFirst(), testNode.getSecond());
        }
    }

    @Override
    public void check(Parser parser, AssertionFeatureMap assertionFeatureMap) {
        assertionFeatureMap.getAssertionFeatures().forEach(assertionFeature -> {
            if (!assertionFeature.getSyntacticalCorrectness().isCorrect() || assertionFeature.getStaticSemanticalCorrectness() == null || !assertionFeature.getStaticSemanticalCorrectness().isCorrect()) {
                assertionFeature.setDynamicSemanticCorrect(null);
            } else
                assertionFeature.setDynamicSemanticCorrect(isDynamicallySemanticallyCorrect(assertionFeature, parser));
        });
    }

    private DynamicSemanticCorrectObject isDynamicallySemanticallyCorrect(AssertionFeature assertionFeature, Parser parser) {
        try {
            PrettyPrinterConfiguration configuration = new PrettyPrinterConfiguration();
            configuration.setIndentType(Indentation.IndentType.TABS);
            configuration.setIndentSize(1);
            MethodDeclaration augmentedMethod = StaticJavaParser.parseMethodDeclaration(assertionFeature.embedAssertionAndReturnMethod(parser.getRecord()));
            MethodDeclaration augmentedFlaggedMethod = StaticJavaParser.parseMethodDeclaration(addAssertronFlag(augmentedMethod));

            String originalContent = parser.getCompilationUnit(true).toString(configuration);


            Path filePath = Paths.get(parser.getRecord().getFullMethodPath(true));
            CompilationUnit cu = RepoJavaParser.getInstance(Paths.get(parser.getRecord().getTempRepoPath(), parser.getRecord().getMiddleModulePath()).toString()).parse(filePath.toFile()).getResult().get();

//            // Find the class in the file
            ClassOrInterfaceDeclaration classDecl = cu.findAll(ClassOrInterfaceDeclaration.class).stream().filter(cls -> cls.getNameAsString().equals(parser.getRecord().getClassName(true))).findFirst().orElseThrow(() -> new RuntimeException("Class not found: " + parser.getRecord().getClassName(false)));

//            // Find the method in the class
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
                    UnitTestExecutionResult testResult = runTest(parser, test);
                    System.out.println("Test result: " + (testResult.isPassed() ? "Passed" : "Failed") + " | " + (testResult.isCovered() ? " Covered" : "Not covered"));
                    unitTests.add(testResult);
                }

                return new DynamicSemanticCorrectObject(unitTests);
            } finally {
                Utils.writeToFile(parser.getRecord().getFullMethodPath(true), originalContent);
            }

        } catch (Exception e) {
            return new DynamicSemanticCorrectObject();
        }
    }
}