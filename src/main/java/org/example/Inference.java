package org.example;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import javassist.NotFoundException;
import org.codehaus.plexus.util.FileUtils;
import org.example.call_graph.CallGraphNode;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static org.example.Constants.*;

public class Inference {
    private static Instant lastRequestTime = Instant.MIN;
    private final LLM_Config config;
    private final int trial_number;

    public Inference(LLM_Config config, int trial_number) {
        this.config = config;
        this.trial_number = trial_number;
    }

    private static String addLineNumbersToMethod(String method) throws Exception {
        String withCurlyBracesMethod = Parser.addCurlyBraces(method);
        String[] lines = withCurlyBracesMethod.split("\n");
        StringBuilder numberedMethod = new StringBuilder();

        for (int i = 0; i < lines.length; i++) {
            numberedMethod.append(i + 1).append(". ").append(lines[i]).append("\n");
        }
        return numberedMethod.toString();
    }

    public static List<Pair<Integer, String>> extractAssertionsWithLineNumbers(String originalMethod) throws Exception {
        List<Pair<Integer, String>> assertionsListWithNumber = new ArrayList<>();

        String originalMethodWithCurlyBraces = Parser.addCurlyBraces(originalMethod);

        String[] lines = originalMethodWithCurlyBraces.split("\n");
        Pattern assertionPattern = Pattern.compile("\\s*assert\\s+.*;");
        int lineNumberCounter = 1;

        // Create an array to store line numbers
        int[] assignedLineNumbers = new int[lines.length];

        // First pass: Assign line numbers to non-assert lines
        for (int i = 0; i < lines.length; i++) {
            Matcher matcher = assertionPattern.matcher(lines[i]);
            if (!matcher.find()) {
                assignedLineNumbers[i] = lineNumberCounter++;
            }
        }

        // Second pass: Assign line numbers to assert lines
        for (int i = 0; i < lines.length; i++) {
            Matcher matcher = assertionPattern.matcher(lines[i]);
            if (matcher.find()) {
                // Find the next non-zero line number
                int nextLineNumber = 0;
                for (int j = i; j < lines.length; j++) {
                    if (assignedLineNumbers[j] != 0) {
                        nextLineNumber = assignedLineNumbers[j];
                        break;
                    }
                }
                assertionsListWithNumber.add(Pair.of(nextLineNumber, lines[i]));
            }
        }

        // Convert the list of line numbers to a comma-separated string
        return assertionsListWithNumber;
    }

    public static Inference getInferenceObject() {
        return new Inference(new LLM_Config(Constants.OPENAI_API_KEY, Constants.LLM.GPT_4O, 1.0, 1.0, 0, 0), LLM_REQUEST_TRIAL_NUMBER);//we use GPT-4o for method-summary and similar method extraction -related works (even if that fine-tuning dataset is going to be used for another model)
    }

    public LLM_Config getConfig() {
        return config;
    }

    public Pair<LLM_InputContent, String> askModel(LLM_InputContent command) throws Exception {
        String response = runScript(command, LLM_Operation.COMPLETION);
        response = purifyIfPossible(response);
        if (response.startsWith("Traceback") || response.startsWith("Exceeded maximum trials"))
            System.err.printf("Error while asking Completion!%n===> Command:%n%s%n===> Response: %s%n", command, response);

        return Pair.of(command, response);
    }

    public String purifyIfPossible(String response) {
        if (config.getModel().getName().toLowerCase().contains("gpt"))
            return response;
        if (config.getModel().getName().toLowerCase().contains("llama")) {
            try {
                return new JSONObject(response).optString("response", response);
            } catch (Exception e) {
                return response;
            }
        } else
            return response;
    }

    private String generatePythonModelRunnerCommand(LLM_Operation operation, String systemPromptFilePath, String userPromptFilePath, String assistantPromptFilePath) throws NotFoundException {
        String path;
        String modelName = switch (operation) {
            case EMBEDDING -> {
                path = config.getModel().getScript();
                yield config.getModel().getEmbeddingModelName();
            }
            case COMPLETION -> {
                path = config.getModel().getScript();
                yield config.getModel().getCompletionModelName();
            }
            default -> throw new NotFoundException("LLM Operation cannot be found");
        };

        double temperature = config.getTemperature();
        double topP = config.getTop_p();
        String apiKey = config.getApiKey();
        double frequencyPenalty = config.getFrequency_penalty();
        double presencePenalty = config.getPresence_penalty();
        // Set environment variables
        String envVars = "export MODEL=" + modelName + " && " +
                "export ASSERTION_TYPE=" + (operation.equals(LLM_Operation.EMBEDDING) ? "embedding" : "generate") + " && " +
                "export SYSTEM_PATH=" + systemPromptFilePath + " && " +
                "export USER_PATH=" + userPromptFilePath + " && " +
                "export ASSISTANT_PATH=" + assistantPromptFilePath + " && " +
                "export API_KEY=" + apiKey + " && " +
                "export URL=" + config.getModel().getUrl() + " && " +
                "export TRIAL_NUMBER=" + trial_number + " && " +
                "export TEMPERATURE=" + temperature + " && " +
                "export TOP_P=" + topP + " && " +
                "export FREQUENCY_PENALTY=" + frequencyPenalty + " && " +
                "export PRESENCE_PENALTY=" + presencePenalty + " && " +
                "export MAX_LENGTH=" + Constants.MAX_LLM_RESPONSE_LENGTH;

        // Construct the command without exposing environment variables
        String command = "source ./env/bin/activate && python3 " + path;

        // Return the full command with environment variables
        return envVars + " && " + command;
    }

    private String generateBatchEmbeddingPythonModelRunnerCommand(String systemPromptFilePath) {
        String path;
        String modelName;
        path = config.getModel().getScript();
        modelName = config.getModel().getEmbeddingModelName();
        double temperature = config.getTemperature();
        double topP = config.getTop_p();
        String apiKey = config.getApiKey();
        double frequencyPenalty = config.getFrequency_penalty();
        double presencePenalty = config.getPresence_penalty();

        // Set environment variables
        String envVars = String.format(
                "export MODEL=%s && " +
                        "export ASSERTION_TYPE=embedding && " +
                        "export IS_BATCH=true && " +
                        "export SYSTEM_PATH=%s && " +
                        "export API_KEY=%s && " +
                        "export TRIAL_NUMBER=%d && " +
                        "export MRDS=%d && " +
                        "export TEMPERATURE=%f && " +
                        "export TOP_P=%f && " +
                        "export FREQUENCY_PENALTY=%f && " +
                        "export PRESENCE_PENALTY=%f && " +
                        "export MAX_LENGTH=%d",
                modelName, systemPromptFilePath, apiKey, trial_number, MINIMUM_REQUEST_DELAY_SECONDS, temperature, topP,
                frequencyPenalty, presencePenalty, Constants.MAX_LLM_RESPONSE_LENGTH
        );

        // Construct the command for script execution
        String command = String.format("source ./env/bin/activate && python %s", path);

        // Combine environment variables with the command
        return envVars + " && " + command;
    }

    private String runScript(LLM_InputContent input, LLM_Operation operation) throws Exception {
        Instant currentTime = Instant.now();
        Duration timeSinceLastRequest = Duration.between(lastRequestTime, currentTime);
        long secondsSinceLastRequest = timeSinceLastRequest.getSeconds();

        if (secondsSinceLastRequest >= MINIMUM_REQUEST_DELAY_SECONDS) {
            lastRequestTime = currentTime;
            return _runScript(input, operation);
        } else {
            long remainingSeconds = MINIMUM_REQUEST_DELAY_SECONDS - secondsSinceLastRequest;
            Thread.sleep(remainingSeconds * 1000);
            lastRequestTime = Instant.now();
            return _runScript(input, operation);
        }
    }

    private String runBatchEmbeddingScript(List<LLM_InputContent> inputs) throws Exception {
        Instant currentTime = Instant.now();
        Duration timeSinceLastRequest = Duration.between(lastRequestTime, currentTime);
        long secondsSinceLastRequest = timeSinceLastRequest.getSeconds();

        if (secondsSinceLastRequest >= MINIMUM_REQUEST_DELAY_SECONDS) {
            lastRequestTime = currentTime;
        } else {
            long remainingSeconds = MINIMUM_REQUEST_DELAY_SECONDS - secondsSinceLastRequest;
            Thread.sleep(remainingSeconds * 1000);
            lastRequestTime = Instant.now();
        }
        return _runBatchEmbeddingScript(inputs);
    }

    private String _runScript(LLM_InputContent input, LLM_Operation operation) throws Exception {
        Gson gson = new GsonBuilder().disableHtmlEscaping().create();
        String userPromptFilePath = Paths.get(Constants.PROJECT_ROOT_DIR, PYTHON_SCRIPT_DIR, Constants.PYTHON_SCRIPT_MODELS_DIR, Constants.USER_PROMPT_FILE).toString();
        File userPromptFile = new File(userPromptFilePath);
        String systemPromptFilePath = Paths.get(Constants.PROJECT_ROOT_DIR, PYTHON_SCRIPT_DIR, Constants.PYTHON_SCRIPT_MODELS_DIR, Constants.SYSTEM_PROMPT_FILE).toString();
        File systemPromptFile = new File(systemPromptFilePath);
        String assistantPromptFilePath = Paths.get(Constants.PROJECT_ROOT_DIR, PYTHON_SCRIPT_DIR, Constants.PYTHON_SCRIPT_MODELS_DIR, Constants.ASSISTANT_PROMPT_FILE).toString();
        File assistantPromptFile = new File(assistantPromptFilePath);
        if (systemPromptFile.exists())
            systemPromptFile.delete();
        if (userPromptFile.exists())
            userPromptFile.delete();
        if (assistantPromptFile.exists())
            assistantPromptFile.delete();

        try {
            if (input.getUser() != null && !input.getUser().isEmpty()) {
                FileUtils.fileWrite(new File(userPromptFilePath), "UTF-8", gson.toJson(new TransitPrompt(gson.toJson(input.getUser()))));
            }

            if (input.getSystem() != null) {
                FileUtils.fileWrite(new File(systemPromptFilePath), "UTF-8", gson.toJson(new TransitPrompt(input.getSystem())));
            }

            if (input.getAssistant() != null && !input.getAssistant().isEmpty()) {
                FileUtils.fileWrite(new File(assistantPromptFilePath), "UTF-8", gson.toJson(new TransitPrompt(gson.toJson(input.getAssistant()))));
            }

            String[] terminalCommand = {"/bin/bash", "-c", generatePythonModelRunnerCommand(operation, systemPromptFilePath, userPromptFilePath, assistantPromptFilePath)};
            ProcessBuilder processBuilder = new ProcessBuilder(terminalCommand);
            processBuilder.directory(new File(Paths.get(Constants.PROJECT_ROOT_DIR, PYTHON_SCRIPT_DIR, Constants.PYTHON_SCRIPT_MODELS_DIR).toString()));
            processBuilder.environment().putAll(System.getenv());
            processBuilder.redirectErrorStream(true);
            Process process = processBuilder.start();
            InputStream inputStream = process.getInputStream();
            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
            String line;
            StringBuilder response = new StringBuilder();
            while ((line = reader.readLine()) != null)
                response.append(line).append("\n");

            return response.toString().trim();
        } finally {
            if (systemPromptFile.exists())
                systemPromptFile.delete();
            if (userPromptFile.exists())
                userPromptFile.delete();
            if (assistantPromptFile.exists())
                assistantPromptFile.delete();
        }
    }

    private String _runBatchEmbeddingScript(List<LLM_InputContent> inputs) throws Exception {
        Gson gson = new GsonBuilder().disableHtmlEscaping().create();
        String systemPromptFilePath = Paths.get(Constants.PROJECT_ROOT_DIR, PYTHON_SCRIPT_DIR, Constants.PYTHON_SCRIPT_MODELS_DIR, Constants.SYSTEM_PROMPT_FILE).toString();
        File systemPromptFile = new File(systemPromptFilePath);
        try {
            FileUtils.fileWrite(new File(systemPromptFilePath), "UTF-8",
                    gson.toJson(new TransitPrompt(gson.toJson(
                            inputs.stream().map(LLM_InputContent::getSystem).collect(Collectors.toList())
                    )))
            );

            String[] terminalCommand = {"/bin/bash", "-c", generateBatchEmbeddingPythonModelRunnerCommand(systemPromptFilePath)};
            ProcessBuilder processBuilder = new ProcessBuilder(terminalCommand);
            processBuilder.directory(new File(Paths.get(Constants.PROJECT_ROOT_DIR, PYTHON_SCRIPT_DIR, Constants.PYTHON_SCRIPT_MODELS_DIR).toString()));
            processBuilder.environment().putAll(System.getenv());
            processBuilder.redirectErrorStream(true);
            Process process = processBuilder.start();
            InputStream inputStream = process.getInputStream();
            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
            String line;
            StringBuilder response = new StringBuilder();
            while ((line = reader.readLine()) != null)
                response.append(line).append("\n");
            return response.toString().trim();
        } finally {
            if (systemPromptFile.exists())
                systemPromptFile.delete();
        }
    }

    public List<Double> calculateEmbeddings(String input) throws Exception {
        LLM_InputContent command = new LLM_InputContent(null, input, null);
        List<Double> list = new LinkedList<>();
        try {
            String response = runScript(command, LLM_Operation.EMBEDDING);
            list.addAll(new Gson().fromJson(response, List.class));
            return list;
        } catch (Exception e) {
            return new LinkedList<>();
        }
    }

    public List<List<Double>> calculateEmbeddingsForBatchInputs(List<LLM_InputContent> commands) throws Exception {
        List<List<Double>> list = new ArrayList<>();
        String response = runBatchEmbeddingScript(commands);
        list.addAll(new Gson().fromJson(response, List.class));

        return list;
    }

    public Pair<LLM_InputContent, String> inference(Experiment_ID experimentId, Map<String, String> similarMethods, Record record, MethodSpecificInfo methodSpecificInfo) throws Exception {
        switch (experimentId) {
            case A:
                return askModel(generateLLMInputContentForExperiment_A(record));
            case B:
                return askModel(generateLLMInputContentForExperiment_B(record, methodSpecificInfo));
            case C:
                return askModel(generateLLMInputContentForExperiment_C(record, methodSpecificInfo));
            case D:
                return askModel(generateLLMInputContentForExperiment_D(record, methodSpecificInfo, similarMethods));
            default:
                return null;
        }
    }

    public LLM_InputContent generateLLMInputContentForExperiment_D(Record record, MethodSpecificInfo methodSpecificInfo, Map<String, String> similarMethods) throws Exception {
        System.out.println("Similar examples size: " + similarMethods.size());
        LLM_InputContent content = generateLLMInputContentForExperiment_C(record, methodSpecificInfo);
        boolean firstSample = true;
        for (Map.Entry<String, String> entry : similarMethods.entrySet()) {
            String toAppend = "";
            String methodWithoutAssertion = entry.getKey();
            String methodWithAssertion = entry.getValue();
//                    String delimitedMethodWithoutAssertion = LLM_USER_INPUT_METHOD_DELIMITER + "\n" + methodWithoutAssertion + "\n" + LLM_USER_INPUT_METHOD_DELIMITER;
//                    String delimitedMethodWithAssertion = LLM_ASSISTANT_DELIMITER[0] + "\n" + methodWithAssertion + "\n" + LLM_ASSISTANT_DELIMITER[1];


            String methodWithoutAssertion_lineNumbered = addLineNumbersToMethod(methodWithoutAssertion);
            List<Pair<Integer, String>> assistantItemsWithLines = extractAssertionsWithLineNumbers(methodWithAssertion);
            StringBuilder assertionsResponse = new StringBuilder();
            for (Pair<Integer, String> pair : assistantItemsWithLines) {
                assertionsResponse.append(String.format("%s(%d, %s)%s", LLM_ASSISTANT_DELIMITER[0], pair.getFirst(), pair.getSecond().trim(), LLM_ASSISTANT_DELIMITER[1]));
            }
            String assertionsResponseStr = assertionsResponse.toString().trim();
            toAppend += "\n\nInput: " + LLM_USER_INPUT_METHOD_DELIMITER + "\n" + methodWithoutAssertion_lineNumbered + LLM_USER_INPUT_METHOD_DELIMITER + "\n\nOutput: " + assertionsResponseStr + " \n-----------------\n";
            int systemLength = Utils.computeNumberOfTokens(content.getSystem());
            int userLength = Utils.computeNumberOfTokens(content.getUser().get(0) + (firstSample ? "\nSimilar Examples:\n\n" : "") + toAppend.trim());
            if (userLength + systemLength < Utils.getMaxTPMLengthOfModel(this.getConfig().getModel())) {
                if (firstSample) {
                    content.getUser().set(0, content.getUser().get(0) + "\n\nSimilar Examples:\n");
                    firstSample = false;
                }
                content.getUser().set(0, content.getUser().get(0) + "\n" + toAppend.trim());
            }
        }

        return content;
    }

    public LLM_InputContent generateLLMInputContentForExperiment_C(Record record, MethodSpecificInfo methodSpecificInfo) throws Exception {
        List<String> usersSet = new LinkedList<>();
        String command = String.format("The method for which you will generate assertions has the following characteristic(s):%n" +
                        "* Name: \"%s\",%n" +
                        "* Signature: \"%s\"%n" +
                        "* Purpose: \"%s\"%n" +
                        "* External dependencies: %n%s%n" +
                        "* Method declaration: %n%n%s%n%s%s",
                record.findResolvedMethodDeclaration().getNameAsString(),
                record.findResolvedMethodDeclaration().getDeclarationAsString(),
                methodSpecificInfo.getSummary(),
                methodSpecificInfo.getDependenciesSummaries().isEmpty() ? "The method does not have any external dependencies." : methodSpecificInfo.getInvokedMethodsDescriptionAsString(),
                LLM_USER_INPUT_METHOD_DELIMITER,
                addLineNumbersToMethod(Parser.addCurlyBraces(record.printPureMethod())),

                LLM_USER_INPUT_METHOD_DELIMITER);

        usersSet.add(command);
        return new LLM_InputContent(usersSet, LLM_SYSTEM_MESSAGE);
    }

    public LLM_InputContent generateLLMInputContentForExperiment_B(Record record, MethodSpecificInfo methodSpecificInfo) throws Exception {
        List<String> usersSet = new LinkedList<>();
        String command = String.format("The method for which you will generate assertions has the following characteristic(s):%n" +
                        "* Name: \"%s\",%n" +
                        "* Signature: \"%s\"%n" +
                        "* Purpose: \"%s\"%n" +
                        "* Method declaration: %n%n%s%n%s%s",
                record.findResolvedMethodDeclaration().getNameAsString(),
                record.findResolvedMethodDeclaration().getDeclarationAsString(),
                methodSpecificInfo.getSummary(),
                LLM_USER_INPUT_METHOD_DELIMITER,
                addLineNumbersToMethod(Parser.addCurlyBraces(record.printPureMethod())),
                LLM_USER_INPUT_METHOD_DELIMITER);

        usersSet.add(command);
        return new LLM_InputContent(usersSet, LLM_SYSTEM_MESSAGE);
    }

    public LLM_InputContent generateLLMInputContentForExperiment_A(Record record) throws Exception {
        List<String> usersSet = new LinkedList<>();
        String command = String.format("The method for which you will generate assertions has the following characteristic(s):%n" +
                        "* Name: \"%s\",%n" +
                        "* Signature: \"%s\"%n" +
                        "* Method declaration: %n%n%s%n%s%s",
                record.findResolvedMethodDeclaration().getNameAsString(),
                record.findResolvedMethodDeclaration().getDeclarationAsString(),
                LLM_USER_INPUT_METHOD_DELIMITER,
                addLineNumbersToMethod(Parser.addCurlyBraces(record.printPureMethod())),
                LLM_USER_INPUT_METHOD_DELIMITER);

        usersSet.add(command);
        return new LLM_InputContent(usersSet, LLM_SYSTEM_MESSAGE);
    }

    private List<String> getTreeSetWithoutNotNullElements(Collection<String> values) {
        List<String> set = new LinkedList<>();
        for (String str : values)
            if (str != null)
                set.add(str);
        return set;
    }

    public LLM_InputContent generateFineTuningLLMInputContent(Record record, Experiment_ID experimentId, FSLDataset fslDataset) throws Exception {
        if (experimentId == Experiment_ID.D)
            assert fslDataset != null;

        MethodSpecificInfoExtractor methodSpecificInfoExtractor = new MethodSpecificInfoExtractor();
        PreEvaluator preEvaluator = new PreEvaluator();
        Parser parser = new Parser(record);
        CallGraphNode recordNode = parser.computeCallGraph(false);

        if (!parser.hasCallGraph())
            throw new Exception("FINE-TUNING - CALL GRAPH IS EMPTY");

        if (!preEvaluator.evaluate(parser, false, false)) {
            throw new Exception("FINE-TUNING - PRE-EVALUATION FAILED");
        }

        record.setHasAnyAssertions(recordNode.hasAnyAssertion());

        LLM_InputContent assistantLessContent = null;
        if (experimentId == Experiment_ID.A) {
            assistantLessContent = generateLLMInputContentForExperiment_A(record);
        } else if (experimentId == Experiment_ID.B) {
            assistantLessContent = generateLLMInputContentForExperiment_B(record, methodSpecificInfoExtractor.extract(parser, this));
        } else if (experimentId == Experiment_ID.C) {
            assistantLessContent = generateLLMInputContentForExperiment_C(record, methodSpecificInfoExtractor.extract(parser, this));
        } else if (experimentId == Experiment_ID.D) {
            SimilarRecordsExtractor similarRecordsExtractor = new SimilarRecordsExtractor(Constants.IN_CONTEXT_LEARNING_NUMBER_OF_SAMPLES, Constants.FEW_SHOT_LEARNING_COSINE_THRESHOLD);
            similarRecordsExtractor.setFSLDataset(fslDataset);
            Map<String, String> similarMethods = similarRecordsExtractor.getSimilarMethods(parser, experimentId, this);
            assistantLessContent = generateLLMInputContentForExperiment_D(record, methodSpecificInfoExtractor.extract(parser, this), similarMethods);
        }

        if (assistantLessContent != null) {
            //extract expected answer and add it to assistantLessContent
            List<Pair<Integer, String>> assistantItemsWithLines = extractAssertionsWithLineNumbers(record.printMethod(true, false, true));
            StringBuilder assertionsResponse = new StringBuilder();
            for (Pair<Integer, String> pair : assistantItemsWithLines) {
                assertionsResponse.append(String.format("%s(%d, %s)%s", LLM_ASSISTANT_DELIMITER[0], pair.getFirst(), pair.getSecond().trim(), LLM_ASSISTANT_DELIMITER[1]));
            }
            String assertionsResponseStr = assertionsResponse.toString().trim();
            List<String> assistants = new LinkedList<>();
            assistants.add(assertionsResponseStr);
            assistantLessContent.setAssistant(assistants);

            return assistantLessContent;
        }

        return null;
    }

    public enum LLM_Operation {
        COMPLETION, EMBEDDING;
    }
}
