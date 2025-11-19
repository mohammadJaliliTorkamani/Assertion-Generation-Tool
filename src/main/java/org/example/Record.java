package org.example;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.stmt.AssertStmt;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;
import com.github.javaparser.javadoc.Javadoc;
import com.github.javaparser.printer.PrettyPrinter;
import com.github.javaparser.printer.configuration.PrettyPrinterConfiguration;
import org.example.call_graph.RepoJavaParser;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class Record {
    private transient boolean productionRecord;
    private String className;
    private String name;
    private String path;
    private String middleModulePath;
    private String repoPath;
    private String signature;
    private String repoName;
    private String packageName;
    private int startLine;
    private int endLine;
    private transient MethodDeclaration MethodDeclaration = null;
    private List<Double> embeddingVector;
    private String tempRepoPath;
    private boolean hasAnyAssertions;
    private transient List<Record> unitTests = new ArrayList<>();

    public Record() {
        this(true);
    }

    public Record(boolean productionRecord) {
        this.productionRecord = productionRecord;
    }

    public void setHasAnyAssertions(boolean hasAnyAssertions) {
        this.hasAnyAssertions = hasAnyAssertions;
    }


    public boolean hasAnyAssertions() {
        return hasAnyAssertions;
    }

    public MethodDeclaration getMethodWithoutAssertion() {
        if (MethodDeclaration == null)
            MethodDeclaration = findResolvedMethodDeclaration();
        try {
            return RepoJavaParser.getInstance(Paths.get(getRepoPath(), getMiddleModulePath()).toString()).parseMethodDeclaration(printMethod(false, true, false)).getResult().orElse(null);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public boolean isProductionRecord() {
        return productionRecord;
    }

    public void setProductionRecord(boolean productionRecord) {
        this.productionRecord = productionRecord;
    }

    public List<Double> getEmbeddingVector() {
        return embeddingVector;
    }

    public void setEmbeddingVector(List<Double> embeddingVector) {
        this.embeddingVector = embeddingVector;
    }

    public String getFullMethodPath(boolean useTempRepo) {
        return Paths.get(useTempRepo ? tempRepoPath : repoPath, middleModulePath, path).toString();
    }

    public String getFullMethodPath() {
        return getFullMethodPath(false);
    }

    public Javadoc extractJavadoc(String methodName) throws Exception {
        return findResolvedMethodDeclaration().getJavadoc().orElse(null);
    }

    public String printPureMethod() throws Exception {
        return printMethod(true, true, true);
    }


    public String printMethod(boolean excludeComments, boolean excludeAssertions, boolean excludeJavadoc) throws Exception {
        MethodDeclaration _methodDeclaration = findResolvedMethodDeclaration().clone();
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
            Javadoc _javadoc = extractJavadoc(_methodDeclaration.getNameAsString());
            if (_javadoc != null) javadoc = _javadoc.toComment().toString();
        }

        PrettyPrinter prettyPrinter = new PrettyPrinter(configuration);
        return javadoc + Parser.addCurlyBraces(prettyPrinter.print(_methodDeclaration));
    }

    public MethodDeclaration findResolvedMethodDeclaration() {
        if (MethodDeclaration != null)
            return MethodDeclaration;
        try {
            // Build the full path to the file
            Path filePath = Paths.get(this.getFullMethodPath());
            CompilationUnit cu = StaticJavaParser.parse(filePath.toFile());

            // Find the class in the file
            ClassOrInterfaceDeclaration classDecl = cu.findAll(ClassOrInterfaceDeclaration.class)
                    .stream()
                    .filter(cls -> cls.getNameAsString().equals(this
                            .getClassName(true)))
                    .findFirst()
                    .orElseThrow(() -> new RuntimeException("Class not found: " + this.getClassName(true)));

            // Find the method in the class
            this.MethodDeclaration = classDecl.findAll(MethodDeclaration.class)
                    .stream()
                    .filter(method -> method.getNameAsString().equals(this.getName()) &&
                            method.getDeclarationAsString(true, true, true).trim().equals(this.getSignature()))
                    .findFirst()
                    .orElseThrow(() -> new RuntimeException("Method not found: " + this.getName()));
            ;
            return this.MethodDeclaration;

        } catch (Exception e) {
            throw new RuntimeException("Failed to resolve method declaration", e);
        }
    }

    public void addUnitTest(Record unitTest) {
        this.unitTests.add(unitTest);
    }

    public String getRepoPath() {
        return repoPath;
    }

    public void setRepoPath(String repoPath) {
        this.repoPath = repoPath;
    }

    public String getMiddleModulePath() {
        return middleModulePath;
    }

    public void setMiddleModulePath(String middleModulePath) {
        this.middleModulePath = middleModulePath;
    }


    public String getClassName(boolean getInnerName) {
        if (getInnerName)
            return className.split("\\.")[className.split("\\.").length - 1];

        return className;
    }

    public void setClassName(String className) {
        this.className = className;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getSignature() {
        return signature == null ? null : signature.trim();
    }

    public void setSignature(String signature) {
        this.signature = signature == null ? signature : signature.trim();
    }

    public String getRepoName() {
        return repoName;
    }

    public void setRepoName(String repoName) {
        this.repoName = repoName;
    }

    public String getPackageName() {
        return packageName;
    }

    public void setPackageName(String packageName) {
        this.packageName = packageName;
    }

    public int getStartLine() {
        return startLine;
    }

    public void setStartLine(int startLine) {
        this.startLine = startLine;
    }

    public int getEndLine() {
        return endLine;
    }

    public void setEndLine(int endLine) {
        this.endLine = endLine;
    }

    @Override
    public String toString() {
        return "Method{" +
                "className='" + className + '\'' +
                ", name='" + name + '\'' +
                ", path='" + path + '\'' +
                ", middleModulePath='" + middleModulePath + '\'' +
                ", repoPath='" + repoPath + '\'' +
                ", signature='" + signature + '\'' +
                ", repoName='" + repoName + '\'' +
                ", packageName='" + packageName + '\'' +
                ", startLine=" + startLine +
                ", endLine=" + endLine +
                '}';
    }

    public String getMethodWithoutAssertions() throws Exception {
        return printMethod(false, true, false);
    }

    public String getMethodWithAssertions() throws Exception {
        return printMethod(false, false, false);
    }

    public Set<AssertStmt> extractAssertions() {
        Set<AssertStmt> assertions = new HashSet<>();
        findResolvedMethodDeclaration().accept(new VoidVisitorAdapter<Void>() {
            @Override
            public void visit(AssertStmt assertStmt, Void arg) {
                super.visit(assertStmt, arg);
                assertions.add((AssertStmt) assertStmt.removeComment());

            }
        }, null);
        return assertions;
    }

    public String getTempRepoPath() {
        return tempRepoPath;
    }

    public void setTempRepoPath(String tmpRepoPath) {
        this.tempRepoPath = tmpRepoPath;
    }

    public void print() {
        System.out.println("\n\n***  Record: `" + getName() + "' in class (`" + getClassName(false) + "')\n");
    }
}

