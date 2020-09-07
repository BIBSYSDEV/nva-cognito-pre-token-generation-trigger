package no.unit.nva.cognito.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class CustomerResponse {

    private final String identifier;
    private final String cristinId;

    @JsonCreator
    public CustomerResponse(
        @JsonProperty("identifier") String identifier,
        @JsonProperty("cristinId") String cristinId) {
        this.identifier = identifier;
        this.cristinId = cristinId;
    }

    public String getIdentifier() {
        return identifier;
    }

    public String getCristinId() {
        return cristinId;
    }

}
