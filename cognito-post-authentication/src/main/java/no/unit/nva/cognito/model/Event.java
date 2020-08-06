package no.unit.nva.cognito.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public class Event {

    @JsonProperty("userPoolId")
    private String userPoolId;
    @JsonProperty("userName")
    private String userName;
    @JsonProperty("request")
    private Request request;

    public Event() {
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
}
