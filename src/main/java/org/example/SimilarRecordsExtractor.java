package org.example;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.body.MethodDeclaration;
import org.example.call_graph.RepoJavaParser;

import java.nio.file.Paths;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SimilarRecordsExtractor {
    private final int fewShotLearningNumberOfSamples;
    private final double minimumAcceptableCosineValue;
    private FSLDataset fslDataset;

    public SimilarRecordsExtractor(int fewShotLearningNumberOfSamples, double minimumAcceptableCosineValue) {
        this.fewShotLearningNumberOfSamples = fewShotLearningNumberOfSamples;
        this.minimumAcceptableCosineValue = minimumAcceptableCosineValue;
    }

    public Map<String, String> getSimilarMethods(Parser parser, Constants.Experiment_ID experimentId, Inference inference) throws Exception {
        if (!experimentId.equals(Constants.Experiment_ID.D))
            return null;

        System.out.println("Extracting similar methods...");
        return getTopNSimilarMethods(parser, inference);
    }

    public void setFSLDataset(FSLDataset fslDataset) {
        this.fslDataset = fslDataset;
    }

    private Map<String, String> getTopNSimilarMethods(Parser parser, Inference inference) throws Exception {
        int n = this.fewShotLearningNumberOfSamples;
        Record record = parser.getRecord();

        Map<String, String> map = new HashMap<>();

        String pureMethodBody = RepoJavaParser.getInstance(Paths.get(parser.getRecord().getRepoPath(), parser.getRecord().getMiddleModulePath()).toString()).parseMethodDeclaration(record.printPureMethod()).getResult().get().getBody().get().toString().trim();

        List<Double> embeddingVector = inference.calculateEmbeddings(pureMethodBody);
        this.fslDataset
                .getRecordsAsList()
                .stream()
                .filter(FSLRecord -> {
                    try { //because sometimes there could be some pattern (like instance of that Javaparser does not support it and throws exception)
                        StaticJavaParser.parseMethodDeclaration(FSLRecord.printMethod(true, true, true));
                    } catch (Exception e) {
                        return false;
                    }
                    return FSLRecord.getEmbeddingVector() != null && !FSLRecord.getEmbeddingVector().isEmpty();
                }) //because of character encoding problems on behalf of OpenAI, we have saved null when calculating embeddings. so to ensure they are not null, we discard corrupted records on reading
                .map(FSLRecord -> {
                    double similarity = calculateCosineSimilarity(FSLRecord.getEmbeddingVector(), embeddingVector);
                    return new FSL_Pair(FSLRecord, similarity);
                })
                .sorted(Comparator.comparingDouble(pair -> ((FSL_Pair) pair).getSimilarity()).reversed())
                .filter(fsl_pair -> fsl_pair.getSimilarity() >= minimumAcceptableCosineValue)
                .limit(n)
                .forEach(fsl_pair ->
                {
                    try {
                        MethodDeclaration methodWithoutAssertions = StaticJavaParser.parseMethodDeclaration(fsl_pair.getRecord().getMethodWithoutAssertions());
                        MethodDeclaration methodWithAssertions = StaticJavaParser.parseMethodDeclaration(fsl_pair.getRecord().getMethodWithAssertions());
                        map.put(
                                Parser.getCommentLessAndJavadocLessMethodDeclaration(methodWithoutAssertions),
                                Parser.getCommentLessAndJavadocLessMethodDeclaration(methodWithAssertions)
                        );
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                });

        return map;
    }

    private double calculateCosineSimilarity(List<Double> vector1, List<Double> vector2) {
        int dimensionality = Math.max(vector1.size(), vector2.size());
        if (dimensionality == 0) {
            throw new IllegalArgumentException("Both vectors are empty.");
        }
        if(vector2.isEmpty())
            return -1;

        double dotProduct = 0.0;
        double normVector1 = 0.0;
        double normVector2 = 0.0;

        for (int i = 0; i < dimensionality; i++) {
            double value1 = i < vector1.size() ? vector1.get(i) : 0.0;
            double value2 = i < vector2.size() ? vector2.get(i) : 0.0;

            dotProduct += value1 * value2;
            normVector1 += value1 * value1;
            normVector2 += value2 * value2;
        }

        normVector1 = Math.sqrt(normVector1);
        normVector2 = Math.sqrt(normVector2);

        if (normVector1 == 0 || normVector2 == 0) {
            return 0.0; // Return 0 to indicate no similarity if any vector is zero vector
        }

        return dotProduct / (normVector1 * normVector2);
    }
}
