package org.example;

import java.util.LinkedList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class LLMResponseExtractor {
    public List<PredictedAssertion> extract(String llmRawResponse) {
        List<PredictedAssertion> list = new LinkedList<>();
        if (llmRawResponse == null || llmRawResponse.isBlank())
            return list;

        llmRawResponse = llmRawResponse.trim();

        //this line previously didn't use constants from other files (instead, we directly used <JAVA> and </JAVA> literals)
        String regex = "%s\\((\\d+),\\s*(assert .*?;)\\)%s".formatted(Constants.LLM_ASSISTANT_DELIMITER[0], Constants.LLM_ASSISTANT_DELIMITER[1]);
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(llmRawResponse);

        // Loop through matches and print the extracted values
        while (matcher.find()) {
            String lineNumber = matcher.group(1);
            String assertion = matcher.group(2);
            list.add(new PredictedAssertion(assertion, Integer.parseInt(lineNumber)));
        }

        //make it have unique assertions (we avoided overriding PredictedAssertion class to avoid potential side effects)
        List<PredictedAssertion> uniquePredictedAssertions = new LinkedList<>();
        for (PredictedAssertion assertion : list) {
            if (!existsIn(assertion, uniquePredictedAssertions))
                uniquePredictedAssertions.add(assertion);
        }

        return uniquePredictedAssertions;
    }

    private boolean existsIn(PredictedAssertion assertion, List<PredictedAssertion> uniquePredictedAssertions) {
        if (assertion != null && assertion.getAssertion() != null && !assertion.getAssertion().isBlank())
            for (PredictedAssertion uniqueAssertion : uniquePredictedAssertions) {
                if (uniqueAssertion.getAssertion() != null)
                    if (uniqueAssertion.getAssertion().trim().equals(assertion.getAssertion().trim()) && uniqueAssertion.getLineNumber() == assertion.getLineNumber())
                        return true;
            }
        return false;
    }
}
