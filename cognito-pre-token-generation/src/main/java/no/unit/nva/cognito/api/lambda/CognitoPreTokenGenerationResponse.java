package no.unit.nva.cognito.api.lambda;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class CognitoPreTokenGenerationResponse {
    private final ClaimsOverrideDetails claimsOverrideDetails;

    @JsonCreator
    public CognitoPreTokenGenerationResponse(@JsonProperty("claimsOverrideDetails") ClaimsOverrideDetails claimsOverrideDetails) {
        this.claimsOverrideDetails = claimsOverrideDetails;
    }

    public ClaimsOverrideDetails getClaimsOverrideDetails() {
        return claimsOverrideDetails;
    }
}
