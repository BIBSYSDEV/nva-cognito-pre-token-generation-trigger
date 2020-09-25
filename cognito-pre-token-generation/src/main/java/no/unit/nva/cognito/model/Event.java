package no.unit.nva.cognito.model;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.HashMap;
import java.util.Map;
import nva.commons.utils.JacocoGenerated;

public class Event {

    @JsonProperty("triggerSource")
    private String triggerSource;
    @JsonProperty("userPoolId")
    private String userPoolId;
    @JsonProperty("userName")
    private String userName;
    @JsonProperty("request")
    private Request request;
    @JsonAnySetter
    private Map<String, Object> otherProperties;

    public Event() {
        otherProperties = new HashMap<>();
    }

    public String getTriggerSource() {
        return triggerSource;
    }

    public void setTriggerSource(String triggerSource) {
        this.triggerSource = triggerSource;
    }

    public String getUserPoolId() {
        return userPoolId;
    }

    public void setUserPoolId(String userPoolId) {
        this.userPoolId = userPoolId;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public Request getRequest() {
        return request;
    }

    public void setRequest(Request request) {
        this.request = request;
    }

    @JsonAnyGetter
    @JacocoGenerated
    public Map<String, Object> getOtherProperties() {
        return otherProperties;
    }

    @JacocoGenerated
    public void setOtherProperties(Map<String, Object> otherProperties) {
        this.otherProperties = otherProperties;
    }
}
