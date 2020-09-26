package no.unit.nva.cognito.api.user.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import java.util.List;
import java.util.Objects;
import no.unit.nva.cognito.model.Role;
import nva.commons.utils.JacocoGenerated;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
public class UserDto {
    private final String localFederatedUsername;
    private final String givenName;
    private final String familyName;
    private final String institution;
    private final String cristinId;
    private final List<Role> roles;
    
    @JsonCreator
    public UserDto(@JsonProperty("username") String username,
                   @JsonProperty("givenName") String givenName,
                   @JsonProperty("familyName") String familyName,
                   @JsonProperty("institution") String institution,
                   @JsonProperty("cristinId") String cristinId,
                   @JsonProperty("roles") List<Role> roles) {
        this.localFederatedUsername = username;
        this.givenName = givenName;
        this.familyName = familyName;
        this.institution = institution;
        this.cristinId = cristinId;
        this.roles = roles;
    }

    public String getLocalFederatedUsername() {
        return localFederatedUsername;
    }

    public String getGivenName() {
        return givenName;
    }

    public String getFamilyName() {
        return familyName;
    }

    public String getInstitution() {
        return institution;
    }

    public String getCristinId() {
        return cristinId;
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
        UserDto userDto = (UserDto) o;
        return Objects.equals(getLocalFederatedUsername(), userDto.getLocalFederatedUsername())
            && Objects.equals(getGivenName(), userDto.getGivenName())
            && Objects.equals(getFamilyName(), userDto.getFamilyName())
            && Objects.equals(getInstitution(), userDto.getInstitution())
            && Objects.equals(getCristinId(), userDto.getCristinId())
            && Objects.equals(getRoles(), userDto.getRoles());
    }

    @Override
    @JacocoGenerated
    public int hashCode() {
        return Objects.hash(getLocalFederatedUsername(),
            getGivenName(),
            getFamilyName(),
            getInstitution(),
            getCristinId(),
            getRoles());
    }
}
