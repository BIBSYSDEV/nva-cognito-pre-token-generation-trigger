package no.unit.nva.cognito.api.lambda;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public class GroupOverrideDetails {
    private List<String> groupsToOverride;
    private List<String> iamRolesToOverride;
    private String preferredRole;

    @JsonCreator
    public GroupOverrideDetails(@JsonProperty("groupsToOverride") List<String> groupsToOverride,
                                @JsonProperty("iamRolesToOverride") List<String> iamRolesToOverride,
                                @JsonProperty("preferredRole") String preferredRole) {
        this.groupsToOverride = groupsToOverride;
        this.iamRolesToOverride = iamRolesToOverride;
        this.preferredRole = preferredRole;
    }

    public List<String> getGroupsToOverride() {
        return groupsToOverride;
    }

    public List<String> getIamRolesToOverride() {
        return iamRolesToOverride;
    }

    public String getPreferredRole() {
        return preferredRole;
    }
}
