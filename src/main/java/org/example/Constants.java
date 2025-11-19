package org.example;

import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

public class Constants {
    public static final Integer IN_CONTEXT_LEARNING_NUMBER_OF_SAMPLES = 3;
    public static final double FEW_SHOT_LEARNING_COSINE_THRESHOLD = 0.5;
    public static final String PYTHON_SCRIPT_DIR = "python_scripts";
    public static final String PYTHON_SCRIPT_MODELS_DIR = "models";
    public static final String USER_PROMPT_FILE = "user.txt";
    public static final String SYSTEM_PROMPT_FILE = "system.txt";
    public static final String ASSISTANT_PROMPT_FILE = "assistant.txt";
    public static final String METHOD_SUMMARIZER_USER_FIELD = "\"Summarize what this Java method is doing by describing its objective preferably in one paragraph:\n\n";
    public static final Experiment_ID[] EXPERIMENTS_ID = {
            Experiment_ID.A
//            Experiment_ID.B
//            Experiment_ID.C
//            Experiment_ID.D
    };
    public static final LLM[] LLMs_ID = {
//            LLM.GPT_4O,
//            LLM.GPT_4O_FINE_TUNED_ON_A,
//            LLM.GPT_4O_FINE_TUNED_ON_B,
//            LLM.GPT_4O_FINE_TUNED_ON_C,
//            LLM.GPT_4O_FINE_TUNED_ON_D,
//            LLM.LLAMA_2,
//            LLM.LLAMA_2_FINE_TUNED_ON_A,
//            LLM.LLAMA_2_FINE_TUNED_ON_B
            LLM.LLAMA_2_FINE_TUNED_ON_C,
//            LLM.LLAMA_2_FINE_TUNED_ON_D,
    };
    public static final int API_KEY_ASTERISK_MARGIN = 7;
    public static final String OPENAI_API_KEY = "API KEY COMES HERE"; // research organization
    public static final String GITHUB_API_KEY = "API KEY COMES HERE";
    public static final int LLM_REQUEST_TRIAL_NUMBER = 10;
    public static final int MINIMUM_REQUEST_DELAY_SECONDS = 3;
    public static final int MAX_LLM_RESPONSE_LENGTH = 100_000;
    public static final String LLM_USER_INPUT_METHOD_DELIMITER = "\"\"\"";
    public static final String[] LLM_ASSISTANT_DELIMITER = {"<JAVA>", "</JAVA>"};
    public static final String PYTHON_TOKENIZER_TEMP_FILE = "tokenizer_tmp.txt";
    public static final String PYTHON_TOKENIZER_ENCODER = "cl100k_base";
    public static final String PYTHON_TOKENIZER_FILE = "tokenizer.py";
    public static final String PYTHON_HUGGINGFACE_FILE = "huggingface_scores.py";
    public static final int EMBEDDING_BATCH_SIZE = 50;
    public static final String LLM_SYSTEM_MESSAGE =
            String.format("You are an expert in generating Java standard assertions. Your task is to insert assertions into a given method, ensuring the method’s correct behavior while keeping it compilable. Follow these instructions carefully:\n" +
                            "Input Method: A Java method will be provided, delimited by triple quotes (%s), where each line is numbered (starting from 1).\n" +
                            "Task: Insert Java standard assertions along with lines at which the assertion must be placed. Do not generate JUnit assertions. The remaining method lines after placing the inferred assertion will shift down by one to accommodate the assertion.\n" +
                            "Expectation: The purpose of these assertions must be to help developers comprehend the code by highlighting key assumptions, invariants, and expected program states. \n" +
                            "Assertions should be placed at locations where they improve readability and understanding of the method’s logic.\n" +
                            "They should not be added arbitrarily or excessively—only where they clarify intended behavior.\n" +
                            "Constraints:\n" +
                            "Generate only Java standard assertions. Avoid using any undefined methods, variable, or symbols in the project.\n" +
                            "The assertions must use only variables, method or classes defined before the predicted line, ensuring the code remains compilable.\n" +
                            "Do not generate a new method definition., only focus on generating assertion and line number pairs.\n" +
                            "Do not generate assertions that require importing additional classes.\n" +
                            "Assertions must not alter the behavior of the method but validate the expected program state and program behavior at that program location.\n" +
                            "Output Format:\n" +
                            "Provide output in pairs of assertions and line numbers.\n" +
                            "Each pair must be encapsulated within %s and %s tags, formatted as (line_number, assertion). For instance: %s(3, assert a < 3;)%s, %s(5, assert a.getAge() == 4;)%s.\n" +
                            "Exclude all descriptions, explanations, or any additional code (e.g., method structure or import statements). Only return the assertion and line number pairs.",
                    LLM_USER_INPUT_METHOD_DELIMITER, LLM_ASSISTANT_DELIMITER[0], LLM_ASSISTANT_DELIMITER[1],
                    LLM_ASSISTANT_DELIMITER[0], LLM_ASSISTANT_DELIMITER[1], LLM_ASSISTANT_DELIMITER[0], LLM_ASSISTANT_DELIMITER[1]);
    public static final String JAVA_HOME_VERSION_8 = Paths.get("/Library", "Java", "JavaVirtualMachines", "openlogic-openjdk-8.jdk", "Contents", "Home").toString(); //TODO: ADJUST THIS ACCORDING TO THE TOOL'S JAVA ADDRESS
    public static final String JAVA_HOME_VERSION_17 = Paths.get("/Library", "Java", "JavaVirtualMachines", "openlogic-openjdk-17.jdk", "Contents", "Home").toString();//TODO: ADJUST THIS ACCORDING TO THE TOOL'S JAVA ADDRESS
    public static final String JAVA_HOME_VERSION_21 = Paths.get("/Library", "Java", "JavaVirtualMachines", "openlogic-openjdk-21.jdk", "Contents", "Home").toString();//TODO: ADJUST THIS ACCORDING TO THE TOOL'S JAVA ADDRESS
    public static final String JAVA_HOME_VERSION_22 = Paths.get("/Library", "Java", "JavaVirtualMachines", "jdk-22.0.2.jdk", "Contents", "Home").toString();//TODO: ADJUST THIS ACCORDING TO THE TOOL'S JAVA ADDRESS
    public static final String JAVA_HOME_VERSION_23 = Paths.get("/Library", "Java", "JavaVirtualMachines", "jdk-23.jdk", "Contents", "Home").toString();//TODO: ADJUST THIS ACCORDING TO THE TOOL'S JAVA ADDRESS
    public static final String[] JAVA_HOME_VERSIONS =
            {JAVA_HOME_VERSION_23, JAVA_HOME_VERSION_17, JAVA_HOME_VERSION_8, JAVA_HOME_VERSION_21};
    public static final String MVN_BINARY_PATH = "mvn";
    public static final Map<String, String> REPO_JAVA_ADDRESS_MAP = new HashMap<>();
    public static final String ASSERTRON_COVERAGE_FLAG_MESSAGE = "!@# ASSERTRON - 2025 #@!";
    public static final int MAXIMUM_NUMBER_OF_UNIT_TESTS_PER_RECORD = 10;
    public static final int STAR_THRESHOLD_PERCENTILE = 50;
    public static final int EVALUATION_DATASET_UPPER_LIMIT_SIZE = 100;
    public static final int MAX_CALL_GRAPH_NODES_SIZE = 50_000;
    public static final String REFERENCE_SUMMARY_FILE = "reference_summary.txt";
    public static final String CANDIDATE_SUMMARY_FILE = "candidate_summary.txt";
    public static Integer EXECUTION_REPEAT_COUNTER = 3;
    public static Integer GPT_4O_MAX_TOKENS = 8192;
    public static Integer PERCENTAGE_FINE_TUNINIG_TRAINING = 80;
    public static Integer PERCENTAGE_FINE_TUNINIG_VALIDATION = 10;
    public static Integer PERCENTAGE_FINE_TUNINIG_FSL = 5;
    public static Integer PERCENTAGE_PIPELINE_FSL = 5;
    public static String PROJECT_ROOT_DIR = Paths.get(Paths.get(".").toString()).toAbsolutePath().normalize().toString();
    public static String EVALUATION_DB = Paths.get(PROJECT_ROOT_DIR, "eval_dataset.json").toString();
    public static String FSL_DB = Paths.get(PROJECT_ROOT_DIR, "fsl_dataset.json").toString();
    public static String FSL_FOR_FINE_TUNING_DB = Paths.get(PROJECT_ROOT_DIR, "fsl_for_ft_dataset.json").toString();
    public static String FINE_TUNING_TRAINING_DB = Paths.get(PROJECT_ROOT_DIR, "ft_training_dataset.json").toString();
    public static String FINE_TUNING_VALIDATION_DB = Paths.get(PROJECT_ROOT_DIR, "ft_validation_dataset.json").toString();

    public static String FINE_TUNING_TRAINING_DB_JSONL_A = Paths.get(PROJECT_ROOT_DIR, "ft_training_dataset_A.jsonl").toString();
    public static String FINE_TUNING_TRAINING_DB_JSONL_B = Paths.get(PROJECT_ROOT_DIR, "ft_training_dataset_B.jsonl").toString();
    public static String FINE_TUNING_TRAINING_DB_JSONL_C = Paths.get(PROJECT_ROOT_DIR, "ft_training_dataset_C.jsonl").toString();
    public static String FINE_TUNING_TRAINING_DB_JSONL_D = Paths.get(PROJECT_ROOT_DIR, "ft_training_dataset_D.jsonl").toString();

    public static String FINE_TUNING_VALIDATION_DB_JSONL_A = Paths.get(PROJECT_ROOT_DIR, "ft_validation_dataset_A.jsonl").toString();
    public static String FINE_TUNING_VALIDATION_DB_JSONL_B = Paths.get(PROJECT_ROOT_DIR, "ft_validation_dataset_B.jsonl").toString();
    public static String FINE_TUNING_VALIDATION_DB_JSONL_C = Paths.get(PROJECT_ROOT_DIR, "ft_validation_dataset_C.jsonl").toString();
    public static String FINE_TUNING_VALIDATION_DB_JSONL_D = Paths.get(PROJECT_ROOT_DIR, "ft_validation_dataset_D.jsonl").toString();

    public static String OUTPUT_FILE = "output.json";
    public static String AVERAGE_OUTPUT_FILE = "average_output.json";
    public static String EXPERIMENTS_DIRECTORY_NAME = "experiments";
    public static String REPOSITORIES_DIRECTORY_NAME = "repositories";
    public static String STAR_TMP_CSV = "star_tmp.csv";
    public static String ORIGINAL_DOWNLOADED_REPOSITORIES_RESERVOIR_CSV_NAME = Paths.get(Constants.PROJECT_ROOT_DIR,"CSV filter script","output_successful_csv_records.csv").toString();
    public static String FILTERED_ORIGINAL_DOWNLOADED_REPOSITORIES_RESERVOIR_CSV_NAME = "filtered_input_source.csv";
    public static String ORIGINAL_DOWNLOADED_REPOSITORIES_RESERVOIR_DIRECTORY_NAME = "original_repositories";
    public static String DATE_TIME_FORMAT = "yyyy_MM_dd_HH_mm";

    public enum Stage {
        DATASET_THRESHOLDS_CALCULATION,
        DATASET_GENERATION,
        PIPELINE,
        FINE_TUNING_DATASET_EXTRACTOR
    }

    public enum Experiment_ID {
        A, B, C, D
    }

    public static enum LLM {
        GPT_4O("GPT-4O", "gpt-4o", "text-embedding-3-small", "openai_script.py"),
        GPT_4O_FINE_TUNED_ON_A("GPT-4O-FT_A", "gpt-4o", "text-embedding-3-small", "openai_script.py"),
        GPT_4O_FINE_TUNED_ON_B("GPT-4O-FT_B", "gpt-4o", "text-embedding-3-small", "openai_script.py"),
        GPT_4O_FINE_TUNED_ON_C("GPT-4O-FT_C", "gpt-4o", "text-embedding-3-small", "openai_script.py"),
        GPT_4O_FINE_TUNED_ON_D("GPT-4O-FT_D", "gpt-4o", "text-embedding-3-small", "openai_script.py"),
        LLAMA_2("LLAMA-2", "llama-2", "text-embedding-3-small", "meta_script.py"),
        LLAMA_2_FINE_TUNED_ON_A("LLAMA-2-FT-A", "llama-2-ft-a", "text-embedding-3-small", "meta_script.py","URL COMES HERE"),
        LLAMA_2_FINE_TUNED_ON_B("LLAMA-2-FT-B", "llama-2-ft-b", "text-embedding-3-small", "meta_script.py","URL COMES HERE"),//*
        LLAMA_2_FINE_TUNED_ON_C("LLAMA-2-FT-C", "llama-2-ft-c", "text-embedding-3-small", "meta_script.py","URL COMES HERE"),
        LLAMA_2_FINE_TUNED_ON_D("LLAMA-2-FT-D", "llama-2-ft-d", "text-embedding-3-small", "meta_script.py","URL COMES HERE");

        private final String name;
        private final String completionModelName;
        private final String embeddingModelName;
        private final String script;
        private final String url;

        LLM(String name, String completionModelName, String embeddingModelName, String script) {
            this(name, completionModelName, embeddingModelName, script, null);
        }

        LLM(String name, String completionModelName, String embeddingModelName, String script, String url) {
            this.name = name;
            this.completionModelName = completionModelName;
            this.embeddingModelName = embeddingModelName;
            this.script = script;
            this.url = url;
        }

        public String getName() {
            return name;
        }

        public String getScript() {
            return script;
        }

        public String getCompletionModelName() {
            return completionModelName;
        }

        public String getEmbeddingModelName() {
            return embeddingModelName;
        }

        public String getUrl() {
            return url;
        }

        @Override
        public String toString() {
            return "LLM{" +
                    "name='" + name + '\'' +
                    "completionModelName='" + completionModelName + '\'' +
                    ", embeddingModelName='" + embeddingModelName + '\'' +
                    ", script='" + script + '\'' +
                    ", url='" + url + '\'' +
                    '}';
        }
    }

}
