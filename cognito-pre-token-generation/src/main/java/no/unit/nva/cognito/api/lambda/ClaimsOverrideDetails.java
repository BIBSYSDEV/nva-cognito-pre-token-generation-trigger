package no.unit.nva.cognito.api.lambda;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import java.util.Map;

public class ClaimsOverrideDetails {
    private Map<String, String> claimsToAddOrOverride;
    private List<String> claimsToSuppress;
    private GroupOverrideDetails groupsOverrideDetails;

    @JsonCreator
    public ClaimsOverrideDetails(@JsonProperty("claimsToAddOrOverride") Map<String, String> claimsToAddOrOverride,
                                 @JsonProperty("claimsToSuppress") List<String> claimsToSuppress,
                                 @JsonProperty("groupsOverrideDetails") GroupOverrideDetails groupsOverrideDetails) {
        this.claimsToAddOrOverride = claimsToAddOrOverride;
        this.claimsToSuppress = claimsToSuppress;
        this.groupsOverrideDetails = groupsOverrideDetails;
    }

    public Map<String, String> getClaimsToAddOrOverride() {
        return claimsToAddOrOverride;
    }

    public List<String> getClaimsToSuppress() {
        return claimsToSuppress;
    }

    public GroupOverrideDetails getGroupsOverrideDetails() {
        return groupsOverrideDetails;
    }
}
