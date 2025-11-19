package org.example;

public class AssertionPicker {
    public static void pickFrom(AssertionFeatureMap assertionFeatureMap) {
        System.out.println("Picking best assertions... (total: " + assertionFeatureMap.getAssertionFeatures().size() + ")");
        for (AssertionFeature feature : assertionFeatureMap.getAssertionFeatures()) {
            if (feature.getSyntacticalCorrectness().isCorrect() && feature.getStaticSemanticalCorrectness().isCorrect() && feature.getDynamicSemanticalCorrectness().isCorrect())
                feature.setPicked(true);
            else
                feature.setPicked(false);
        }
    }
}