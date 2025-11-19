package org.example;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

public class DynamicSemanticCorrectObject {
    private List<UnitTestExecutionResult> unitTestExecutionResults;

    public DynamicSemanticCorrectObject(List<UnitTestExecutionResult> unitTestExecutionResults) {
        this.unitTestExecutionResults = unitTestExecutionResults;
    }

    public DynamicSemanticCorrectObject() {
        this.unitTestExecutionResults = new LinkedList<>();
    }

    /**
     * By unique, we mean unique "path and name" for each unit test.
     *
     * @param covered only consider covered unit tests or only consider uncovered ones.
     * @return number of unique unit tests
     */
    public int getNumberOfUniqueTestExecutionResults(boolean covered) {
        if (unitTestExecutionResults == null)
            return 0;

        Set<String> uniqueKeys = new HashSet<>();
        for (UnitTestExecutionResult test : unitTestExecutionResults)
            if (test.isCovered() == covered)
                uniqueKeys.add(test.getTestPath() + "##" + test.getTestName());

        return uniqueKeys.size();
    }

    /**
     * By unique, we mean unique "path and name" for each unit test.
     *
     * @param covered only consider covered unit tests or only consider uncovered ones.
     * @return number of unique passed unit tests
     */
    public int getNumberOfUniquePassedTestExecutionResults(boolean covered) {
        if (unitTestExecutionResults == null)
            return 0;

        Set<String> uniqueKeys = new HashSet<>();
        for (UnitTestExecutionResult test : unitTestExecutionResults)
            if (test.isPassed() && (test.isCovered() == covered))
                uniqueKeys.add(test.getTestPath() + "##" + test.getTestName());

        return uniqueKeys.size();
    }

    /**
     * By unique, we mean unique "path and name" for each unit test.
     *
     * @param covered only consider covered unit tests or only consider uncovered ones.
     * @return number of unique failed unit tests
     */
    public int getNumberOfUniqueFailedTestExecutionResults(boolean covered) {
        if (unitTestExecutionResults == null)
            return 0;

        Set<String> uniqueKeys = new HashSet<>();
        for (UnitTestExecutionResult test : unitTestExecutionResults)
            if ((!test.isPassed()) && (test.getComponentResponse() == null) && (test.isCovered() == covered))
                uniqueKeys.add(test.getTestPath() + "##" + test.getTestName());

        return uniqueKeys.size();
    }

    /**
     * By unique, we mean unique "path and name" for each unit test.
     *
     * @return number of unique erroneous unit tests
     */
    public int getNumberOfUniqueErroneousTestExecutionResults(boolean covered) {
        if (unitTestExecutionResults == null)
            return 0;

        Set<String> uniqueKeys = new HashSet<>();
        for (UnitTestExecutionResult test : unitTestExecutionResults)
            if ((!test.isPassed()) && (test.getComponentResponse() != null) && (test.isCovered() == covered))
                uniqueKeys.add(test.getTestPath() + "##" + test.getTestName());

        return uniqueKeys.size();
    }

    public List<UnitTestExecutionResult> getUnitTextExecutionResults() {
        return unitTestExecutionResults;
    }

    public boolean isCorrect() {
        return unitTestExecutionResults.stream().allMatch(UnitTestExecutionResult::isPassed);
    }
}
