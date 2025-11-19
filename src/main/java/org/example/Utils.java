package org.example;

import com.fasterxml.jackson.core.JsonEncoding;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.introspect.AnnotatedMember;
import com.fasterxml.jackson.databind.introspect.JacksonAnnotationIntrospector;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.javaparser.ParserConfiguration;
import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.stmt.AssertStmt;
import com.google.common.reflect.TypeToken;
import com.google.gson.*;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.codehaus.plexus.util.FileUtils;
import org.example.call_graph.CallGraphNode;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.*;
import java.lang.reflect.Field;
import java.lang.reflect.Type;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Utils {
    public static void configureJavaParser() {
        ParserConfiguration configuration = new ParserConfiguration()
                .setLanguageLevel(ParserConfiguration.LanguageLevel.JAVA_14);
        StaticJavaParser.setConfiguration(configuration);
    }

    public static <T extends Dataset> T loadDataset(String datasetPath, Class<T> datasetClass) {
        Gson gson = new Gson();

        try (FileReader reader = new FileReader(datasetPath)) {
            // Define the type for a list of Method objects
            Type methodListType = new TypeToken<List<Record>>() {
            }.getType();

            // Parse the JSON array into a list of Method objects
            List<Record> records = gson.fromJson(reader, methodListType);

            // Create an instance of the Dataset (EvaluationDataset or FSLDataset)
            T dataset = datasetClass.getDeclaredConstructor().newInstance();

            dataset.setPath(datasetPath);

            // Set the methods in the dataset
            dataset.setRecords(records);

            System.out.println(datasetClass.getSimpleName() + " dataset loaded! raw size: " + records.size());

            return dataset;
        } catch (Exception e) {
            System.err.println("Error loading dataset: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    public static void cleanFSLJsonFile(String filePath) throws IOException {
        String content = new String(Files.readAllBytes(Paths.get(filePath)));

        // Parse JSON array
        JSONArray jsonArray = new JSONArray(content);
        JSONArray filteredArray = new JSONArray();

        // Filter elements
        for (int i = 0; i < jsonArray.length(); i++) {
            JSONObject obj = jsonArray.getJSONObject(i);

            // Check if "embeddingVector" exists and is a valid JSONArray with elements
            if (obj.has("embeddingVector") && obj.get("embeddingVector") instanceof JSONArray) {
                JSONArray embeddingVector = obj.getJSONArray("embeddingVector");
                if (embeddingVector.length() > 0) {
                    filteredArray.put(obj);
                }
            }
        }


        // Write back to the file
        Files.write(Paths.get(filePath), filteredArray.toString(4).getBytes());
    }

    public static boolean deleteDirectory(String directoryPath) {
        System.out.println("Deleting directory...");

        File directory = new File(directoryPath);

        if (!directory.exists()) {
            System.out.println("Directory does not exist.");
            return false;
        }

        if (!directory.isDirectory()) {
            System.out.println("Provided path is not a directory.");
            return false;
        }

        // Recursively delete the contents of the directory
        deleteContents(directory);

        // Delete the directory itself
        return directory.delete();
    }

    public static String extractRepoOwner(String repoUrl) {
        // Pattern to capture the owner from the GitHub repository URL
        String regex = "https?://github.com/([^/]+)/([^/]+)";
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(repoUrl);

        if (matcher.find()) {
            // Capture and return the first group (owner)
            return matcher.group(1);
        } else {
            throw new IllegalArgumentException("Invalid GitHub repository URL.");
        }
    }

    public static boolean createDirIfNotExists(String dirName) {
        File file = new File(dirName);
        if (!file.exists()) {
            return file.mkdir();
        }
        return true;
    }

    private static void deleteContents(File directory) {
        File[] files = directory.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isDirectory()) {
                    deleteContents(file); // Recursively delete subdirectory
                }
                file.delete(); // Delete file or empty directory
            }
        }
    }

    public static String getDateTime(String format) {
        LocalDateTime now = LocalDateTime.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(format);
        return now.format(formatter);
    }

    public static void backupRepository(Record record, File destinationDir) {
        File sourceDirectory = new File(record.getRepoPath());
        try {
            copyDirectory(sourceDirectory, destinationDir);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void copyDirectory(File sourceDir, File destDir) throws IOException {
        // Create the destination directory if it doesn't exist
        if (!destDir.exists()) {
            destDir.mkdirs();
        }

        // Iterate over all files and directories in the source directory
        File[] files = sourceDir.listFiles();
        if (files != null) {
            for (File file : files) {
                File destFile = new File(destDir, file.getName());
                if (file.isDirectory()) {
                    // Recursively copy subdirectories
                    copyDirectory(file, destFile);
                } else {
                    // Copy individual files
                    Path sourcePath = file.toPath();
                    Path destPath = destFile.toPath();
                    try {
                        Files.copy(sourcePath, destPath, StandardCopyOption.REPLACE_EXISTING);
                    } catch (Exception e) {
                        System.err.printf("Access denied while copying %s to %s .%n", sourcePath.getFileName(), destPath);
                    }
                }
            }
        }
    }

    public static int computeNumberOfTokens(String input) throws Exception {
        if (input == null || input.isEmpty())
            return 0;
        String path = Paths.get(Constants.PROJECT_ROOT_DIR, Constants.PYTHON_SCRIPT_DIR, Constants.PYTHON_TOKENIZER_TEMP_FILE).toString();
        File file = new File(path);
        if (file.exists())
            file.delete();
        try {
            FileUtils.fileWrite(path, input);
            String[] terminalCommand = {"/bin/bash", "-c", generateTokenComputingCommand()};
            ProcessBuilder processBuilder = new ProcessBuilder(terminalCommand);
            processBuilder.directory(new File(Paths.get(Constants.PROJECT_ROOT_DIR, Constants.PYTHON_SCRIPT_DIR).toString()));
            processBuilder.environment().putAll(System.getenv());
            processBuilder.environment().put("SYSTEM_PATH", path);
            processBuilder.redirectErrorStream(true);
            Process process = processBuilder.start();
            InputStream inputStream = process.getInputStream();
            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
            String line;
            StringBuilder response = new StringBuilder();
            while ((line = reader.readLine()) != null)
                response.append(line).append("\n");

            return Integer.parseInt(response.toString().trim());
        } finally {
            if (file.exists())
                file.delete();
        }
    }

    public static int getMaxTPMLengthOfModel(Constants.LLM completionModel) {
        return switch (completionModel) {
            case GPT_4O -> 10_000_000;
            case GPT_4O_FINE_TUNED_ON_A -> 10_000_000;
            case GPT_4O_FINE_TUNED_ON_B -> 10_000_000;
            case GPT_4O_FINE_TUNED_ON_C -> 10_000_000;
            case GPT_4O_FINE_TUNED_ON_D -> 10_000_000;
            case LLAMA_2 -> 500_000_000;//random large number (infinite) indeed since we have it offline
            case LLAMA_2_FINE_TUNED_ON_A -> 500_000_000;//random large number (infinite) indeed since we have it offline
            case LLAMA_2_FINE_TUNED_ON_B -> 500_000_000;//random large number (infinite) indeed since we have it offline
            case LLAMA_2_FINE_TUNED_ON_C -> 500_000_000;//random large number (infinite) indeed since we have it offline
            case LLAMA_2_FINE_TUNED_ON_D -> 500_000_000;//random large number (infinite) indeed since we have it offline
        };
    }

    private static String generateTokenComputingCommand() {
        return "source ./models/env/bin/activate && python3 " +
                Constants.PYTHON_TOKENIZER_FILE +
                " --encoder " + Constants.PYTHON_TOKENIZER_ENCODER;
    }

    public static boolean writeToFile(String path, String content) throws Exception {
        // Clear the file content
        Files.write(Paths.get(path), new byte[0]);

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(path, true))) {
            writer.write(content);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public static String getBeforeSrc(String input) {
        int index = input.indexOf("src");
        if (index != -1) {
            String p = input.substring(0, index);
            if (!p.isEmpty())
                return p.substring(0, p.length() - 1);
            return p;
        }
        return null; // If "src" is not found, return null
    }

    public static String getFromSrc(String input) {
        int index = input.indexOf("src");
        if (index != -1) {
            return input.substring(index);
        }
        return null; // If "src" is not found, return null
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

//    public static void saveToJson(Object object, String filePath) {
//        System.out.println("Saving to (" + filePath + ")...");
//
//
//        Gson gson = new GsonBuilder().setPrettyPrinting().serializeNulls().serializeSpecialFloatingPointValues().registerTypeAdapter(AssertStmt.class, new AssertStmtSerializer()).create();
//
//
//        try (FileWriter writer = new FileWriter(filePath)) {
//            String json = gson.toJson(object);
//            gson.toJson(object, writer);
//            System.out.println("Data saved successfully to: " + filePath);
//        } catch (Exception e) {
//            System.err.println("Error saving data to JSON file: " + e.getMessage());
//            System.err.println("The object tried to jsonize is: " + object.toString());
//            e.printStackTrace();
//        }
//    }

    public static void saveToJson(Object object, String filePath) {
        System.out.println("Saving to (" + filePath + ")...");

        ObjectMapper mapper = new ObjectMapper();
        mapper.enable(SerializationFeature.FAIL_ON_SELF_REFERENCES);
        mapper.disable(SerializationFeature.FAIL_ON_UNWRAPPED_TYPE_IDENTIFIERS);
        mapper.configure(SerializationFeature.WRITE_NULL_MAP_VALUES, true);
        mapper.configure(MapperFeature.PROPAGATE_TRANSIENT_MARKER,true);

        mapper.setDefaultPrettyPrinter(new DefaultPrettyPrinter());

        // Register custom serializer if needed (replaces Gson's registerTypeAdapter)
        SimpleModule module = new SimpleModule();
        module.addSerializer(AssertStmt.class, new AssertStmtJacksonSerializer());
        mapper.registerModule(module);

        JsonFactory factory = new JsonFactory();

        try (JsonGenerator generator = factory.createGenerator(new File(filePath), JsonEncoding.UTF8)) {
            generator.setPrettyPrinter(new DefaultPrettyPrinter());
            mapper.writeValue(generator, object);  // This streams the object safely
            System.out.println("Data saved successfully to: " + filePath);
        } catch (Exception e) {
            System.err.println("Error saving data to JSON file: " + e.getMessage());
            System.err.println("The object tried to jsonize is: " + object.toString());
            e.printStackTrace();
        }
    }

    public static boolean isDirectoryEmpty(Path path) throws Exception {
        if (!Files.isDirectory(path)) {
            throw new Exception("Not a directory: " + path);
        }
        try (DirectoryStream<Path> directoryStream = Files.newDirectoryStream(path)) {
            return !directoryStream.iterator().hasNext();
        } catch (IOException e) {
            throw new Exception(e);
        }
    }

    public static void printCallGraph(List<CallGraphNode> graph) {
        for (CallGraphNode node : graph) {
            System.out.println("Node: " + node.getMethodDeclaration().getName() + " in " + node.getPath().toString());
            System.out.println("Children: ");
            for (MethodDeclaration methodDeclaration : node.getChildren())
                System.out.println("   +" + methodDeclaration.getNameAsString());
            System.out.println("--------------");
        }
    }

    public static List<CSVRecord> loadCSV(String filePath) {
        List<CSVRecord> records = new ArrayList<>();
        try (Reader reader = new FileReader(filePath)) {
            // Create CSVParser object to parse the CSV file
            CSVParser csvParser = new CSVParser(reader, CSVFormat.DEFAULT.withHeader().withIgnoreHeaderCase().withTrim());

            // Add all the records to the list
            records = csvParser.getRecords();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return records;
    }

    public static int readNumberFromFile(String filePath) {
        int number = 0;  // Default value in case the file is empty or there's an error
        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            String line = reader.readLine();
            if (line != null) {
                try {
                    // Parse the float from the line
                    number = Integer.parseInt(line);
                } catch (NumberFormatException e) {
                    System.out.println("Invalid number format in file: " + line);
                }
            } else {
                System.out.println("File is empty.");
            }
        } catch (IOException e) {
            System.out.println("Error reading the file: " + e.getMessage());
        }
        return number;
    }

    public static void writeNumberToFile(String filePath, int number) {
        try (PrintWriter writer = new PrintWriter(filePath)) {
            writer.println(number);  // Write the float number
            System.out.println("Number written to file successfully.");
        } catch (IOException e) {
            System.out.println("Error writing to file: " + e.getMessage());
        }
    }

    public static void selectAndSaveElements(String jsonFilePath) throws IOException {
        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode rootNode = objectMapper.readTree(new File(jsonFilePath));

        if (!rootNode.isArray()) {
            throw new IllegalArgumentException("The JSON file must contain an array.");
        }

        List<JsonNode> allElements = new ArrayList<>();
        List<JsonNode> selectedElements = new ArrayList<>();

        for (JsonNode node : rootNode) {
            if (node.has("hasAnyAssertions") && node.get("hasAnyAssertions").asBoolean()) {
                selectedElements.add(node);
            } else {
                allElements.add(node);
            }
        }

        int remaining = 300 - selectedElements.size();
        if (remaining > 0 && allElements.size() > remaining) {
            Collections.shuffle(allElements);
            selectedElements.addAll(allElements.subList(0, remaining));
        } else {
            selectedElements.addAll(allElements);
        }

        ArrayNode outputArray = objectMapper.createArrayNode();
        outputArray.addAll(selectedElements);
        objectMapper.writeValue(new File(jsonFilePath), outputArray);
    }

    public static int countUniqueRepositories(String csvFilePath) {
        Set<String> uniqueRepositories = new HashSet<>();

        try (BufferedReader reader = Files.newBufferedReader(Paths.get(csvFilePath))) {
            String headerLine = reader.readLine();
            if (headerLine == null) {
                return 0; // Empty file
            }

            List<String> headers = Arrays.asList(headerLine.split(","));
            int repoIndex = headers.indexOf("Repository");

            if (repoIndex == -1) {
                throw new IllegalArgumentException("Column 'Repository' not found in the file.");
            }

            String line;
            while ((line = reader.readLine()) != null) {
                String[] values = line.split(",");
                if (values.length > repoIndex) {
                    uniqueRepositories.add(values[repoIndex].trim());
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
            return -1; // Error case
        }

        return uniqueRepositories.size();
    }

    public static void modifyJsonScores(String filePath) {
        ObjectMapper mapper = new ObjectMapper();

        try {
            // Load JSON file
            JsonNode rootNode = mapper.readTree(new File(filePath));
            int size = rootNode.get("size").asInt();
            int numberOfAssertionFulRecord = rootNode.get("numberOfAssertionFulRecord").asInt();

            if (numberOfAssertionFulRecord == 0) {
                System.out.println("numberOfAssertionFulRecord is zero. Cannot perform modification.");
                return;
            }

            // Modify scores for averageInitialScoresForAssertionFulMethods and averageFinalScoresForAssertionFulMethods
            updateScores(rootNode, "averageInitialScoresForAssertionFulMethods", size, numberOfAssertionFulRecord);
            updateScores(rootNode, "averageFinalScoresForAssertionFulMethods", size, numberOfAssertionFulRecord);

            // Write updated JSON back to file
            mapper.writerWithDefaultPrettyPrinter().writeValue(new File(filePath), rootNode);
            System.out.println("JSON file modified successfully.");

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void updateScores(JsonNode rootNode, String key, int size, int numberOfAssertionFulRecord) {
        JsonNode scoresNode = rootNode.get(key);

        if (scoresNode != null) {
            JsonNode rougeScores = scoresNode.get("rougeScores");
            JsonNode bleuScores = scoresNode.get("bleuScores");
            JsonNode levenshteinScores = scoresNode.get("levenshteinScores");
            JsonNode jaccardScores = scoresNode.get("jaccardScores");

            // Process each type of score if it exists
            if (rougeScores != null) {
                updateScoreFields((ObjectNode) rougeScores, size, numberOfAssertionFulRecord);
            }
            if (bleuScores != null) {
                updateScoreFields((ObjectNode) bleuScores, size, numberOfAssertionFulRecord);
            }
            if (levenshteinScores != null) {
                updateScoreFields((ObjectNode) levenshteinScores, size, numberOfAssertionFulRecord);
            }
            if (jaccardScores != null) {
                updateScoreFields((ObjectNode) jaccardScores, size, numberOfAssertionFulRecord);
            }
        }
    }

    private static void updateScoreFields(ObjectNode node, int size, int numberOfAssertionFulRecord) {
        Iterator<Map.Entry<String, JsonNode>> fields = node.fields();
        while (fields.hasNext()) {
            Map.Entry<String, JsonNode> field = fields.next();
            if (field.getValue().isNumber()) {
                double originalValue = field.getValue().asDouble();
                double updatedValue = (originalValue * size) / numberOfAssertionFulRecord;
                node.put(field.getKey(), updatedValue);
            }
        }
    }

    public static double computeAverageLength(List<String> list) {
        if (list == null || list.isEmpty()) {
            return 0.0;
        }

        return list.stream().mapToInt(String::length).average().orElseThrow(() -> new RuntimeException("Cannot take average length"));
    }

    public static void deleteFile(Path path, String... fileNames) {
        System.out.println("Cleaning up!");
        for (String fileName : fileNames) {
            Path filePath = path.resolve(fileName);
            try {
                Files.deleteIfExists(filePath);
                System.out.println("Deleted: " + filePath);
            } catch (IOException e) {
                System.err.println("Failed to delete: " + filePath + " - " + e.getMessage());
            }
        }
        System.out.println("Cleaned up!");
    }

    public static class AssertStmtJacksonSerializer extends JsonSerializer<AssertStmt> {
        @Override
        public void serialize(AssertStmt stmt, JsonGenerator gen, SerializerProvider serializers) throws IOException {
            gen.writeString(stmt.toString());
        }
    }

//    public static class AssertStmtSerializer implements JsonSerializer<AssertStmt> {
//        @Override
//        public JsonElement serialize(AssertStmt stmt, Type typeOfSrc, JsonSerializationContext context) {
//            return new JsonPrimitive(stmt.toString());
//        }
//    }

    public static String getCurrentDateTime() {
        LocalDateTime now = LocalDateTime.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        return now.format(formatter);
    }
}
