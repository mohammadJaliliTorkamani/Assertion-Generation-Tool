package org.example;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

public abstract class Dataset {
    private List<Record> records;
    private String path;

    public List<Record> getRecordsAsList() {
        return records;
    }

    public Iterator<Record> getRecordsAsIterator() {
        return records == null ? Collections.emptyIterator() : records.iterator();
    }

    public void restoreDataset() {
        if (records == null)
            System.out.println("Warning: Saving `null' into the dataset " + path);
        Utils.saveToJson(records, path);
        try {
            Utils.cleanFSLJsonFile(path);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void setRecords(List<Record> records) {
        this.records = records;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public int getRecordCount() {
        return records == null ? 0 : records.size();
    }

    public long countMethodsInPackage(String packageName) {
        return records.stream()
                .filter(record -> packageName.equals(record.getPackageName()))
                .count();
    }


    public double calculateAverageMethodLength() {
        if (records == null || records.isEmpty()) {
            return 0;
        }

        return records.stream()
                .mapToInt(record -> record.getEndLine() - record.getStartLine() + 1)
                .average()
                .orElse(0);
    }

    public int calculateTotalCodeLines() {
        if (records == null || records.isEmpty()) {
            return 0;
        }

        return records.stream()
                .mapToInt(record -> record.getEndLine() - record.getStartLine() + 1)
                .sum();
    }

    public Map<String, Long> countMethodsByRepo() {
        if (records == null || records.isEmpty()) {
            return Map.of();
        }

        return records.stream()
                .collect(Collectors.groupingBy(Record::getRepoName, Collectors.counting()));
    }

    public Set<String> getUniqueRepoNames() {
        if (records == null || records.isEmpty()) {
            return Set.of();
        }

        return records.stream()
                .map(Record::getRepoName)
                .collect(Collectors.toSet());
    }

    @Override
    public String toString() {
        return "Dataset{" +
                "records=" + records +
                '}';
    }
}
