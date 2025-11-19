package org.example.call_graph;

import com.github.javaparser.JavaParser;
import com.github.javaparser.resolution.TypeSolver;
import com.github.javaparser.symbolsolver.JavaSymbolSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.CombinedTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.JavaParserTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.ReflectionTypeSolver;

import java.io.File;
import java.nio.file.Paths;

public class RepoJavaParser {
    public static JavaParser getInstance(String repoPath) {
        TypeSolver reflectionTypeSolver = new ReflectionTypeSolver();
        File productionDir = new File(Paths.get(repoPath, "src", "main", "java").toString());
        File testDir = new File(Paths.get(repoPath, "src", "test", "java").toString());

        assert productionDir.exists();
        CombinedTypeSolver combinedSolver = new CombinedTypeSolver();
        combinedSolver.add(reflectionTypeSolver);

        if (productionDir.exists())
            combinedSolver.add(new JavaParserTypeSolver(productionDir));

        if (testDir.exists())
            combinedSolver.add(new JavaParserTypeSolver(testDir));

        JavaSymbolSolver symbolSolver = new JavaSymbolSolver(combinedSolver);

//        StaticJavaParser.getConfiguration().setSymbolResolver(symbolSolver);

        JavaParser javaParser = new JavaParser();
        javaParser.getParserConfiguration().setSymbolResolver(symbolSolver);
//        StaticJavaParser.getParserConfiguration().setSymbolResolver(symbolSolver);
//
        return javaParser;
    }
}
