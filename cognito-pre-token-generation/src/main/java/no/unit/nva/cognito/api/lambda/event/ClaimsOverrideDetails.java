package no.unit.nva.cognito.api.lambda.event;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import java.util.Map;

public class ClaimsOverrideDetails {
    @JsonProperty("claimsToAddOrOverride")
    private Map<String, String> claimsToAddOrOverride;
    @JsonProperty("claimsToSuppress")
    private List<String> claimsToSuppress;
    @JsonProperty("groupsOverrideDetails")
    private GroupOverrideDetails groupsOverrideDetails;

    public ClaimsOverrideDetails() {
    }

    public Map<String, String> getClaimsToAddOrOverride() {
        return claimsToAddOrOverride;
    }

    public void setClaimsToAddOrOverride(Map<String, String> claimsToAddOrOverride) {
        this.claimsToAddOrOverride = claimsToAddOrOverride;
    }

    public List<String> getClaimsToSuppress() {
        return claimsToSuppress;
    }

    public void setClaimsToSuppress(List<String> claimsToSuppress) {
        this.claimsToSuppress = claimsToSuppress;
    }

    public GroupOverrideDetails getGroupsOverrideDetails() {
        return groupsOverrideDetails;
    }

    public void setGroupsOverrideDetails(GroupOverrideDetails groupsOverrideDetails) {
        this.groupsOverrideDetails = groupsOverrideDetails;
    }


}
