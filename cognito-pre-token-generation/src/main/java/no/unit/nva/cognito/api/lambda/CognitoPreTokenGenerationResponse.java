package no.unit.nva.cognito.api.lambda;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class CognitoPreTokenGenerationResponse {
    @JsonProperty("claimsOverrideDetails")
    private ClaimsOverrideDetails claimsOverrideDetails;

    public CognitoPreTokenGenerationResponse() {
    }

    public ClaimsOverrideDetails getClaimsOverrideDetails() {
        return claimsOverrideDetails;
    }

    public void setClaimsOverrideDetails(ClaimsOverrideDetails claimsOverrideDetails) {
        this.claimsOverrideDetails = claimsOverrideDetails;
    }


}
