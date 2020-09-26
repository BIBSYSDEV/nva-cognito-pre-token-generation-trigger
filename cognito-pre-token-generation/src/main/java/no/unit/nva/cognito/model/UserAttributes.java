package no.unit.nva.cognito.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public class UserAttributes {

    @JsonProperty("custom:feideId")
    private String feideId;

    @JsonProperty("custom:orgNumber")
    private String orgNumber;

    @JsonProperty("custom:affiliation")
    private String affiliation;

    @JsonProperty("custom:cristinId")
    private String cristinId; // populated by our triggers

    @JsonProperty("custom:customerId")
    private String customerId; // populated by our triggers

    @JsonProperty("given_name")
    private String givenName;

    @JsonProperty("family_name")
    private String familyName;

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

    public String getGivenName() {
        return givenName;
    }

    public void setGivenName(String givenName) {
        this.givenName = givenName;
    }

    public String getFamilyName() {
        return familyName;
    }

    public void setFamilyName(String familyName) {
        this.familyName = familyName;
    }

    public String getCristinId() {
        return cristinId;
    }

    public void setCristinId(String cristinId) {
        this.cristinId = cristinId;
    }

    public String getCustomerId() {
        return customerId;
    }

    public void setCustomerId(String customerId) {
        this.customerId = customerId;
    }
}
