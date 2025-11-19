package org.example;

import com.github.javaparser.StaticJavaParser;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import static org.example.Constants.EMBEDDING_BATCH_SIZE;

public class FSLDataset extends Dataset {

    public void calculateEmbeddingVectors(Inference inference) {
        for (int i = 0; i < getRecordsAsList().size(); i += EMBEDDING_BATCH_SIZE) {
            int endIndex = Math.min(i + EMBEDDING_BATCH_SIZE, getRecordsAsList().size());
            List<Record> subList = getRecordsAsList().subList(i, endIndex);
            System.out.printf("Calculating sample [%d-%d] / %d%n", i, endIndex, getRecordsAsList().size());


            try {
                List<List<Double>> batchEmbeddings = inference.calculateEmbeddingsForBatchInputs(
                        subList
                                .stream()
//                                .filter(fslRecord -> {//to discard methods with 'instanceof' which are not supported by the JavaParser
//                                            try {
//                                                return !fslRecord.getMethodWithoutAssertions().contains("instanceof") &&
//                                                        !fslRecord.getMethodWithAssertions().contains("instanceof");
//                                            } catch (Exception e) {
//                                                return false;
//                                            }
//                                        }
//                                )
                                .map(fslRecord -> { //to discard Text Block Literals which are not supported by the JavaParser
                                    try {
                                        return StaticJavaParser.parseMethodDeclaration(fslRecord.getMethodWithoutAssertions()).getBody().get().toString();
                                    } catch (Exception e) {
                                        return null;
                                    }
                                })
                                .filter(Objects::nonNull)
                                .map(parsedMethodBodyStr -> {
                                    return new LLM_InputContent(null, parsedMethodBodyStr, null);
                                })
                                .collect(Collectors.toList()));
                for (int j = 0; j < batchEmbeddings.size(); j++) {
                    subList.get(j).setEmbeddingVector(batchEmbeddings.get(j));
                }
            } catch (Exception e) {
                e.printStackTrace();
                continue;
            }
        }
    }

    public boolean hasIncompleteEmbeddingVectors() {
        return getRecordsAsList().stream().anyMatch(record -> record.getEmbeddingVector() == null || record.getEmbeddingVector().isEmpty());
    }
    //some statistic calculator methods, specific to fsl dataset

}