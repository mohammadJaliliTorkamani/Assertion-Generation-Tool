package org.example;

import java.util.HashSet;
import java.util.Set;

public class CoverageSurplusIndex {
    private double CSI_Score;
    private int S1_Size;
    private int S2_Size;
    private Set<String> S1;
    private Set<String> S2;

    public CoverageSurplusIndex(double CSI_Score) {
        this.CSI_Score = CSI_Score;
        this.S1_Size = -1;
        this.S2_Size = -1;
    }

    public CoverageSurplusIndex(Set<String> s1, Set<String> s2) {
        S1 = s1;
        S2 = s2;
        this.S1_Size = S1.size();
        this.S2_Size = S2.size();


        if (s1.isEmpty() && s2.isEmpty())
            this.CSI_Score = -1;
        else if (s2.isEmpty())
            this.CSI_Score = -1;
        else {
            Set<String> intersection = new HashSet<>(s1);
            intersection.retainAll(s2);
            this.CSI_Score = 1.0 * intersection.size() / s2.size();
        }
    }

    public double getCSI_Score() {
        return CSI_Score;
    }

    public void setCSI_Score(double CSI_Score) {
        this.CSI_Score = CSI_Score;
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
}
