package org.example.call_graph;

import com.github.javaparser.ast.body.MethodDeclaration;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;


public class CallGraphNode {
    private transient MethodDeclaration methodDeclaration;
    private transient Path path;
    private transient Path repoPath;
    private String middleModulePath;
    private boolean hasAssertion;
    private boolean isTest;
    private transient List<MethodDeclaration> children;

    public CallGraphNode(MethodDeclaration methodDeclaration, Path path, Path repoPath, String middleModulePath, boolean hasAssertion) {
        this.methodDeclaration = methodDeclaration;
        this.path = path;
        this.repoPath = repoPath;
        this.middleModulePath = middleModulePath;
        this.isTest = path == null ? false : isTestMethod(path, methodDeclaration);
        this.hasAssertion = isTest ? false : hasAssertion; //hasAssertion is false for methods since we target production assertions
        this.children = new ArrayList<>();
    }

    public static String generateMethodIdentifier(MethodDeclaration method) {
        String className = method.findAncestor(com.github.javaparser.ast.body.ClassOrInterfaceDeclaration.class)
                .map(classOrInterfaceDeclaration -> classOrInterfaceDeclaration.getNameAsString())
                .orElse("UnknownClass");
        String methodName = method.getNameAsString();
        String parameters = method.getParameters().stream()
                .map(param -> param.getType().asString())
                .reduce((a, b) -> a + "," + b)
                .orElse("");

        return className + "#" + methodName + "(" + parameters + ")";
    }

    public boolean hasAnyAssertion() {
        return hasAssertion;
    }

    public boolean isOfTest() {
        return isTest;
    }

    public boolean isOfProduction() {
        return !isTest;
    }

    public Path getRepoPath() {
        return repoPath;
    }

    public String getMiddleModulePath() {
        return middleModulePath;
    }

    public void setMiddleModulePath(String middleModulePath) {
        this.middleModulePath = middleModulePath;
    }

    private boolean isTestMethod(Path path, MethodDeclaration methodDeclaration) {
        boolean pathIndicatesTest = path.toString().contains("src" + File.separator + "test" + File.separator + "java");
        boolean hasTestAnnotation = methodDeclaration.getAnnotations().stream()
                .anyMatch(annotation -> annotation.getNameAsString().equals("Test"));
        return pathIndicatesTest || hasTestAnnotation;
    }

    public List<MethodDeclaration> getChildren() {
        return children;
    }

    public void setChildren(List<MethodDeclaration> children) {
        this.children = children;
    }

    public void addChildIfNotExists(MethodDeclaration child) {
        if (!children.contains(child))
            children.add(child);
    }

    public MethodDeclaration getMethodDeclaration() {
        return methodDeclaration;
    }

    public void setMethodDeclaration(MethodDeclaration methodDeclaration) {
        this.methodDeclaration = methodDeclaration;
    }

    public Path getPath() {
        return path;
    }

    public void setPath(Path path) {
        this.path = path;
    }

    public void setHasAssertion(boolean hasAssertion) {
        this.hasAssertion = hasAssertion;
    }

    public void setTestMethod(boolean testMethod) {
        isTest = testMethod;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CallGraphNode that = (CallGraphNode) o;
        return generateMethodIdentifier(this.getMethodDeclaration()).equals(generateMethodIdentifier(that.getMethodDeclaration()));
    }

    @Override
    public int hashCode() {
        return Objects.hash(generateMethodIdentifier(methodDeclaration));
    }

    @Override
    public String toString() {
        return "CallGraphNode{" +
                "methodDeclaration=" + methodDeclaration +
                ", path=" + path +
                ", repoPath=" + repoPath +
                ", middleModulePath=" + middleModulePath +
                ", hasAssertion=" + hasAssertion +
                ", isTest=" + isTest +
                ", children=" + children + '\'' +
                '}';
    }
}