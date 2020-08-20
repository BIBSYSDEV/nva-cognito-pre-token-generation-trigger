package no.unit.nva.cognito;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import nva.commons.utils.JacocoGenerated;

@JacocoGenerated
public class DebugHandler implements RequestHandler<String, String> {

    @Override
    @JacocoGenerated
    public String handleRequest(String input, Context context) {
        System.out.println(input);
        return input;
    }
}
