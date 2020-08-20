package no.unit.nva.cognito.model;

import java.io.File;
import java.io.IOException;
import nva.commons.utils.JsonUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class EventTest {

    public static final String SAMPLE_EVENT_JSON = "src/test/resources/sample_event.json";

    @Test
    public void inputIsParsedToEventWithOrgNumber() throws IOException {
        File eventJsonFile = new File(SAMPLE_EVENT_JSON);

        Event event = JsonUtils.objectMapper.readValue(eventJsonFile, Event.class);

        Assertions.assertNotNull(event.getRequest().getUserAttributes().getOrgNumber());
    }

}
