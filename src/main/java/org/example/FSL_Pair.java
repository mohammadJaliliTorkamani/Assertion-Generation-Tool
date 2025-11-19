package org.example;

public class FSL_Pair {
    private Record record;
    private double similarity;

    public FSL_Pair(Record record, double similarity) {
        this.record = record;
        this.similarity = similarity;
    }

    public Record getRecord() {
        return record;
    }

    public void setRecord(Record record) {
        this.record = record;
    }

    public double getSimilarity() {
        return similarity;
    }

    public void setSimilarity(double similarity) {
        this.similarity = similarity;
    }
}
