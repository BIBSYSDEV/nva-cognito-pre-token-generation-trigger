package no.unit.nva.cognito.api.lambda.event;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public class GroupOverrideDetails {

    @JsonProperty("groupsToOverride")
    private List<String> groupsToOverride;
    @JsonProperty("iamRolesToOverride")
    private List<String> iamRolesToOverride;
    @JsonProperty("preferredRole")
    private String preferredRole;

    public GroupOverrideDetails() {
    }

    public List<String> getGroupsToOverride() {
        return groupsToOverride;
    }

    public void setGroupsToOverride(List<String> groupsToOverride) {
        this.groupsToOverride = groupsToOverride;
    }

    public List<String> getIamRolesToOverride() {
        return iamRolesToOverride;
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
