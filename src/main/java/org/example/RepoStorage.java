package org.example;

import java.io.File;
import java.nio.file.Paths;

public class RepoStorage {

    private static final String TEMPORARY_DIRECTORY_PREFIX = "tmp_";
    private static final String FINAL_DIRECTORY_PREFIX = "final_";
    private final String projectRootDir;
    private final String repositoriesDirectoryName;
    private final String dateTimeFormat;
    private final String experimentsDirectoryName;
    private final String experimentsDateTime;

    public RepoStorage(String projectRootDir, String experimentsDirectoryName, String dateTimeFormat, String repositoriesDirectoryName) {
        this.projectRootDir = projectRootDir;
        this.experimentsDirectoryName = experimentsDirectoryName;
        assert Utils.createDirIfNotExists(Paths.get(this.projectRootDir, this.experimentsDirectoryName).toString());

        this.dateTimeFormat = dateTimeFormat;
        this.repositoriesDirectoryName = repositoriesDirectoryName;

        this.experimentsDateTime = Utils.getDateTime(dateTimeFormat);
        assert Utils.createDirIfNotExists(Paths.get(this.projectRootDir, this.experimentsDirectoryName, this.experimentsDateTime).toString());
    }

    public static String getFinalDirectoryPrefix() {
        return FINAL_DIRECTORY_PREFIX;
    }

    public static String getTemporaryDirectoryPrefix() {
        return TEMPORARY_DIRECTORY_PREFIX;
    }

    public String getExperimentsDateTime() {
        return experimentsDateTime;
    }

    public String getProjectRootDir() {
        return projectRootDir;
    }

    public String getDateTimeFormat() {
        return dateTimeFormat;
    }

    public String getExperimentsDirectoryName() {
        return experimentsDirectoryName;
    }

    public String getRepositoriesDirectoryName() {
        return repositoriesDirectoryName;
    }

    public void storeTemporarily(Record record, String modelName, Constants.Experiment_ID experimentId, int executionCounter) {
        System.out.println("Storing temporarily...");
        String repoDirMethodDirName = record.getRepoName() + "_" + record.getName() + "_" + record.getStartLine() + "_" + record.getEndLine();
        File repoDirMethodNameDir = new File(Paths.get(this.projectRootDir, this.experimentsDirectoryName, this.experimentsDateTime, modelName, experimentId.name(), Integer.toString(executionCounter), this.repositoriesDirectoryName, repoDirMethodDirName).toString());
        String tmpAddress = null;
        if (repoDirMethodNameDir.exists())
            tmpAddress = repoDirMethodNameDir.getAbsolutePath();
        else {
            Utils.backupRepository(record, repoDirMethodNameDir);
            tmpAddress = repoDirMethodNameDir.getAbsolutePath();
        }

        record.setTempRepoPath(tmpAddress);
    }
}
