package org.example;

import com.github.javaparser.resolution.declarations.ResolvedMethodDeclaration;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.example.call_graph.CallGraphNode;

import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.List;

public class MavenBuildToolModel extends BuildToolModel {
    private final String repoPath;

    public MavenBuildToolModel(String repoPath) {
        this.repoPath = repoPath;
    }

    public static List<Dependency> parseDependencyFromPOM(String repoPath) throws Exception {
        File pomFile = new File(repoPath, "pom.xml");
        List<Dependency> dependencies = new ArrayList<>();

        try (FileReader reader = new FileReader(pomFile)) {
            MavenXpp3Reader xpp3Reader = new MavenXpp3Reader();
            Model model = xpp3Reader.read(reader);

            List<Dependency> pomDependencies = model.getDependencies();
            dependencies.addAll(pomDependencies);
        }

        return dependencies;
    }

    @Override
    public ComponentResponse compile() throws Exception {
        return this.compile(false, null);
    }

//    @Override
//    @Deprecated
//    public ComponentResponse compile(boolean clean, String javaAddress) throws Exception {
//        String[] commands;
//        File mvnwFile = new File(repoPath, "mvnw");
//        boolean mvnwExists = mvnwFile.exists() && mvnwFile.isFile();
//        if (!mvnwExists) {
//            if (clean)
//                commands = new String[]{
//                        Constants.MVN_BINARY_PATH,
//                        "clean", "package",
//                        "dependency:resolve",
//                        "process-resources",
//                        "-DskipTests=true", "-Dcheckstyle.skip=true", "-Dspotbugs.skip=true", "-Dspotless.check.skip=true", "-Dmaven.javadoc.skip=true"
//                };
//            else
//                commands = new String[]{
////                    "cmd.exe", "/c",
//                        Constants.MVN_BINARY_PATH, "package",
//                        "-DskipTests=true", "-Dcheckstyle.skip=true", "-Dspotbugs.skip=true", "-Dspotless.check.skip=true", "-Dmaven.javadoc.skip=true"
//                };
//        } else {
//            if (clean)
//                commands = new String[]{"./mvnw", "clean", "install", "-DskipTests=true", "-Dcheckstyle.skip=true", "-Dspotbugs.skip=true", "-Dspotless.check.skip=true", "-Dmaven.javadoc.skip=true"};
//            else
//                commands = new String[]{"./mvnw", "install", "-DskipTests=true", "-Dcheckstyle.skip=true", "-Dspotbugs.skip=true", "-Dspotless.check.skip=true", "-Dmaven.javadoc.skip=true"};
//        }
//
//
////        System.out.printf("---> Compiling repository....%n");
//        return BuildToolModel.runShellCommand(commands, repoPath, javaAddress);
//    }

    @Override
    public ComponentResponse compile(boolean clean, String javaAddress) throws Exception {
        String[] commands;
        File mvnwFile = new File(repoPath, "mvnw");
        if (clean)
            commands = new String[]{
                    Constants.MVN_BINARY_PATH,
                    "-T","1C",
                    "clean", "install",
                    "-DskipTests=true", "-Dcheckstyle.skip=true", "-Dspotbugs.skip=true", "-Dspotless.check.skip=true", "-Dmaven.javadoc.skip=true"
            };
        else
            commands = new String[]{
//                    "cmd.exe", "/c",
                    Constants.MVN_BINARY_PATH,
                    "-T","1C", "install",
                    "-DskipTests=true", "-Dcheckstyle.skip=true", "-Dspotbugs.skip=true", "-Dspotless.check.skip=true", "-Dmaven.javadoc.skip=true"
            };


//        System.out.printf("---> Compiling repository....%n");
        return BuildToolModel.runShellCommand(commands, repoPath, javaAddress);
    }

    @Override
    public ComponentResponse runTestCase(CallGraphNode node, String moduleName, String javaAddress) throws Exception {
        ResolvedMethodDeclaration resolvedMethodDeclaration = node.getMethodDeclaration().resolve();
        String test = resolvedMethodDeclaration.getPackageName() + "." + resolvedMethodDeclaration.getClassName() + "#" + node.getMethodDeclaration().getName();

        String[] command;
        if (moduleName == null || moduleName.isEmpty())
            command = new String[]{
                    Constants.MVN_BINARY_PATH, "test", "-Dcheckstyle.skip=true", "-Dspotbugs.skip=true", "-Dspotless.check.skip=true", "-Dmaven.javadoc.skip=true", "-Dtest=" + test};
        else
            command = new String[]{
                    Constants.MVN_BINARY_PATH, "-pl", moduleName, "test", "-Dcheckstyle.skip=true", "-Dspotbugs.skip=true", "-Dspotless.check.skip=true", "-Dmaven.javadoc.skip=true", "-Dtest=" + test};

        return BuildToolModel.runShellCommand(command, repoPath, javaAddress);
    }


}
