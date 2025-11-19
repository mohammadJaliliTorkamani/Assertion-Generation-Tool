package org.example;

import java.util.ArrayList;
import java.util.List;

public class EvaluationDataset extends Dataset {
    //some statistic calculator methods, specific to evaluation dataset

    //

    @Override
    public String toString() {
        return "EvaluationDataset{" +
                "records=" + getRecordsAsList() +
                '}';
    }

    public void create(Pair<Constants.Experiment_ID, String>... experimentID_path) {
        for (Pair<Constants.Experiment_ID, String> experimentID_path_pair : experimentID_path) {
            Constants.Experiment_ID experimentId = experimentID_path_pair.getFirst();
            String path = experimentID_path_pair.getSecond();
            System.out.println("Experiment: "+experimentId.name());

            List<LLM_InputContent> fineTuneDatasetRecords = switch (experimentId) {
                case A -> createFineTuneDataset_A();
                case B -> createFineTuneDataset_B();
                case C -> createFineTuneDataset_C();
                case D -> createFineTuneDataset_D();
            };
            Utils.saveToJson(fineTuneDatasetRecords, path);
        }
    }

    private List<LLM_InputContent> createFineTuneDataset_D() {
        List<LLM_InputContent> list = new ArrayList<>();
        Inference inference = Inference.getInferenceObject();

        //// Check if all records in the fsl_ft_dataset have embedding vectors, otherwise compute and re-store them
        FSLDataset fslDataset = Utils.loadDataset(Constants.FSL_FOR_FINE_TUNING_DB, FSLDataset.class);
        if (fslDataset.hasIncompleteEmbeddingVectors()) {
            fslDataset.calculateEmbeddingVectors(inference);
            fslDataset.setRecords(fslDataset.getRecordsAsList().stream().filter(record -> record.getEmbeddingVector() != null && !record.getEmbeddingVector().isEmpty()).toList());
            fslDataset.restoreDataset();
            fslDataset = Utils.loadDataset(Constants.FSL_FOR_FINE_TUNING_DB, FSLDataset.class);
            assert fslDataset != null;
        }

        assert !fslDataset.hasIncompleteEmbeddingVectors();
        ////

        int i = 1;
        for (Record record : getRecordsAsList()) {
            System.out.println("    Progress: " + (i++) + "/" + getRecordCount());
            try {
                list.add(inference.generateFineTuningLLMInputContent(record, Constants.Experiment_ID.D, fslDataset));
            } catch (Exception e) {
                System.out.println("Cannot generate LLM Input Content for Experiment D for record: " + record.getName() + " | Error: " + e.getMessage());
            }
        }

        return list;
    }

    private List<LLM_InputContent> createFineTuneDataset_C() {
        List<LLM_InputContent> list = new ArrayList<>();
        Inference inference = Inference.getInferenceObject();

        int i = 1;
        for (Record record : getRecordsAsList()) {
            System.out.println("    Progress: " + (i++) + "/" + getRecordCount());
            try {
                list.add(inference.generateFineTuningLLMInputContent(record, Constants.Experiment_ID.C, null));
            } catch (Exception e) {
                System.out.println("Cannot generate LLM Input Content for Experiment C for record: " + record.getName() + " | Error: " + e.getMessage());
            }
        }

        return list;
    }

    private List<LLM_InputContent> createFineTuneDataset_B() {
        List<LLM_InputContent> list = new ArrayList<>();
        Inference inference = Inference.getInferenceObject();

        int i = 1;
        for (Record record : getRecordsAsList()) {
            System.out.println("    Progress: " + (i++) + "/" + getRecordCount());
            try {
                list.add(inference.generateFineTuningLLMInputContent(record, Constants.Experiment_ID.B, null));
            } catch (Exception e) {
                System.out.println("Cannot generate LLM Input Content for Experiment B for record: " + record.getName() + " | Error: " + e.getMessage());
            }
        }

        return list;
    }

    private List<LLM_InputContent> createFineTuneDataset_A() {
        List<LLM_InputContent> list = new ArrayList<>();
        Inference inference = Inference.getInferenceObject();

        int i = 1;
        for (Record record : getRecordsAsList()) {
            System.out.println("    Progress: " + (i++) + "/" + getRecordCount());

            try {
                list.add(inference.generateFineTuningLLMInputContent(record, Constants.Experiment_ID.A, null));
            } catch (Exception e) {
                System.out.println("Cannot generate LLM Input Content for Experiment A for record: " + record.getName() + " | Error: " + e.getMessage());
            }
        }

        return list;
    }
}