package org.example;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.stmt.AssertStmt;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.CSVRecord;
import org.json.JSONObject;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

public class ThresholdGenerator {
    private final String csvPath;
    private final String outputCSVPath;
    private final String reservoirPath;
    private int starThreshold;
    private int productionAssertionThreshold;
    private Map<String, Integer> repo2NumberOfProductionAssertions = new HashMap<>();

    public ThresholdGenerator(String inputCSVPath, String reservoirPath, String outputCSVPath) {
        this.csvPath = inputCSVPath;
        this.reservoirPath = reservoirPath;
        this.outputCSVPath = outputCSVPath;
    }

    public int getStarThreshold() {
        return starThreshold;
    }

    public int getProductionAssertionThreshold() {
        return productionAssertionThreshold;
    }

    public void extractAndCreateFilteredCSV() {
        List<CSVRecord> selectedStarredRepos;
        if (new File(Constants.STAR_TMP_CSV).exists() && new File("star_threshold.txt").exists()) {
            System.out.println("Reading stars offline...");
            selectedStarredRepos = Utils.loadCSV(Constants.STAR_TMP_CSV);
            this.starThreshold = Utils.readNumberFromFile("star_threshold.txt");
        } else {
            System.out.println("Reading online...");
            Pair<Integer, List<CSVRecord>> starStatistics = calculateStarThreshold(this.csvPath);
            assert starStatistics != null && starStatistics.getSecond() != null;
            this.starThreshold = starStatistics.getFirst();
            Utils.writeNumberToFile("star_threshold.txt", this.starThreshold);
            selectedStarredRepos = starStatistics.getSecond();
            saveInCSV(selectedStarredRepos, true);
        }

        List<CSVRecord> filteredRecords = extractSatisfyingProductionAssertionRecords(selectedStarredRepos);
        saveInCSV(filteredRecords, false);
    }

    /**
     * Using median
     *
     * @param selectedStarredRepos
     * @return
     */
    private List<CSVRecord> extractSatisfyingProductionAssertionRecords(List<CSVRecord> selectedStarredRepos) {
        List<Pair<CSVRecord, Integer>> candidateProductionAssertionFilterRecords = Collections.synchronizedList(new ArrayList<>());

        for (CSVRecord record : selectedStarredRepos) {
            try {
                String repoURL = record.get("Repository").trim();

                String repoName = DatasetExtractor.cloneRepository(repoURL, this.reservoirPath);
                if (repoName == null) {
                    throw new RuntimeException("Failed to clone repository");
                }

                int numberOfProductionAssertions = getNumberOfProductionAssertions(this.reservoirPath, repoName);
                if (numberOfProductionAssertions == -1) {
                    throw new RuntimeException("Failed to extract production assertions");
                }

                System.out.println("Production assertion number: " + numberOfProductionAssertions + " | " + repoURL);
                candidateProductionAssertionFilterRecords.add(Pair.of(record, numberOfProductionAssertions));
            } catch (Throwable e) {
                System.err.println("Skipping " + record.get("Repository").trim() + " due to error: " + e.getMessage());
            }
        }


        this.productionAssertionThreshold = computeMedianElement(candidateProductionAssertionFilterRecords);

        return candidateProductionAssertionFilterRecords
                .stream()
                .filter(pair -> pair.getSecond() >= this.productionAssertionThreshold)
                .map(Pair::getFirst)
                .collect(Collectors.toList());
    }

    private int computeMedianElement(List<Pair<CSVRecord, Integer>> list) {
        if (list == null || list.isEmpty()) {
            throw new IllegalArgumentException("List cannot be null or empty");
        }

        list.sort(Comparator.comparingInt(Pair::getSecond));

        int size = list.size();
        return list.get(size / 2).getSecond();
    }

    public void saveInCSV(List<CSVRecord> filteredRecords, boolean starTmp) {
        assert filteredRecords != null;
        String outputPath = starTmp ? "star_tmp.csv" : this.outputCSVPath;

        // Read the header from the existing file
        String[] header;
        try (BufferedReader reader = Files.newBufferedReader(Paths.get(this.csvPath));
             CSVParser parser = new CSVParser(reader, CSVFormat.DEFAULT.withFirstRecordAsHeader())) {

            // Convert header names into a String array
            header = parser.getHeaderMap().keySet().toArray(new String[0]);
        } catch (IOException e) {
            System.err.println("Error reading header from file: " + this.csvPath);
            e.printStackTrace();
            return;
        }

        // Write the filtered records along with the header into a new file
        try (BufferedWriter writer = Files.newBufferedWriter(Paths.get(outputPath));
             CSVPrinter printer = new CSVPrinter(writer, CSVFormat.DEFAULT.withHeader(header))) {

            for (CSVRecord record : filteredRecords) {
                // Print each record. Note that CSVPrinter can accept a CSVRecord directly.
                printer.printRecord(record);
            }
            printer.flush();
        } catch (IOException e) {
            System.err.println("Error writing to new file: " + outputPath);
            e.printStackTrace();
        }
    }


    /**
     * Using percentile-based approach
     *
     * @param csvPath
     * @return CSV threshold + records which have stars greater-equal to the calculated threshold.
     */
    private Pair<Integer, List<CSVRecord>> calculateStarThreshold(String csvPath) {
        List<Pair<CSVRecord, Integer>> records = new ArrayList<>();
        List<Integer> starCounts = new ArrayList<>();
        Map<String, Integer> repo2stars = new HashMap<>();

        try {
            String token = Constants.GITHUB_API_KEY;
            try (FileReader fileReader = new FileReader(csvPath);
                 CSVParser csvParser = new CSVParser(fileReader, CSVFormat.DEFAULT.withFirstRecordAsHeader())) {
                for (CSVRecord record : csvParser) {
                    String repoUrl = record.get("Repository").trim();
                    // Normalize the URL to extract the repository path
                    String clonedRepoUrl = repoUrl.replace("https://github.com/", "")
                            .replace("http://github.com/", "")
                            .replace("github.com/", "");

                    if (repo2stars.containsKey(repoUrl)) {
                        System.out.println("Retrieving from cache...");
                        int starCount = repo2stars.get(repoUrl);
                        System.out.println("Star Count: " + starCount + " | " + clonedRepoUrl);
                        starCounts.add(starCount);
                        records.add(Pair.of(record, starCount)); // Store repo and star count
                    } else {
                        try {
                            // GitHub API URL to get repository details
                            String apiUrl = "https://api.github.com/repos/" + clonedRepoUrl;

                            // Set up the connection
                            URL url = new URL(apiUrl);
                            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                            connection.setRequestMethod("GET");
                            connection.setRequestProperty("Accept", "application/vnd.github.v3+json");

                            // Add authorization header if token is provided
                            connection.setRequestProperty("Authorization", "token " + token);

                            // Check the response code
                            int responseCode = connection.getResponseCode();
                            if (responseCode == 200) {
                                // Read the response
                                BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                                StringBuilder response = new StringBuilder();
                                String inputLine;
                                while ((inputLine = in.readLine()) != null) {
                                    response.append(inputLine);
                                }
                                in.close();

                                // Parse the JSON response
                                JSONObject jsonResponse = new JSONObject(response.toString());

                                // Extract the star count
                                int starCount = jsonResponse.getInt("stargazers_count");
                                System.out.println("Star Count: " + starCount + " | " + clonedRepoUrl);
                                starCounts.add(starCount);
                                repo2stars.put(repoUrl, starCount);
                                Thread.sleep(500);
                                records.add(Pair.of(record, starCount)); // Store repo and star count
                            } else {
                                System.out.println("Failed to fetch data for " + clonedRepoUrl + ". HTTP response code: " + responseCode);
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }

        // If there are no star counts, return null
        if (starCounts.isEmpty()) {
            return null;
        }

        // Sort the star counts
        Collections.sort(starCounts);

        // Compute percentile threshold
        double fraction = (double) Constants.STAR_THRESHOLD_PERCENTILE / 100.0;
        int n = starCounts.size();
        double rank = fraction * (n - 1);
        int lowerIndex = (int) Math.floor(rank);
        int upperIndex = (int) Math.ceil(rank);
        double fractionPart = rank - lowerIndex;

        double percentileValue;
        if (lowerIndex == upperIndex) {
            percentileValue = starCounts.get(lowerIndex);
        } else {
            int lowerValue = starCounts.get(lowerIndex);
            int upperValue = starCounts.get(upperIndex);
            percentileValue = lowerValue + fractionPart * (upperValue - lowerValue);
        }

        int threshold = (int) percentileValue;

        // Filter records that meet the threshold
        List<CSVRecord> filteredRecords = records.stream()
                .filter(pair -> pair.getSecond() >= threshold)
                .map(Pair::getFirst)
                .collect(Collectors.toList());

        return Pair.of(threshold, filteredRecords);
    }

    private int getNumberOfProductionAssertions(String originalRepoFolderPath, String repoName) {
        if (repo2NumberOfProductionAssertions.containsKey(repoName)) {
            System.out.println("Retrieving from cache...");
            return repo2NumberOfProductionAssertions.get(repoName);
        }

        Path fullPath = Paths.get(originalRepoFolderPath, repoName);
        File repoFolder = fullPath.toFile();

        if (!repoFolder.exists() || !repoFolder.isDirectory()) {
            return -1;
        }

        // Queue for BFS directory traversal
        Queue<File> queue = new LinkedList<>();
        queue.add(repoFolder);

        int totalAssertions = 0;

        while (!queue.isEmpty()) {
            File currentFile = queue.poll();

            if (currentFile.isDirectory()) {
                // Add all files and subdirectories to the queue
                File[] files = currentFile.listFiles();
                if (files != null) {
                    queue.addAll(Arrays.asList(files));
                }
            } else if (currentFile.getName().endsWith(".java") && !currentFile.getAbsolutePath().contains(File.separator + "src" + File.separator + "test")) {
                // Parse the Java file and count assertions
                try {
                    String fileContent = new String(Files.readAllBytes(currentFile.toPath()));
                    CompilationUnit cu = StaticJavaParser.parse(fileContent);

                    // Count method calls named "assert" (or customize to match your assertion naming conventions)
                    List<AssertStmt> assertStmts = cu.findAll(AssertStmt.class);
                    int assertionCount = assertStmts.size();

                    totalAssertions += assertionCount;
                } catch (Exception e) {

                }
            }
        }

        repo2NumberOfProductionAssertions.put(repoName, totalAssertions);
        return totalAssertions;
    }
}
