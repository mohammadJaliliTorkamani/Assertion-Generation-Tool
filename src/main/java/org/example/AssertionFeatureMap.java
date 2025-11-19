package org.example;

import java.util.ArrayList;
import java.util.List;

public class AssertionFeatureMap {
    private List<AssertionFeature> assertionFeatures;

    private AssertionFeatureMap(List<AssertionFeature> assertionFeatures) {
        this.assertionFeatures = assertionFeatures;
    }

    public static AssertionFeatureMap initialize(List<PredictedAssertion> predictedAssertions, Parser parser) {
        List<AssertionFeature> assertionFeatures = new ArrayList<>();
        for (PredictedAssertion predictedAssertion : predictedAssertions) {
            try {
                AssertionFeature assertionFeature = new AssertionFeature(predictedAssertion.getAssertion(), predictedAssertion.getLineNumber(), parser);
                assertionFeatures.add(assertionFeature);
            }catch (Exception e){}
        }
        return new AssertionFeatureMap(assertionFeatures);
    }

    public List<AssertionFeature> getAssertionFeatures() {
        return assertionFeatures;
    }

    public boolean hasImperfectRows() {
        if (assertionFeatures.isEmpty())
            return true;

        return assertionFeatures.stream().anyMatch(assertionFeature ->
                !assertionFeature.getSyntacticalCorrectness().isCorrect() ||
                        !assertionFeature.getStaticSemanticalCorrectness().isCorrect() ||
                        !assertionFeature.getDynamicSemanticalCorrectness().isCorrect());
    }

    public void print() {
        System.out.println("|-----------------------------------------------|");
        System.out.println("|           Inferred Checked Features           |");
        System.out.println("|-----------------------------------------------|");
        for (AssertionFeature assertionFeature : assertionFeatures) {
            System.out.println("    (" + assertionFeature.getAssertion() + " , line# " + assertionFeature.getLineNumber() + ") => ");
            System.out.println("        Syntactically Correct: " + assertionFeature.getSyntacticalCorrectness().isCorrect());
            System.out.println("        Static  Semantically Correct: " + (assertionFeature.getStaticSemanticalCorrectness() == null ? "Null" : assertionFeature.getStaticSemanticalCorrectness().isCorrect()));
            System.out.println("        Dynamic Semantically Correct: " + (assertionFeature.getDynamicSemanticalCorrectness() == null ? "Null" : (assertionFeature.getDynamicSemanticalCorrectness().isCorrect())));
            System.out.println("------------------------------------------------");
        }
    }

    public boolean hasAtLeastOneImperfectAssertion() {
        return getAssertionFeatures().stream().anyMatch(feature -> !feature.getSyntacticalCorrectness().isCorrect() || !feature.getStaticSemanticalCorrectness().isCorrect() || !feature.getDynamicSemanticalCorrectness().isCorrect());
    }
}