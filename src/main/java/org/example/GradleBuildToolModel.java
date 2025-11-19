package org.example;

import org.example.call_graph.CallGraphNode;

import java.util.ArrayList;
import java.util.List;

public class GradleBuildToolModel extends BuildToolModel {

    private final String repoPath;

    public GradleBuildToolModel(String repoPath) {
        this.repoPath = repoPath;
    }

    @Override
    public ComponentResponse compile() throws Exception {
        return this.compile(false, null);
    }

//    @Override
//    @Deprecated
//    public ComponentResponse compile(boolean clean, String javaAddress) throws Exception {
//        String[] commands;
//        if (clean)
//            commands = new String[]{
////                    "cmd.exe", "/c",
//                    "./gradlew", "clean",
//                    "dependencies", "processResources", "processTestResources",
//                    "build",
//                    "-x", "test",
//                    "-x", "check",    //new ( to fix the problem: Mqtt3SendMaximumIT > mqtt3_sendMaximum_applied() FAILED)
//                    "-x", "javadoc"
//            };
//        else
//            commands = new String[]{
////                    "cmd.exe", "/c",
//                    "./gradlew",
//                    "dependencies", "processResources", "processTestResources",
//                    "build",
//                    "-x", "test",
//                    "-x", "check",    //new ( to fix the problem: Mqtt3SendMaximumIT > mqtt3_sendMaximum_applied() FAILED)
//                    "-x", "javadoc"
//            };
////        System.out.printf("---> Compiling repository.... %n");
//        return runShellCommand(commands, repoPath, javaAddress);
//    }

    @Override
    public ComponentResponse compile(boolean clean, String javaAddress) throws Exception {
        String[] commands;
        if (clean)
            commands = new String[]{
//                    "cmd.exe", "/c",
                    "./gradlew", "clean",
                    "build",
                    "--parallel",
                    "--max-workers=8",
                    "-x", "test",
                    "-x", "javadoc"
            };
        else
            commands = new String[]{
//                    "cmd.exe", "/c",
                    "./gradlew",
                    "build",
                    "--parallel",
                    "--max-workers=8",
                    "-x", "test",
                    "-x", "javadoc"
            };
//        System.out.printf("---> Compiling repository.... %n");
        return runShellCommand(commands, repoPath, javaAddress);
    }

    @Override
    public ComponentResponse runTestCase(CallGraphNode node, String moduleName, String javaAddress) throws Exception {

        String test = node.getMethodDeclaration().resolve().getClassName() + "." + node.getMethodDeclaration().getName();

        moduleName = moduleName.contains("sirix-core") ? "sirix-core" : moduleName;

        List<String> params = new ArrayList<>();
        params.add("./gradlew");
        params.add((moduleName != null && !moduleName.isBlank() ? (":" + moduleName + ":") : "") + "test");
        params.add("--tests");
        params.add("--no-daemon");
        params.add("--debug");
        params.add(test);

        ComponentResponse componentResponse = BuildToolModel.runShellCommand(params.toArray(new String[0]), repoPath, javaAddress);
        if (componentResponse.getMessage().contains("BUILD SUCCESSFUL"))
            componentResponse.setCode(ComponentResponse.Status.OK);
        else if (componentResponse.getMessage().contains("BUILD FAILED"))
            componentResponse.setCode(ComponentResponse.Status.ERROR_OCCURRED);
        else
            componentResponse.setCode(ComponentResponse.Status.EXCEPTION_OCCURRED);

        return componentResponse;
    }
}
