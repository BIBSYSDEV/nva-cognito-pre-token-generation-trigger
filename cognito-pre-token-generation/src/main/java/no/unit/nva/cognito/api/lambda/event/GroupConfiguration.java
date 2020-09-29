package no.unit.nva.cognito.api.lambda.event;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class GroupConfiguration {
    @JsonProperty("groupsToOverride")
    private List<String> groupsToOverride;
    @JsonProperty("iamRolesToOverride")
    private List<String> iamRolesToOverride;
    @JsonProperty("preferredRole")
    private String preferredRole;

    public List<String> getGroupsToOverride() {
        return Objects.requireNonNullElse(groupsToOverride, Collections.emptyList());
    }

    public void setGroupsToOverride(List<String> groupsToOverride) {
        this.groupsToOverride = groupsToOverride;
    }

    public List<String> getIamRolesToOverride() {
        return Objects.requireNonNullElse(iamRolesToOverride, Collections.emptyList());
    }

    public void setIamRolesToOverride(List<String> iamRolesToOverride) {
        this.iamRolesToOverride = iamRolesToOverride;
    }

    public String getPreferredRole() {
        return preferredRole;
    }

    public void setPreferredRole(String preferredRole) {
        this.preferredRole = preferredRole;
    }
}
