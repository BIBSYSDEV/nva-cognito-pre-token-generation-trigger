package no.unit.nva.cognito;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.Map;
import nva.commons.utils.JacocoGenerated;

@JacocoGenerated
public class DebugHandler implements RequestHandler<Map<String, Object>, Map<String, Object>> {

    @Override
    @JacocoGenerated
    public Map<String, Object> handleRequest(Map<String, Object> input, Context context) {
        try {
            new ObjectMapper().writeValue(System.out, input);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return input;
    }
}
