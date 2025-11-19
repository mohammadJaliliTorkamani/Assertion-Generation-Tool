package org.example.call_graph;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ParseResult;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.stmt.AssertStmt;
import com.github.javaparser.resolution.declarations.ResolvedMethodDeclaration;
import com.github.javaparser.symbolsolver.javaparsermodel.declarations.JavaParserMethodDeclaration;
import org.example.Constants;
import org.example.Pair;
import org.example.Record;
import org.example.Utils;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

public class CallGraphAnalyzer {
    private final static Map<String,List<CallGraphNode>> callGraphCache = new HashMap<>();
    /**
     * This method considers an upper bound to the tester nodes to make method efficient.
     * This method first finds all methods (either test or production) that are either directly or indirectly connected
     * to the targetNode node. Then separates the testers, and to mke things easier during pipeline operation, limits the
     * number of testers to `MAXIMUM_NUMBER_OF_UNIT_TESTS_PER_RECORD' defined in Constants.java.
     * the second parameter of the Pair is related to whether or not the node is immediately connected to the target node in the reversed graph.
     * Note that it takes the reverse graph. so passing normal graph results in a wrong result
     *
     * @param targetNode
     * @param reverseGraph
     * @return
     * @throws Exception
     */
    public static List<Pair<CallGraphNode, Boolean>> getTesterNodes(CallGraphNode targetNode, List<CallGraphNode> reverseGraph) throws Exception {
        List<CallGraphNode> connectedNodes = new ArrayList<>();
        Queue<CallGraphNode> queue = new LinkedList<>();

        // Start BFS from the target node.
        CallGraphNode targetInReverseGraph = getFromReverseCallGraph(targetNode.getMethodDeclaration(), reverseGraph);

        queue.add(targetInReverseGraph);

        while (!queue.isEmpty()) {
            CallGraphNode current = queue.poll();

            if (!connectedNodes.contains(current)) {
                connectedNodes.add(current);
            }

            for (MethodDeclaration callee : current.getChildren()) {
                CallGraphNode calleeNode = getFromReverseCallGraph(callee, reverseGraph);

                if (!connectedNodes.contains(calleeNode))
                    queue.add(calleeNode);
            }
        }

        connectedNodes.remove(targetNode);

        List<CallGraphNode> immediateNodes = targetInReverseGraph.getChildren().stream().map(methodDeclaration -> {
            try {
                CallGraphNode inReverseNode = getFromReverseCallGraph(methodDeclaration, reverseGraph);
                return new CallGraphNode(methodDeclaration, inReverseNode.getPath(), inReverseNode.getRepoPath(), inReverseNode.getMiddleModulePath(), inReverseNode.hasAnyAssertion());
            } catch (Exception e) {
                e.printStackTrace();
                return null;
            }
        }).filter(Objects::nonNull).toList();

//        System.out.println("\nTarget node: " + targetNode.getMethodDeclaration().getName());
//        System.out.println("Immediate nodes: ");
//        for(CallGraphNode node : immediateNodes) {
//            System.out.println("    "+node.getMethodDeclaration().getName());
//        }

//        System.out.println();
//        System.out.println("Connected nodes: ");
//        for(CallGraphNode node : connectedNodes) {
//            System.out.println("    "+node.getMethodDeclaration().getName());
//        }

        return connectedNodes
                .stream()
                .map(callGraphNode -> {
//                    System.out.println(callGraphNode.getMethodDeclaration().getName()+" "+immediateNodes.contains(callGraphNode));
//                    for(CallGraphNode node : immediateNodes) {
//                        System.out.println("    "+node.getMethodDeclaration().getName()+" "+node.getMethodDeclaration().getBody());
//                        System.out.println("######");
//                    }
                    return Pair.of(callGraphNode, immediateNodes.contains(callGraphNode));
                })
                .filter(callGraphNodeBooleanPair -> callGraphNodeBooleanPair.getFirst().isOfTest())
                .limit(Constants.MAXIMUM_NUMBER_OF_UNIT_TESTS_PER_RECORD)
                .toList();
    }

    public static List<CallGraphNode> buildGraph(String baseRepoPath, String middleModuleName) throws Exception {
        if(callGraphCache.containsKey(baseRepoPath+middleModuleName))
            return callGraphCache.get(baseRepoPath+middleModuleName);

        String repoPath = Paths.get(baseRepoPath, middleModuleName).toAbsolutePath().toString();
        repoPath = repoPath.replace("/", File.separator);
        List<CallGraphNode> callGraph = new LinkedList<>();

        // Set up type solver and symbol resolver
        JavaParser javaParser = new RepoJavaParser().getInstance(repoPath);

        // Parse all Java files in the repository
        Path repoPathObj = Paths.get(repoPath);

        try {
            //old version (working but too slow)
            Files.walk(repoPathObj)
                    .filter(Files::isRegularFile)
                    .filter(path -> path.toString().endsWith(".java"))
                    .forEach(path -> {
                        try {
                            ParseResult<CompilationUnit> parseResult = javaParser.parse(path);
                            if (parseResult.isSuccessful()) {
                                if (parseResult.getResult().isPresent()) {
                                    CompilationUnit cu = parseResult.getResult().get();
                                    cu.findAll(MethodDeclaration.class).forEach(method -> {
                                        Optional<ClassOrInterfaceDeclaration> classOrInterfaceDeclaration = method.findAncestor(ClassOrInterfaceDeclaration.class);
                                        if (classOrInterfaceDeclaration.isPresent() && !classOrInterfaceDeclaration.get().isInterface()) {
                                            boolean hasAssertion = !method.findAll(AssertStmt.class).isEmpty();
                                            CallGraphNode node = new CallGraphNode(method, path, Paths.get(baseRepoPath), middleModuleName, hasAssertion);
                                            callGraph.add(node);
                                            if (callGraph.size() > Constants.MAX_CALL_GRAPH_NODES_SIZE)
                                                throw new RuntimeException("CALL GRAPH SIZE EXCEEDED");
                                        }
                                    });
                                }
                            }
                        } catch (RuntimeException e) {
                            if (e.getMessage().equals("CALL GRAPH SIZE EXCEEDED"))
                                throw e;
                        } catch (Exception e) {
                            System.err.println("Error parsing file: " + path);
                            e.printStackTrace();
                        }
                    });
            //parallel version
//            List<Path> javaFiles;
//            try (Stream<Path> paths = Files.walk(repoPathObj)) {
//                javaFiles = paths
//                        .filter(Files::isRegularFile)
//                        .filter(path -> path.toString().endsWith(".java"))
//                        .toList();
//            }
//            javaFiles.parallelStream().forEach(path -> {
//                System.out.println("Hi");
//                try {
//                    JavaParser localParser = new JavaParser();  // 👈 create a new one per thread
//                    String content = Files.readString(path);
//                    ParseResult<CompilationUnit> parseResult = RepoJavaParser.getInstance(path.toString()).parse(content);
//                    if (parseResult.isSuccessful() && parseResult.getResult().isPresent()) {
//                        System.out.println("BBB");
//                        CompilationUnit cu = parseResult.getResult().get();
//                        cu.findAll(MethodDeclaration.class).forEach(method -> {
//                            System.out.println("Method");
//                            Optional<ClassOrInterfaceDeclaration> classOrInterfaceDeclaration = method.findAncestor(ClassOrInterfaceDeclaration.class);
//                            if (classOrInterfaceDeclaration.isPresent() && !classOrInterfaceDeclaration.get().isInterface()) {
//                                boolean hasAssertion = !method.findAll(AssertStmt.class).isEmpty();
//                                CallGraphNode node = new CallGraphNode(method, path, Paths.get(baseRepoPath), middleModuleName, hasAssertion);
//                                synchronized (callGraph) {
//                                    callGraph.add(node);
//                                    if (callGraph.size() > Constants.MAX_CALL_GRAPH_NODES_SIZE) {
//                                        throw new RuntimeException("CALL GRAPH SIZE EXCEEDED");
//                                    }
//                                }
//                            }
//                        });
//                    }
//                } catch (RuntimeException e) {
//                    if ("CALL GRAPH SIZE EXCEEDED".equals(e.getMessage())) {
//                        throw e;
//                    }
//                } catch (Exception e) {
//                    System.err.println("Error parsing file: " + path);
//                    e.printStackTrace();
//                }
//            });

        } catch (RuntimeException e) {
            if (!e.getMessage().equals("CALL GRAPH SIZE EXCEEDED"))
                throw e;
            else
                return null;
        }

        callGraph
                .forEach(callGraphNode -> {
                    MethodDeclaration method = callGraphNode.getMethodDeclaration();
                    List<MethodDeclaration> resolvableChildren = new ArrayList<>();
                    method.findAll(MethodCallExpr.class).forEach(call -> {
                        try {
                            ResolvedMethodDeclaration resolvedMethodDeclaration = call.resolve();
                            if (resolvedMethodDeclaration instanceof JavaParserMethodDeclaration) { //if defined inside the project
                                if (!((JavaParserMethodDeclaration) resolvedMethodDeclaration).getWrappedNode().getBody().isEmpty()) {
                                    MethodDeclaration methodDeclaration = ((JavaParserMethodDeclaration) resolvedMethodDeclaration).getWrappedNode();
                                    for (CallGraphNode candidateChildNode : callGraph)
                                        if (candidateChildNode.hashCode() == new CallGraphNode(methodDeclaration, null, null, middleModuleName, false).hashCode()) {
                                            resolvableChildren.add(candidateChildNode.getMethodDeclaration());
                                        }
                                }
                            }

                        } catch (Exception e) {
                        }
                    });
                    callGraphNode.setChildren(resolvableChildren);
                });

        callGraphCache.put(baseRepoPath+middleModuleName, callGraph);
        return callGraph;
    }

    public static boolean canReach(List<CallGraphNode> callGraph, CallGraphNode nodeA, CallGraphNode nodeB) {
        return canReachOneWay(callGraph, nodeB, nodeA) || canReachOneWay(callGraph, nodeA, nodeB);
    }

    public static boolean canReachOneWay(List<CallGraphNode> callGraph, CallGraphNode startNode, CallGraphNode targetNode) {
        // Track visited nodes to avoid revisiting them
        Set<Integer> visited = new HashSet<>();

        if (startNode == null || targetNode == null || startNode.getMethodDeclaration() == null || targetNode.getMethodDeclaration() == null || callGraph == null || callGraph.isEmpty())
            return false;

        // Initialize the queue for BFS traversal
        Stack<CallGraphNode> queue = new Stack<>();
        queue.push(startNode);

        while (!queue.isEmpty()) {

            CallGraphNode currentNode = queue.pop();

            if (currentNode.hashCode() == targetNode.hashCode())
                return true;

            visited.add(currentNode.hashCode());
            for (MethodDeclaration childCall : currentNode.getChildren()) {
                if (!visited.contains(new CallGraphNode(childCall, null, null, null, false).hashCode())) {
                    queue.add(extractCallGraphNodeOfMethodDeclarationFromCallGraph(callGraph, childCall));
                }
            }
        }

        // If the queue is empty and the target node was not found, return false
        return false;
    }

    private static CallGraphNode extractCallGraphNodeOfMethodDeclarationFromCallGraph(List<CallGraphNode> callGraph, MethodDeclaration childCall) {
        for (CallGraphNode callGraphNode : callGraph)
            if (callGraphNode.hashCode() == new CallGraphNode(childCall, null, null, null, false).hashCode()) {
                return callGraphNode;
            }
        return null;
    }

    public static List<Record> toRecords(List<CallGraphNode> nodes) {
        List<Record> records = new LinkedList<>();
        nodes.forEach(node -> {
            // due to the problem in javaparser and for checking if each and every record's essential data is present, we have to have a try-catch block to handle non-resolvable ones to not add them in the records.
            try {
                Record record = new Record();

                ResolvedMethodDeclaration resolvedMethodDeclaration = node.getMethodDeclaration().resolve();

                String relativePath = node.getRepoPath().relativize(node.getPath()).toString();

                record.setMiddleModulePath(Utils.getBeforeSrc(relativePath));

                record.setHasAnyAssertions(node.hasAnyAssertion());

                record.setPath(Utils.getFromSrc(relativePath));
                assert record.getPath() != null;

                record.setPackageName(node.getMethodDeclaration().resolve().getPackageName());

                record.setClassName(resolvedMethodDeclaration.getClassName());
                assert record.getClassName(false) != null && !record.getClassName(false).isEmpty();

                record.setStartLine(node.getMethodDeclaration().getBegin().map(pos -> pos.line).orElse(-1));
                assert record.getStartLine() != -1;

                record.setEndLine(node.getMethodDeclaration().getEnd().map(pos -> pos.line).orElse(-1));
                assert record.getEndLine() != -1;

                record.setProductionRecord(node.isOfProduction());
                record.setSignature(node.getMethodDeclaration().getDeclarationAsString(true, true, true).trim());
                assert record.getSignature() != null && !record.getSignature().isEmpty();

                record.setRepoPath(node.getRepoPath().toAbsolutePath().toString());
                assert record.getRepoPath() != null && !record.getRepoPath().isEmpty();

                record.setName(node.getMethodDeclaration().getNameAsString());
                assert record.getName() != null && !record.getName().isEmpty();

                record.setRepoName(node.getRepoPath().getFileName().toString());
                assert record.getRepoName() != null && !record.getRepoName().isEmpty();

                records.add(record);
            } catch (Throwable e) {
                System.out.println("Error while making records for a node." +
                        (node.getPath() == null ? "Node path is empty!. " : "Node path: " + node.getPath().toAbsolutePath() + " . ")
                        + (node.getMethodDeclaration() != null && node.getMethodDeclaration().getName() != null && !node.getMethodDeclaration().getName().toString().isEmpty() ? "Method name: " + node.getMethodDeclaration().getName() : "No Valid method declaration!")
                        + " . Error: " + e.getMessage());
            }
        });

        return records;
    }

    public static List<CallGraphNode> buildReversedGraph(List<CallGraphNode> graph) throws Exception {
        List<CallGraphNode> reversedGraph = new LinkedList<>();

        //create edge less reversed graph
        for (CallGraphNode node : graph) {
            CallGraphNode callGraphNode = new CallGraphNode(node.getMethodDeclaration(), node.getPath(), node.getRepoPath(), node.getMiddleModulePath(), node.hasAnyAssertion());
            reversedGraph.add(callGraphNode);
        }


        for (CallGraphNode A : graph) {
            for (MethodDeclaration B : A.getChildren()) {
                CallGraphNode B_p = getFromReverseCallGraph(B, reversedGraph);
                B_p.addChildIfNotExists(A.getMethodDeclaration());
            }
        }

        return reversedGraph;
    }

    private static CallGraphNode getFromReverseCallGraph(MethodDeclaration methodDeclaration, List<CallGraphNode> reversedGraph) throws Exception {
        for (CallGraphNode node : reversedGraph) {
            if (node.hashCode() == new CallGraphNode(methodDeclaration, null, null, null, false).hashCode())
                return node;
        }
        throw new Exception("No equivalent node found in reverse graph!");
    }
}