package no.unit.nva.cognito.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class CustomerResponse {

    private final String identifier;

    @JsonCreator
    public CustomerResponse(@JsonProperty("identifier") String identifier) {
        this.identifier = identifier;
    }

    public String getIdentifier() {
        return identifier;
    }
}
