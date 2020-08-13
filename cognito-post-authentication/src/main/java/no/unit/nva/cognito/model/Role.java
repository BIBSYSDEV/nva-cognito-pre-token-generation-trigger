package no.unit.nva.cognito.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import java.util.Objects;
import nva.commons.utils.JacocoGenerated;

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

    @Override
    @JacocoGenerated
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Role role = (Role) o;
        return Objects.equals(getRolename(), role.getRolename());
    }

    @Override
    @JacocoGenerated
    public int hashCode() {
        return Objects.hash(getRolename());
    }
}
