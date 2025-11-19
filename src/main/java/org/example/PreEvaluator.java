package org.example;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import org.example.call_graph.CallGraphAnalyzer;
import org.example.call_graph.CallGraphNode;

import java.io.File;
import java.nio.file.Paths;
import java.util.List;

public class PreEvaluator {
    public boolean alphaEvaluate(Record record, boolean log) {

        if(log)
            System.out.println("    + Alpha evaluation");

        try {
            // Build the full path to the file
            String fullPath = Paths.get(record.getFullMethodPath()).toString();

            // Check if the file exists
            File file = new File(fullPath);
            if (!file.exists()) {
                System.out.println("File not found: " + fullPath);
                return false;
            }

            // Parse the file with JavaParser
            CompilationUnit cu = StaticJavaParser.parse(file);

            // Check for the class by its name
            String className = record.getClassName(true);

            ClassOrInterfaceDeclaration classDeclaration = cu.findAll(ClassOrInterfaceDeclaration.class).stream().filter(c -> c.getNameAsString().equals(className)).findFirst()
                    .orElse(null);

            if (classDeclaration == null) {
                System.out.println("Class not found: " + record.getClassName(true));
                return false;
            }

            // Check for the method by its name
            MethodDeclaration methodDeclaration = classDeclaration.getMethodsByName(record.getName())
                    .stream()
                    .filter(method -> method.getDeclarationAsString(true, true, true).trim().equals(record.getSignature()))
                    .findFirst()
                    .orElse(null);

            if (methodDeclaration == null) {
                System.out.println("Method not found: " + record.getName() + " with signature " + record.getSignature());
                return false;
            }

            // Check if the method falls within the specified line range
            int methodStartLine = methodDeclaration.getBegin().map(pos -> pos.line).orElse(-1);
            int methodEndLine = methodDeclaration.getEnd().map(pos -> pos.line).orElse(-1);

//            System.out.println("Method range check...");
            return methodStartLine >= record.getStartLine() && methodEndLine <= record.getEndLine();

        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }


    /**
     * Side effect: initializes caller unitTests (for the parser) for the record to which parser points
     *
     * @param parser parser of that record
     * @return if the method has any caller unit tests
     */
    public boolean betaEvaluate(Parser parser, boolean log) throws Exception {
        if(log)
            System.out.println("    + Beta evaluation");
        List<Pair<CallGraphNode, Boolean>> unittestNodes = CallGraphAnalyzer.getTesterNodes(parser.findSelfGraphNodeInCallGraph(), parser.getReverseCallGraph());
        parser.setCallerUnitTests(unittestNodes);
        return !unittestNodes.isEmpty();
    }

    /**
     * ensures that the record is valid eval record (existence) and has unit tests (that are either directly or indirectly calling that record).
     */
    public boolean evaluate(Parser parser, boolean betaTesting, boolean log) throws Exception {
        boolean alpha = alphaEvaluate(parser.getRecord(), log);
        boolean beta = true;
        if(betaTesting)
            beta = betaEvaluate(parser, log);

        if(log) {
            System.out.println("Alpha: " + alpha);
            System.out.println("Beta: " + beta + (!betaTesting ? " (fine-tuning)" : ""));
        }

        if(betaTesting)
            parser.extractUnitTests();

        return alpha && beta;
    }
}
