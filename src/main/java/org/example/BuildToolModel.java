package org.example;

import org.example.ComponentResponse.Status;
import org.example.call_graph.CallGraphNode;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Paths;

public abstract class BuildToolModel {
    public static BuildToolModel getProjectBuildToolModel(String repositoryPath) {
        if (hasPomXml(repositoryPath))
            return new MavenBuildToolModel(repositoryPath);
        else if (hasBuildGradle(repositoryPath))
            return new GradleBuildToolModel(repositoryPath);
        return null;
    }

    public static boolean hasPomXml(String repositoryPath) {
        String pomXmlPath = Paths.get(repositoryPath, "pom.xml").toString();
        return Files.exists(Paths.get(pomXmlPath));
    }

    public static boolean hasBuildGradle(String repositoryPath) {
        String buildGradlePath = Paths.get(repositoryPath, "build.gradle").toString();
        String settingsGradlePath = Paths.get(repositoryPath, "settings.gradle").toString();
        String gradlewPath = Paths.get(repositoryPath, "gradlew").toString();
        return Files.exists(Paths.get(buildGradlePath)) ||
                Files.exists(Paths.get(settingsGradlePath)) ||
                Files.exists(Paths.get(gradlewPath));
    }

    public static ComponentResponse runShellCommand(String[] commands, String dir, String javaAddress) throws Exception {
        StringBuilder totalOutput = new StringBuilder();

        String[] javaAddresses = javaAddress != null ? new String[]{javaAddress} : Constants.JAVA_HOME_VERSIONS;
//        System.out.println("running command: " + String.join(" ", commands) + " in " + dir + " with " + (javaAddress == null ? "uninitialized" : "initialized") + " Java version " + (javaAddress == null ? "" : "(" + javaAddress + ")"));
        for (String _javaAddress : javaAddresses) {
            ProcessBuilder processBuilder = new ProcessBuilder(commands);
            processBuilder.directory(new File(dir));
            processBuilder.environment().putAll(System.getenv());
            processBuilder.environment().put("JAVA_HOME", _javaAddress);
            String javaBinPath = Paths.get(processBuilder.environment().get("JAVA_HOME"), "bin").toString();
            processBuilder.environment().put("PATH", javaBinPath + ":" + processBuilder.environment().get("PATH"));
            processBuilder.redirectErrorStream(true);
            processBuilder.redirectOutput(ProcessBuilder.Redirect.PIPE);
            processBuilder.redirectError(ProcessBuilder.Redirect.PIPE);
            Process process = processBuilder.start();
            totalOutput = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    totalOutput.append(line).append(" \n");
                }
            }

            int exitCode = process.waitFor();
            if (exitCode == 0) {
                System.out.println("Shell Result: Successful");
                return new ComponentResponse(Status.OK, totalOutput.toString(), String.join(" ", commands), _javaAddress);
            }
//            else
//                System.out.println("Failed to succeed when running shell command with the associated Java version");
        }


        System.out.println("Shell Result: Error Occurred");
        return
                new ComponentResponse(Status.ERROR_OCCURRED,
                        totalOutput.toString(),
                        String.join(" ", commands),
                        Constants.JAVA_HOME_VERSIONS[0]);
    }

    public abstract ComponentResponse compile() throws Exception;

    public abstract ComponentResponse compile(boolean clean, String javaAddress) throws Exception;

    public abstract ComponentResponse runTestCase(CallGraphNode node, String moduleName, String javaAddress) throws Exception;
}
