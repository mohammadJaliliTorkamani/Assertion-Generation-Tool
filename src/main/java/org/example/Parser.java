package org.example;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.TypeDeclaration;
import com.github.javaparser.ast.stmt.AssertStmt;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.stmt.Statement;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;
import com.github.javaparser.javadoc.Javadoc;
import com.github.javaparser.printer.PrettyPrinter;
import com.github.javaparser.printer.configuration.PrettyPrinterConfiguration;
import org.example.call_graph.CallGraphAnalyzer;
import org.example.call_graph.CallGraphNode;
import org.example.call_graph.RepoJavaParser;

import java.io.File;
import java.io.FileNotFoundException;
import java.nio.file.Paths;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

public class Parser {
    private final Record record;
    private Set<Pair<CallGraphNode, Boolean>> callerUnitTests;
    private List<CallGraphNode> callGraph;
    private List<CallGraphNode> reverseCallGraph;
    private CallGraphNode selfNode;

    public Parser(Record record) {
        this.record = record;
        this.selfNode = null;
    }

    public static String getCommentLessAndJavadocLessMethodDeclaration(MethodDeclaration methodWithoutAssertions) throws Exception {
        return printMethod(methodWithoutAssertions, true, false, true);
    }

    public static String printMethod(MethodDeclaration methodDeclaration, boolean excludeComments, boolean excludeAssertions, boolean excludeJavadoc) throws Exception {
        MethodDeclaration _methodDeclaration = methodDeclaration.clone();

        if (excludeAssertions) {
            _methodDeclaration.accept(new AssertionRemovalVisitor(), null);
//            try (CompilationUnitWrapper cuw = new CompilationUnitWrapper(javaPath)) {
//                visitor.visit(cuw.getCompilationUnit(), null);
//                System.out.println("WWW:"+cuw.getCompilationUnit());
//            }
        }

        PrettyPrinterConfiguration configuration = new PrettyPrinterConfiguration();
        /*
         * seems to be corrupted in case of setPrintComments:false,setPrintJavadoc:true, so we come up
         * with manually printing/not printing javadocs
         */
        configuration.setPrintJavadoc(false);
        configuration.setPrintComments(!excludeComments);

        String javadoc = "";
        if (!excludeJavadoc) {
            Javadoc _javadoc = extractJavadoc(_methodDeclaration);
            if (_javadoc != null) javadoc = _javadoc.toComment().toString();
        }

        PrettyPrinter prettyPrinter = new PrettyPrinter(configuration);
        return javadoc + prettyPrinter.print(_methodDeclaration);
    }

    public static Javadoc extractJavadoc(MethodDeclaration methodDeclaration) throws Exception {
        return methodDeclaration.getJavadoc().orElse(null);
    }

    public static String addCurlyBraces(String methodCode) throws Exception {
        JavaParser parser = new JavaParser();
        CompilationUnit cu = parser.parse("class TempClass { " + methodCode + " }").getResult().orElse(null);

        if (cu == null) {
            throw new IllegalArgumentException("Failed to parse the method.");
        }

        MethodDeclaration method = null;

        for (TypeDeclaration<?> type : cu.getTypes()) {
            for (MethodDeclaration methodDeclaration : type.getMethods()) {
                method = methodDeclaration;
                method.accept(new AddCurlyBracesVisitor(), null);
            }
        }

        if (method == null) {
            throw new IllegalArgumentException("No method found in the provided code.");
        }
        PrettyPrinterConfiguration config = new PrettyPrinterConfiguration();
        config.setIndentSize(4);
        return method.toString(config);
    }

    public static MethodDeclaration removeEmptyLinesFromMethod(MethodDeclaration method) {
        // Step 1: Get the method body
        BlockStmt body = method.getBody().orElse(null);

        if (body != null) {
            // Step 2: Get the list of statements (removes empty lines)
            List<Statement> statements = body.getStatements();

            // Step 3: Filter out statements that are essentially empty (e.g., empty blocks)
            List<Statement> nonEmptyStatements = statements.stream()
                    .filter(stmt -> !stmt.toString().trim().isBlank())  // Filter out empty statements
                    .toList();

            // Step 4: Create a new MethodDeclaration with the filtered body
            MethodDeclaration newMethod = method.clone(); // Clone the original method to preserve the other properties

            // Replace the old body with the non-empty statements
            NodeList<Statement> nodeList = new NodeList<>();
            nodeList.addAll(nonEmptyStatements);
            newMethod.setBody(new BlockStmt(nodeList));

            return newMethod;
        } else {
            return method;  // If no body exists, return the original method (nothing to remove)
        }
    }

    public Record getRecord() {
        return record;
    }

    public List<CallGraphNode> getCallGraph() {
        return callGraph;
    }

    public List<CallGraphNode> getReverseCallGraph() {
        return reverseCallGraph;
    }

    public boolean hasCallGraph() {
        return callGraph != null;
    }

    public CallGraphNode computeCallGraph(boolean log) {
        CallGraphNode callGraphNode = null;
        if (this.callGraph == null) {
            try {
                if (log)
                    System.out.println("        Computing call graph...");

                System.out.println("        Building graph");
                this.callGraph = CallGraphAnalyzer.buildGraph(record.getRepoPath(), record.getMiddleModulePath());

                if (callGraph == null) {
                    throw new Exception("NULL OUTPUT WHEN COMPUTING CALL GRAPH...");
                } else {
                    System.out.println("        Building reversed graph");
                    this.reverseCallGraph = CallGraphAnalyzer.buildReversedGraph(this.callGraph);
                    System.out.println("        Find self graph node");
                    callGraphNode = findSelfGraphNodeInCallGraph();
                    record.setProductionRecord(callGraphNode.isOfProduction());
                }
            } catch (Exception e) {
                System.err.println("Error while computing call graph:" + e.getMessage());
            }
        }
        return callGraphNode;
    }

    public CompilationUnit getCompilationUnit(boolean tmpRepo) {
        String repoPath = !tmpRepo ? getRecord().getRepoPath() : getRecord().getTempRepoPath();
        File toParseFile = new File(record.getFullMethodPath(tmpRepo));

        try {
            return new RepoJavaParser().getInstance(Paths.get(repoPath, getRecord().getMiddleModulePath()).toString()).parse(toParseFile).getResult().orElse(null);
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    public boolean canReach(CallGraphNode nodeA, CallGraphNode nodeB) {
        assert this.callGraph != null;
        return CallGraphAnalyzer.canReach(callGraph, nodeA, nodeB);
    }

    public boolean containsAssertion() {
        AtomicBoolean containsAssert = new AtomicBoolean(false);
        // Traverse the method body to find AssertStmt
        record.findResolvedMethodDeclaration().accept(new VoidVisitorAdapter<Void>() {
            @Override
            public void visit(AssertStmt assertStmt, Void arg) {
                super.visit(assertStmt, arg);
                containsAssert.set(true);
            }
        }, null);

        return containsAssert.get();
    }

    public Set<Pair<CallGraphNode, Boolean>> getCallerUnitTests() {
        return callerUnitTests;
    }

    public void setCallerUnitTests(List<Pair<CallGraphNode, Boolean>> callerUnitTests) {
        if (callerUnitTests != null)
            this.callerUnitTests = callerUnitTests.stream()
                    .limit(Constants.MAXIMUM_NUMBER_OF_UNIT_TESTS_PER_RECORD)
                    .collect(Collectors.toSet());
        else
            this.callerUnitTests = null;
    }

    public void extractUnitTests() {
        callerUnitTests
                .stream()
                .limit(Constants.MAXIMUM_NUMBER_OF_UNIT_TESTS_PER_RECORD)
                .map(Pair::getFirst)
                .forEach(callGraphNode -> {
                    Record testRecord = new Record(false);
                    int startLine = callGraphNode.getMethodDeclaration().getBegin().map(pos -> pos.line).orElse(-1);
                    int endLine = callGraphNode.getMethodDeclaration().getEnd().map(pos -> pos.line).orElse(-1);

                    testRecord.setClassName(callGraphNode.getMethodDeclaration().resolve().getClassName());
                    testRecord.setName(callGraphNode.getMethodDeclaration().getNameAsString());
                    testRecord.setPath(callGraphNode.getPath().toString());
                    testRecord.setStartLine(startLine);
                    testRecord.setEndLine(endLine);
                    testRecord.setPackageName(callGraphNode.getMethodDeclaration().resolve().getPackageName());
                    testRecord.setSignature(callGraphNode.getMethodDeclaration().getDeclarationAsString(true, true, true).trim());
                    testRecord.setRepoPath(callGraphNode.getRepoPath().toString());

                    record.addUnitTest(testRecord);
                });
    }

    public CallGraphNode findSelfGraphNodeInCallGraph() {
        if (selfNode == null) {
            for (CallGraphNode node : callGraph) {
                if (Paths.get(record.getFullMethodPath()).toString().equals(node.getPath().toString()) &&
                        record.getSignature().equals(node.getMethodDeclaration().getDeclarationAsString(true, true, true).trim())) {
                    this.selfNode = node;
                    return node;
                }
            }
            this.selfNode = null;
        }
        return this.selfNode;
    }

    public String getMethodWithAssertionsEmbedded(List<AssertionFeature> assertions) throws Exception {
        String newMethod = this.record.printPureMethod();

        return AssertionFeature.embedAssertionAndReturnMethod(newMethod, assertions);
    }
}
