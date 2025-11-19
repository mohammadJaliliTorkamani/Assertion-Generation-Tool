package org.example;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.eclipse.jgit.api.Git;
import org.example.call_graph.CallGraphAnalyzer;
import org.example.call_graph.CallGraphNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

public class DatasetExtractor {
    private static final Logger logger = LoggerFactory.getLogger(DatasetExtractor.class);
    private final String csvPath;
    private final String reservoirPath;
    private final String evaluationDbPath;
    private final String fslDbPath;
    private final String fslForFinetuningDbPath;
    private final String fineTuning_trainPath;
    private final String fineTuning_validationPath;
    private final List<CallGraphNode> evaluationNodes = new LinkedList<>();
    private final List<CallGraphNode> FSLNodes = new LinkedList<>();
    private final List<CallGraphNode> FSLForFinetuningNodes = new LinkedList<>();
    private final List<CallGraphNode> Fine_tuningTrainingNodes = new LinkedList<>();
    private final List<CallGraphNode> Fine_tuningValidationNodes = new LinkedList<>();
    private final List<List<CallGraphNode>> totalNodes = new LinkedList<>(); //stores repos or their different modules, along with their reverse graph

    public DatasetExtractor(String inputCSVPath, String reservoirPath, String evaluationDbPath, String fslDbPath, String fslForFine_tuningDbPath,
                            String fineTuning_trainPath, String fineTuning_validationPath
    ) {
        this.csvPath = inputCSVPath;
        this.reservoirPath = reservoirPath;
        this.evaluationDbPath = evaluationDbPath;
        this.fslDbPath = fslDbPath;
        this.fslForFinetuningDbPath = fslForFine_tuningDbPath;
        this.fineTuning_trainPath = fineTuning_trainPath;
        this.fineTuning_validationPath = fineTuning_validationPath;
    }

    /**
     * @param repoUrl
     * @param folderPath
     * @return repoName if download is successfully or repo exists
     */
    public static String cloneRepository(String repoUrl, String folderPath) {
        try {
            // Extract repository name from URL
            String repoName = repoUrl.substring(repoUrl.lastIndexOf('/') + 1).replace(".git", "");
            Path repoPath = Paths.get(folderPath, repoName);
            Utils.createDirIfNotExists(repoPath.toAbsolutePath().toString());

            boolean isEmpty = Utils.isDirectoryEmpty(repoPath);
            boolean isExisted = Files.exists(repoPath);
            if (isExisted && !isEmpty) {
                System.out.println("Already exists...");
                return repoName;
            } else if (isExisted) {
                Files.delete(repoPath);
            }

            System.out.println("Downloading...");
            Git.cloneRepository()
                    .setURI((!repoUrl.contains("https://") ? "https://" + repoUrl : repoUrl))
                    .setDirectory(repoPath.toFile())
                    .call();
            return repoName;
        } catch (Exception e) {
            System.out.println("Error while downloading " + repoUrl);
            return null;
        }
    }

    public void extract() throws Exception {
        Utils.createDirIfNotExists(reservoirPath);

        System.out.println("1. Downloading...");
        downloadRepositoriesAndPopulate();
        System.out.println("### Done. Total nodes size: " + totalNodes.size());
        System.out.println("2. Splitting...");
        splitDatasets();
        System.out.println("### Done. Evaluation nodes size: " + evaluationNodes.size() + " | FSL nodes size: " + FSLNodes.size() + " | Fine-tuning training nodes size: " + Fine_tuningTrainingNodes.size() + " | Fine-tuning validation nodes size: " + Fine_tuningValidationNodes.size());
        System.out.println("3. Converting and Saving...");
        convertAndSaveDatasets();
    }

    private void convertAndSaveDatasets() {

        List<Record> evaluationRecords = CallGraphAnalyzer.toRecords(evaluationNodes);
        List<Record> FSLRecords = CallGraphAnalyzer.toRecords(FSLNodes);
        List<Record> FSLForFinetuningRecords = CallGraphAnalyzer.toRecords(FSLForFinetuningNodes);
        List<Record> Fine_tuning_TrainingRecords = CallGraphAnalyzer.toRecords(Fine_tuningTrainingNodes);
        List<Record> Fine_tuning_ValidationRecords = CallGraphAnalyzer.toRecords(Fine_tuningValidationNodes);

        System.out.println("    Total Dataset size:" + totalNodes.size());
        System.out.println("    Correct (working) Evaluation DS size:" + evaluationRecords.size());
        System.out.println("    Correct (working) FSL DS size:" + FSLRecords.size());
        System.out.println("    Correct (working) FSL for fine-tuning DS size:" + FSLForFinetuningRecords.size());
        System.out.println("    Correct (working) Fine-tuning training DS size:" + Fine_tuning_TrainingRecords.size());
        System.out.println("    Correct (working) Fine-tuning validation DS size:" + Fine_tuning_ValidationRecords.size());

        Collections.shuffle(evaluationRecords);
        if (evaluationRecords.size() > Constants.EVALUATION_DATASET_UPPER_LIMIT_SIZE) {
            System.out.println("Evaluation size exceeded the limit (" + Constants.EVALUATION_DATASET_UPPER_LIMIT_SIZE + "). Randomly selecting " + Constants.EVALUATION_DATASET_UPPER_LIMIT_SIZE + " records... .");
            evaluationRecords = evaluationRecords.subList(0, Constants.EVALUATION_DATASET_UPPER_LIMIT_SIZE);
        }

        Collections.shuffle(FSLRecords);
        Collections.shuffle(FSLForFinetuningRecords);
        Collections.shuffle(Fine_tuning_TrainingRecords);
        Collections.shuffle(Fine_tuning_ValidationRecords);

        Utils.saveToJson(evaluationRecords, this.evaluationDbPath);
        Utils.saveToJson(FSLRecords, this.fslDbPath);
        Utils.saveToJson(FSLForFinetuningRecords, this.fslForFinetuningDbPath);
        Utils.saveToJson(Fine_tuning_TrainingRecords, this.fineTuning_trainPath);
        Utils.saveToJson(Fine_tuning_ValidationRecords, this.fineTuning_validationPath);
    }

    private void splitDatasets() throws Exception {
        System.out.println("Total size (totalNodes) to split: " + totalNodes.size());
        boolean fslTurn = false;
        int counter1 = 0;
        int counter2;
        List<CallGraphNode> fourRemainingDatasets = new LinkedList<>();
        for (List<CallGraphNode> graph : totalNodes) {
            List<CallGraphNode> reversedGraph = CallGraphAnalyzer.buildReversedGraph(graph);
            System.out.println((++counter1) + " / " + totalNodes.size());
            System.out.println("#Nodes: " + graph.size() + " | #Evaluation Nodes: " + evaluationNodes.size() + " | #FSL Nodes: " + FSLNodes.size());
            if (graph.size() > Constants.MAX_CALL_GRAPH_NODES_SIZE) {
                System.out.println("Node size (" + graph.size() + ") exceeded (" + Constants.MAX_CALL_GRAPH_NODES_SIZE + "). Skipping...");
                continue;
            }
            counter2 = 0;
            for (CallGraphNode node : graph) {
                System.out.println("    " + (++counter2) + " / " + graph.size());
                if (node.isOfProduction()) {
                    List<Pair<CallGraphNode, Boolean>> testerUnitTests = CallGraphAnalyzer.getTesterNodes(node, reversedGraph);
                    if (node.hasAnyAssertion()) {
                        if (!testerUnitTests.isEmpty())
                            evaluationNodes.add(node);
                        else
                            fourRemainingDatasets.add(node);
                    }
                }
            }
        }

        Collections.shuffle(fourRemainingDatasets);

        assert Constants.PERCENTAGE_FINE_TUNINIG_FSL + Constants.PERCENTAGE_FINE_TUNINIG_TRAINING + Constants.PERCENTAGE_FINE_TUNINIG_VALIDATION + Constants.PERCENTAGE_PIPELINE_FSL == 100;

        int totalSize = fourRemainingDatasets.size();
        int size_finetuning_training = (int) (totalSize * Constants.PERCENTAGE_FINE_TUNINIG_TRAINING / 100.0);
        int size_finetuning_validation = (int) (totalSize * Constants.PERCENTAGE_FINE_TUNINIG_VALIDATION / 100.0);
        int size_finetuning_fsl = (int) (totalSize * Constants.PERCENTAGE_FINE_TUNINIG_FSL / 100.0);
        int size_pipeline_fsl = totalSize - size_finetuning_training - size_finetuning_validation - size_finetuning_fsl;

        Fine_tuningTrainingNodes.addAll(fourRemainingDatasets.subList(0, size_finetuning_training));
        Fine_tuningValidationNodes.addAll(fourRemainingDatasets.subList(size_finetuning_training, size_finetuning_training + size_finetuning_validation));
        FSLForFinetuningNodes.addAll(fourRemainingDatasets.subList(size_finetuning_training + size_finetuning_validation, size_finetuning_training + size_finetuning_validation + size_finetuning_fsl));
        FSLNodes.addAll(fourRemainingDatasets.subList(size_finetuning_training + size_finetuning_validation + size_finetuning_fsl, totalSize));

    }

    public void downloadRepositoriesAndPopulate() {
        // Step 1: Read CSV and extract unique repository URLs
        try (FileReader fileReader = new FileReader(csvPath);
             CSVParser csvParser = new CSVParser(fileReader, CSVFormat.DEFAULT.withFirstRecordAsHeader())) {

            Set<String> middleModuleNames = new HashSet<>(); //sometimes the repo names are the same but from different developers. so we create a unique key (using their author and module names is enough in our research) to track it.
            for (CSVRecord record : csvParser) {
                String repoUrl = "https://" + record.get("Repository").trim();
                System.out.println("Repository: " + repoUrl);

                String repoName = cloneRepository(repoUrl, reservoirPath);
                assert repoName != null;
                String author = Utils.extractRepoOwner(repoUrl);
                assert author != null;

                String originalFilePath = record.get("File path").trim();
                String filePath = Utils.getFromSrc(originalFilePath); // decided to compute nodes with call graph rather than extracting them from excel and using this field. However, Excel's is an alternative way if call graph had any problems in future
                String middleModulePath = Utils.getBeforeSrc(originalFilePath);
                String KEY = repoName + "@@@" + author + ((middleModulePath != null && !middleModulePath.isEmpty()) ? "@@@" + middleModulePath : "");

                if (middleModuleNames.contains(KEY))
                    continue;


                try {
                    List<CallGraphNode> graph;
                    List<CallGraphNode> reverseGraph;
                    System.out.println("Building graph...");
                    graph = CallGraphAnalyzer.buildGraph(Paths.get(reservoirPath, repoName).toString(), middleModulePath);
                    if (graph != null) {
                        middleModuleNames.add(KEY);
                        //we computed call graph. let's populate the entire list!
                        totalNodes.add(graph);
                    } else {
                        System.err.println("CALL GRAPH SIZE EXCEEDED. SKIPPING THE RECORD...");
                    }
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
