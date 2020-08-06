package no.unit.nva.cognito.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import java.util.List;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
public class User {

    private final String username;
    private final String institution;
    private final List<Role> roles;
    
    @JsonCreator
    public User(@JsonProperty("username") String username,
                @JsonProperty("institution") String institution,
                @JsonProperty("roles") List<Role> roles) {
        this.username = username;
        this.institution = institution;
        this.roles = roles;
    }

    public String getUsername() {
        return username;
    }

    public String getInstitution() {
        return institution;
    }

    public List<Role> getRoles() {
        return roles;
    }

}
