package no.unit.nva.cognito.model;

import java.io.IOException;
import java.nio.file.Path;
import no.unit.nva.cognito.api.lambda.event.Event;
import nva.commons.utils.IoUtils;
import nva.commons.utils.JsonUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class EventTest {

    public static final String SAMPLE_EVENT_JSON = "sample_event.json";

    @Test
    public void inputIsParsedToEventWithOrgNumber() throws IOException {

        String eventJson = IoUtils.stringFromResources(Path.of(SAMPLE_EVENT_JSON));

        Event event = JsonUtils.objectMapper.readValue(eventJson, Event.class);

        Assertions.assertNotNull(event.getRequest().getUserAttributes().getOrgNumber());
    }

}
