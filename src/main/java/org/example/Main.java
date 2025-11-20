package org.example;

import java.nio.file.Paths;

import static org.example.Constants.LLM_REQUEST_TRIAL_NUMBER;

public class Main {
    public static void main(String[] args) {
        System.out.println("Assertion start date/time: "+Utils.getCurrentDateTime()+"\n");
        Constants.Stage STAGE = Constants.Stage.PIPELINE;

        Utils.createDirIfNotExists(Paths.get(Constants.PROJECT_ROOT_DIR, Constants.ORIGINAL_DOWNLOADED_REPOSITORIES_RESERVOIR_DIRECTORY_NAME).toString());

        if (STAGE == Constants.Stage.PIPELINE) {
            Pipeline pipeline = new Pipeline();
            PreEvaluator preEvaluator = new PreEvaluator();
            RepoStorage repoStorage = new RepoStorage(Constants.PROJECT_ROOT_DIR, Constants.EXPERIMENTS_DIRECTORY_NAME, Constants.DATE_TIME_FORMAT, Constants.REPOSITORIES_DIRECTORY_NAME);
            SimilarRecordsExtractor similarRecordsExtractor = new SimilarRecordsExtractor(Constants.IN_CONTEXT_LEARNING_NUMBER_OF_SAMPLES, Constants.FEW_SHOT_LEARNING_COSINE_THRESHOLD);
            Inference inference = new Inference(new LLM_Config(Constants.OPENAI_API_KEY, Constants.LLM.GPT_4O, 1.0, 1.0, 0, 0), LLM_REQUEST_TRIAL_NUMBER);
            MethodSpecificInfoExtractor methodSpecificInfoExtractor = new MethodSpecificInfoExtractor();
            LLMResponseExtractor llmResponseExtractor = new LLMResponseExtractor();
            MetricsCalculator metricsCalculator = new MetricsCalculator(Constants.PYTHON_HUGGINGFACE_FILE);
            Evaluator evaluator = new Evaluator();
            pipeline
                    .setExecutionRepeatCounter(Constants.EXECUTION_REPEAT_COUNTER)
                    .setPreEvaluator(preEvaluator)
                    .setRepoStorage(repoStorage)
                    .setSimilarRecordsExtractor(similarRecordsExtractor)
                    .setInference(inference)
                    .setMethodSpecificInfoExtractor(methodSpecificInfoExtractor)
                    .setLLMResponseExtractor(llmResponseExtractor)
                    .setScoreEvaluator(metricsCalculator)
                    .setEvaluator(evaluator)
                    .execute(Constants.EVALUATION_DB, Constants.FSL_DB, Constants.OUTPUT_FILE, Constants.AVERAGE_OUTPUT_FILE);
        } else if (STAGE == Constants.Stage.DATASET_GENERATION) {
            System.out.println("Number of unique repositories in original CSV: " + Utils.countUniqueRepositories(Constants.STAR_TMP_CSV));


            //Strategy: For FSL dataset, we choose methods that have assertions, but not necessarily called by unit tests.
            //Strategy: For Evaluation dataset, we choose methods that are called by unit tests but do not necessarily have assertions
            //
            // We use call graphs to identify these two distinct properties
            // To identify if the method is called by unit tests (either directly or indirectly), we must get all test nodes in callgraph and get only those connected to the candidate method.
            // the methods that meets both conditions (having assertions + calling both conditions), are assigned to evaluation if they have enough number of tests. this enough number should be a dynamic number. otherwise, assign to FSL
            DatasetExtractor datasetExtractor = new DatasetExtractor(
                    Paths.get(Constants.PROJECT_ROOT_DIR, Constants.FILTERED_ORIGINAL_DOWNLOADED_REPOSITORIES_RESERVOIR_CSV_NAME).toString(),
                    Paths.get(Constants.PROJECT_ROOT_DIR, Constants.ORIGINAL_DOWNLOADED_REPOSITORIES_RESERVOIR_DIRECTORY_NAME).toAbsolutePath().toString(),
                    Constants.EVALUATION_DB,
                    Constants.FSL_DB,
                    Constants.FSL_FOR_FINE_TUNING_DB,
                    Constants.FINE_TUNING_TRAINING_DB,
                    Constants.FINE_TUNING_VALIDATION_DB);
            System.out.println("Datasets extracting...");
            try {
                datasetExtractor.extract();
            } catch (Exception e) {
                System.out.println("Failed!");
                throw new RuntimeException(e);
            }

            System.out.println("Dataset extracted! Details are as follows:");
            System.out.println("Evaluation dataset:");
            System.out.println("-----------------------");
            EvaluationDataset evaluationDataset = Utils.loadDataset(Constants.EVALUATION_DB, EvaluationDataset.class);
            assert evaluationDataset != null;
            System.out.println("#Records" + ": " + evaluationDataset.getRecordCount() + " " + "Methods.");
            System.out.println("Average Method Length" + ": " + evaluationDataset.calculateAverageMethodLength() + " " + "Lines.");
            System.out.println("Total Code Lines " + ": " + evaluationDataset.calculateTotalCodeLines() + " " + "Lines.");
            System.out.println("#Unique Repo size" + ": " + evaluationDataset.getUniqueRepoNames().size() + " " + "Repositories.");
            System.out.println("#Unique Repo Names" + ": " + evaluationDataset.getUniqueRepoNames() + " " + "Names.");

            FSLDataset fslDataset = Utils.loadDataset(Constants.FSL_DB, FSLDataset.class);
            assert fslDataset != null;
            System.out.println();
            System.out.println("FSL dataset:");
            System.out.println("-----------------------");
            System.out.println("#Records" + ": " + fslDataset.getRecordCount() + " " + "Methods.");
            System.out.println("Average Method Length" + ": " + fslDataset.calculateAverageMethodLength() + " " + "Lines.");
            System.out.println("Total Code Lines " + ": " + fslDataset.calculateTotalCodeLines() + " " + "Lines.");
            System.out.println("#Unique Repo size" + ": " + fslDataset.getUniqueRepoNames().size() + " " + "Repositories.");
            System.out.println("#Unique Repo Names" + ": " + fslDataset.getUniqueRepoNames() + " " + "Names.");

            FSLDataset fslForFinetuningDataset = Utils.loadDataset(Constants.FSL_FOR_FINE_TUNING_DB, FSLDataset.class);
            assert fslForFinetuningDataset != null;
            System.out.println();
            System.out.println("FSL for fine-tuning dataset:");
            System.out.println("-----------------------");
            System.out.println("#Records" + ": " + fslForFinetuningDataset.getRecordCount() + " " + "Methods.");
            System.out.println("Average Method Length" + ": " + fslForFinetuningDataset.calculateAverageMethodLength() + " " + "Lines.");
            System.out.println("Total Code Lines " + ": " + fslForFinetuningDataset.calculateTotalCodeLines() + " " + "Lines.");
            System.out.println("#Unique Repo size" + ": " + fslForFinetuningDataset.getUniqueRepoNames().size() + " " + "Repositories.");
            System.out.println("#Unique Repo Names" + ": " + fslForFinetuningDataset.getUniqueRepoNames() + " " + "Names.");


            EvaluationDataset fine_tuning_trainingDataset = Utils.loadDataset(Constants.FINE_TUNING_TRAINING_DB, EvaluationDataset.class);
            assert fine_tuning_trainingDataset != null;
            System.out.println("Fine-tuning training dataset:");
            System.out.println("-----------------------");
            System.out.println("#Records" + ": " + fine_tuning_trainingDataset.getRecordCount() + " " + "Methods.");
            System.out.println("Average Method Length" + ": " + fine_tuning_trainingDataset.calculateAverageMethodLength() + " " + "Lines.");
            System.out.println("Total Code Lines " + ": " + fine_tuning_trainingDataset.calculateTotalCodeLines() + " " + "Lines.");
            System.out.println("#Unique Repo size" + ": " + fine_tuning_trainingDataset.getUniqueRepoNames().size() + " " + "Repositories.");
            System.out.println("#Unique Repo Names" + ": " + fine_tuning_trainingDataset.getUniqueRepoNames() + " " + "Names.");

            EvaluationDataset fine_tuning_validationDataset = Utils.loadDataset(Constants.FINE_TUNING_VALIDATION_DB, EvaluationDataset.class);
            assert fine_tuning_validationDataset != null;
            System.out.println("Fine-tuning validation dataset:");
            System.out.println("-----------------------");
            System.out.println("#Records" + ": " + fine_tuning_validationDataset.getRecordCount() + " " + "Methods.");
            System.out.println("Average Method Length" + ": " + fine_tuning_validationDataset.calculateAverageMethodLength() + " " + "Lines.");
            System.out.println("Total Code Lines " + ": " + fine_tuning_validationDataset.calculateTotalCodeLines() + " " + "Lines.");
            System.out.println("#Unique Repo size" + ": " + fine_tuning_validationDataset.getUniqueRepoNames().size() + " " + "Repositories.");
            System.out.println("#Unique Repo Names" + ": " + fine_tuning_validationDataset.getUniqueRepoNames() + " " + "Names.");
        } else if (STAGE == Constants.Stage.DATASET_THRESHOLDS_CALCULATION) {
            ThresholdGenerator thresholdGenerator = new ThresholdGenerator(Constants.ORIGINAL_DOWNLOADED_REPOSITORIES_RESERVOIR_CSV_NAME, Paths.get(Constants.PROJECT_ROOT_DIR, Constants.ORIGINAL_DOWNLOADED_REPOSITORIES_RESERVOIR_DIRECTORY_NAME).toAbsolutePath().toString(), Constants.FILTERED_ORIGINAL_DOWNLOADED_REPOSITORIES_RESERVOIR_CSV_NAME);
            thresholdGenerator.extractAndCreateFilteredCSV();
            System.out.println();
            System.out.println("Thresholds extracted. Star threshold: " + thresholdGenerator.getStarThreshold() + " , Production assertion threshold: " + thresholdGenerator.getProductionAssertionThreshold());
            System.out.println("File Saved");
        } else {
            EvaluationDataset fine_tuning_EvaluationDataset = Utils.loadDataset(Constants.FINE_TUNING_TRAINING_DB, EvaluationDataset.class);
            fine_tuning_EvaluationDataset.create(
//                    Pair.of(Constants.Experiment_ID.A, Constants.FINE_TUNING_TRAINING_DB_JSONL_A),
//                    Pair.of(Constants.Experiment_ID.B, Constants.FINE_TUNING_TRAINING_DB_JSONL_B),
//                    Pair.of(Constants.Experiment_ID.C, Constants.FINE_TUNING_TRAINING_DB_JSONL_C),
                    Pair.of(Constants.Experiment_ID.D, Constants.FINE_TUNING_TRAINING_DB_JSONL_D)
            );

//            EvaluationDataset fine_tuning_ValidationDataset = Utils.loadDataset(Constants.FINE_TUNING_VALIDATION_DB, EvaluationDataset.class);
//            fine_tuning_ValidationDataset.create(
//                    Pair.of(Constants.Experiment_ID.A, Constants.FINE_TUNING_VALIDATION_DB_JSONL_A)
//                    Pair.of(Constants.Experiment_ID.B, Constants.FINE_TUNING_VALIDATION_DB_JSONL_B),
//                    Pair.of(Constants.Experiment_ID.C, Constants.FINE_TUNING_VALIDATION_DB_JSONL_C),
//                     Pair.of(Constants.Experiment_ID.D, Constants.FINE_TUNING_VALIDATION_DB_JSONL_D)
//            );
        }
        System.out.println("\nAssertion end date/time: "+Utils.getCurrentDateTime()+"\n");
    }
}