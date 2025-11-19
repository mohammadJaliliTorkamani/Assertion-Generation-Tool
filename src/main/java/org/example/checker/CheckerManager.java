package org.example.checker;

import org.example.AssertionFeatureMap;
import org.example.Parser;
import org.example.PredictedAssertion;

import java.util.ArrayList;
import java.util.List;

public class CheckerManager {
    private static CheckerManager instance;
    private final List<Checker> checkers;

    private CheckerManager() {
        this.checkers = new ArrayList<>();
    }

    public static CheckerManager getInstance() {
        if (instance == null) {
            instance = new CheckerManager();
        }
        return instance;
    }

    public CheckerManager clearCheckers() {
        checkers.clear();
        return this;
    }

    public CheckerManager syntacticChecker() {
        checkers.add(new SyntacticChecker());
        return this;
    }

    public CheckerManager staticSemanticChecker() {
        checkers.add(new StaticSemanticChecker());
        return this;
    }

    public CheckerManager indirectDynamicSemanticChecker() {
        checkers.add(new DynamicSemanticChecker());
        return this;
    }

    public AssertionFeatureMap check(Parser parser, List<PredictedAssertion> predictedAssertions) {
        AssertionFeatureMap assertionFeatureMap = AssertionFeatureMap.initialize(predictedAssertions, parser);
        for (Checker checker : checkers) {
            System.out.println("Checking " + checker.getClass().getSimpleName());
            checker.check(parser, assertionFeatureMap);
        }
        return assertionFeatureMap;
    }
}
