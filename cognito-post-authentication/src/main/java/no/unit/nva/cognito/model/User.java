package no.unit.nva.cognito.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import java.util.List;
import java.util.Objects;
import nva.commons.utils.JacocoGenerated;

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

    @Override
    @JacocoGenerated
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        User user = (User) o;
        return Objects.equals(getUsername(), user.getUsername())
            && Objects.equals(getInstitution(), user.getInstitution())
            && Objects.equals(getRoles(), user.getRoles());
    }

    @Override
    @JacocoGenerated
    public int hashCode() {
        return Objects.hash(getUsername(), getInstitution(), getRoles());
    }
}
