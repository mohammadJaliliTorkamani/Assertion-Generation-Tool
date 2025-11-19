package org.example;

import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.Parameter;
import com.github.javaparser.ast.type.Type;

import java.util.*;

public class MethodSpecificInfoExtractor {
    private static String describeInputOutputDescription(MethodDeclaration methodDeclaration) {
        Type returnType = methodDeclaration.getType();
        String returnTypeString = returnType.toString();

        NodeList<Parameter> parameters = methodDeclaration.getParameters();
        List<String> parameterList = new ArrayList<>();
        for (Parameter parameter : parameters) {
            Type parameterType = parameter.getType();
            String parameterName = parameter.getNameAsString();
            String parameterString = parameterType + " " + parameterName;
            parameterList.add(parameterString);
        }

        return String.format("The method's returns type is %s and takes %s as its argument%s",
                returnTypeString,
                parameterList.isEmpty() ? "nothing" : String.format("(%s)", String.join(",",
                        parameterList)), parameterList.size() > 1 ? "s" : "");
    }

    public MethodSpecificInfo extract(Parser parser, Inference inference) throws Exception {
        String summary = inference
                .askModel(
                        generateMethodDescriberCommand(
                                parser
                                        .getRecord()
                                        .printMethod(true, true, true)
                        )
                ).getSecond();
        String io_description = describeInputOutputDescription(parser.getRecord().findResolvedMethodDeclaration());
        Map<String, String> dependenciesSummary = extractDependenciesSummary(parser, inference);
        return new MethodSpecificInfo(summary, io_description, dependenciesSummary);
    }

    private Map<String, String> extractDependenciesSummary(Parser parser, Inference inference) throws Exception {
        Map<String, String> descriptionsSummary = new HashMap<>();
        List<MethodDeclaration> invokedDependencySourceCodes = parser.findSelfGraphNodeInCallGraph().getChildren();
        for (MethodDeclaration methodDeclaration : invokedDependencySourceCodes) {
            String summary = inference.askModel(generateMethodDescriberCommand(parser.printMethod(methodDeclaration, false
                    , true, false))).getSecond();
            descriptionsSummary.put(methodDeclaration.getDeclarationAsString(), summary); //passing the method declaration and its body to the LLM and storing the result in map of callExpr and received description
        }
        return descriptionsSummary;
    }

    public LLM_InputContent generateMethodDescriberCommand(String originalMethod) {
        List<String> user = new LinkedList<>();
        user.add(Constants.METHOD_SUMMARIZER_USER_FIELD + originalMethod);
        return new LLM_InputContent(user, null);
    }
}
