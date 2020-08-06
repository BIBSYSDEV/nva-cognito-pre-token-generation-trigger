package no.unit.nva.cognito.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
public class Role {

    private final String rolename;

    @JsonCreator
    public Role(@JsonProperty("rolename") String rolename) {
        this.rolename = rolename;
    }

    public String getRolename() {
        return rolename;
    }
}
