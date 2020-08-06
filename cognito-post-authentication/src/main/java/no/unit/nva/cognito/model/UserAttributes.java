package no.unit.nva.cognito.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public class UserAttributes {

    @JsonProperty("custom:feideId")
    private String feideId;

    @JsonProperty("custom:orgNumber")
    private String orgNumber;

    @JsonProperty("custom:affiliation")
    private String affiliation;

    public String getFeideId() {
        return feideId;
    }

    public void setFeideId(String feideId) {
        this.feideId = feideId;
    }

    public String getOrgNumber() {
        return orgNumber;
    }

    public void setOrgNumber(String orgNumber) {
        this.orgNumber = orgNumber;
    }

    public String getAffiliation() {
        return affiliation;
    }

    public void setAffiliation(String affiliation) {
        this.affiliation = affiliation;
    }
}
