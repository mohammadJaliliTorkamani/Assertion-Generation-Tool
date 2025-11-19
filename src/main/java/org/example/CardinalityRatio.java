package org.example;

import java.util.Set;

public class CardinalityRatio {
    private double score;
    private int S1_Size;
    private int S2_Size;
    private Set<String> S1;
    private Set<String> S2;

    public CardinalityRatio(double score) {
        this.score = score;
        this.S1_Size = -1;
        this.S2_Size = -1;
    }

    public CardinalityRatio(Set<String> S1, Set<String> S2) {
        this.S1 = S1;
        this.S2 = S2;
        this.S1_Size = S1.size();
        this.S2_Size = S2.size();
        this.score = S2_Size == 0 ? -1 : S1_Size * 1.0 / S2_Size;
    }

    public Set<String> getS1() {
        return S1;
    }

    public void setS1(Set<String> s1) {
        S1 = s1;
    }

    public Set<String> getS2() {
        return S2;
    }

    public void setS2(Set<String> s2) {
        S2 = s2;
    }

    public double getScore() {
        return score;
    }

    public void setScore(double score) {
        this.score = score;
    }

    public int getS1_Size() {
        return S1_Size;
    }

    public void setS1_Size(int s1_Size) {
        S1_Size = s1_Size;
    }

    public int getS2_Size() {
        return S2_Size;
    }

    public void setS2_Size(int s2_Size) {
        S2_Size = s2_Size;
    }
}
