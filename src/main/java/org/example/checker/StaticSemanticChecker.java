package org.example.checker;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.printer.configuration.Indentation;
import com.github.javaparser.printer.configuration.PrettyPrinterConfiguration;
import org.example.*;
import org.example.call_graph.RepoJavaParser;

import java.nio.file.Path;
import java.nio.file.Paths;

import static org.example.Constants.REPO_JAVA_ADDRESS_MAP;

public class StaticSemanticChecker implements Checker {
    private static StaticSemanticCorrectObject isStaticSemanticallyCorrect(AssertionFeature assertionFeature, Parser parser) {
        try {
            PrettyPrinterConfiguration configuration = new PrettyPrinterConfiguration();
            configuration.setIndentType(Indentation.IndentType.TABS);
            configuration.setIndentSize(1);
            MethodDeclaration augmentedMethod = StaticJavaParser.parseMethodDeclaration(assertionFeature.embedAssertionAndReturnMethod(parser.getRecord()));
            String originalContent = parser.getCompilationUnit(true).toString(configuration);


            Path filePathInTemp = Paths.get(parser.getRecord().getFullMethodPath(true));
            CompilationUnit cu = RepoJavaParser.getInstance(Paths.get(parser.getRecord().getTempRepoPath(), parser.getRecord().getMiddleModulePath()).toString()).parse(filePathInTemp.toFile()).getResult().get();

//            // Find the class in the file
            ClassOrInterfaceDeclaration classDecl = cu.findAll(ClassOrInterfaceDeclaration.class).stream().filter(cls -> cls.getNameAsString().equals(parser.getRecord().getClassName(true))).findFirst().orElseThrow(() -> new RuntimeException("Class not found: " + parser.getRecord().getClassName(false)));

//            // Find the method in the class
            classDecl.findAll(MethodDeclaration.class).stream().filter(method -> method.getNameAsString().equals(parser.getRecord().getName()) && method.getDeclarationAsString(true, true, true).trim().equals(parser.getRecord().getSignature())).findFirst().orElseThrow(() -> new RuntimeException("Method not found: " + parser.getRecord().getName())).replace(augmentedMethod);

            String newContent = cu.toString(configuration);

            //1. replace in temp repo
            //2. compile temp repo
            //3. revert in temp repo

            Utils.writeToFile(parser.getRecord().getFullMethodPath(true), newContent);
            try {
                ComponentResponse componentResponse = compile(parser);
                if (componentResponse.isOK())
                    return new StaticSemanticCorrectObject(componentResponse, null, true, true);
                else {
                    if (isDueToUnreachableStatement(componentResponse.getMessage()))
                        return new StaticSemanticCorrectObject(componentResponse, "COMPILATION FAILURE - UNREACHABLE STATEMENT", false, false);
                    return new StaticSemanticCorrectObject(componentResponse, "COMPILATION FAILURE - OTHER COMPILATION FAILURES", false, true);
                }
            } finally {
                Utils.writeToFile(parser.getRecord().getFullMethodPath(true), originalContent);
            }
        } catch (Exception e) {
            return new StaticSemanticCorrectObject(null, "COMPILATION FAILURE - EXCEPTION - " + e.getMessage(), false, false);
        }
    }

    private static boolean isDueToUnreachableStatement(String message) {
        if (message == null)
            return false;

        return message.toLowerCase().contains("unreachable statement");
    }

    private static ComponentResponse compile(Parser parser) {
        BuildToolModel buildToolModel = BuildToolModel.getProjectBuildToolModel(parser.getRecord().getTempRepoPath());
        if (buildToolModel != null) {
            try {
                return buildToolModel.compile(true, REPO_JAVA_ADDRESS_MAP.get(parser.getRecord().getRepoPath()));
            } catch (Exception e) {
                return new ComponentResponse(ComponentResponse.Status.EXCEPTION_OCCURRED, e.getMessage(), null, null);
            }
        } else {
            return new ComponentResponse(ComponentResponse.Status.ERROR_OCCURRED, "Project build tool is not supported by the tool", null, null);
        }
    }

    //For now, just compilation and reachability
    @Override
    public void check(Parser parser, AssertionFeatureMap assertionFeatureMap) {
        assertionFeatureMap.getAssertionFeatures().forEach(assertionFeature -> {
            System.out.println("    Assertion under static semantic check: " + assertionFeature.getAssertion() + " | Line number: " + assertionFeature.getLineNumber());

            if (!assertionFeature.getSyntacticalCorrectness().isCorrect()) {
                assertionFeature.setStaticSemanticallyCorrect(null);
                assertionFeature.setDynamicSemanticCorrect(null);
            } else {
                assertionFeature.setStaticSemanticallyCorrect(isStaticSemanticallyCorrect(assertionFeature, parser));
            }
        });
    }

    public static enum FailureType {
        UNREACHABLE_STATEMENT, OTHER_FAILURE_TYPES
    }
}
