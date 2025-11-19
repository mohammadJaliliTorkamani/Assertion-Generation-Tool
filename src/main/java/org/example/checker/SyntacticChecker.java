package org.example.checker;

import org.example.Record;
import org.example.*;

public class SyntacticChecker implements Checker {
    @Override
    public void check(Parser parser, AssertionFeatureMap assertionFeatureMap) {
        //we don't need codeReplacer for syntactic check, since parsing the method (for which the assertion has been generated) is enough to confirm the syntactical correctness of the generated assertion
        assertionFeatureMap
                .getAssertionFeatures()
                .forEach(assertionFeature -> {
                            System.out.println("    Assertion under syntactic check: " + assertionFeature.getAssertion() + " | Line number: " + assertionFeature.getLineNumber());
                            assertionFeature
                                    .setSyntacticallyCorrect(
                                            isSyntacticallyCorrect(
                                                    assertionFeature, parser.getRecord()
                                            )
                                    );
                        }
                );

    }

    private SyntacticCorrectObject isSyntacticallyCorrect(AssertionFeature assertionFeature, Record record) {
        try {
            assertionFeature.embedAssertionAndReturnMethod(record);
            return new SyntacticCorrectObject(true, null);
        } catch (Exception e) {
            return new SyntacticCorrectObject(false, e.getMessage());
        }
    }
}
