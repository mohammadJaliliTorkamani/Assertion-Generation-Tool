package org.example;

import java.util.List;

public class DualMetrics {
    private CardinalityRatio CR;
    private CoverageSurplusIndex CSI;

    public DualMetrics() {
    }

    public DualMetrics(CardinalityRatio CR, CoverageSurplusIndex CSI) {
        this.CR = CR;
        this.CSI = CSI;
    }

    public static DualMetrics calculateAverageDualMetric(List<DualMetrics> metrics) {
        if (metrics == null || metrics.isEmpty())
            return null;

        double averageCRScore = metrics.stream().mapToDouble(dualMetric -> dualMetric.getCR().getScore()).filter(value -> value >= 0).sum();
        double averageCSIScore = metrics.stream().mapToDouble(dualMetric -> dualMetric.getCSI().getCSI_Score()).filter(value -> value >= 0).sum();


        return new DualMetrics(new CardinalityRatio(averageCRScore / metrics.size()), new CoverageSurplusIndex(averageCSIScore / metrics.size()));
    }

    public CardinalityRatio getCR() {
        return CR == null ? new CardinalityRatio(0) : CR;
    }

    public void setCR(CardinalityRatio CR) {
        this.CR = CR;
    }

    public CoverageSurplusIndex getCSI() {
        return CSI == null ? new CoverageSurplusIndex(0) : CSI;
    }

    public void setCSI(CoverageSurplusIndex CSI) {
        this.CSI = CSI;
    }
}
