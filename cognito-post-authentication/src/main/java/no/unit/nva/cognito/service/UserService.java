package no.unit.nva.cognito.service;

import com.amazonaws.services.cognitoidp.AWSCognitoIdentityProvider;
import com.amazonaws.services.cognitoidp.model.AdminAddUserToGroupRequest;
import com.amazonaws.services.cognitoidp.model.AdminUpdateUserAttributesRequest;
import com.amazonaws.services.cognitoidp.model.AttributeType;
import java.util.ArrayList;
import java.util.List;
import no.unit.nva.cognito.model.Role;
import no.unit.nva.cognito.model.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class UserService {

    private final UserApi userApi;
    private final AWSCognitoIdentityProvider awsCognitoIdentityProvider;

    public static final String PUBLISHER = "Publisher";
    public static final String STAFF = "staff";


    public static final String ROLE_GROUP_TEMPLATE = "%sGroup";

    private static final Logger logger = LoggerFactory.getLogger(UserService.class);

    /**
     * Constructor for UserPoolService.
     *
     * @param userApi   userApi
     * @param awsCognitoIdentityProvider    awsCognitoIdentityProvider
     */
    public UserService(UserApi userApi,
                       AWSCognitoIdentityProvider awsCognitoIdentityProvider) {
        this.userApi = userApi;
        this.awsCognitoIdentityProvider = awsCognitoIdentityProvider;
    }

    /**
     * Update groups for user based on provided roles.
     *
     * @param userPoolId    userPoolId
     * @param userName      userName
     * @param roles         roles
     */
    public void updateUserGroups(String userPoolId, String userName, List<Role> roles) {
        roles.forEach(role -> addUserToGroup(userPoolId, userName, toRoleGroupName(role)));
    }

    /**
     * Get user from user catalogue service or create new user if not found.
     *
     * @param feideId       feideId as username
     * @param customerId    customerId as institution
     * @param affiliation   affiliation
     * @return  the user
     */
    public User getOrCreateUser(String feideId, String customerId, String affiliation) {
        return userApi
            .getUser(feideId)
            .orElse(createUser(feideId, customerId, affiliation));
    }

    /**
     * Add user to group defined by groupName.
     *
     * @param userPoolId    userPoolId
     * @param userName      userName
     * @param groupName     groupName
     */
    private void addUserToGroup(String userPoolId, String userName, String groupName) {
        AdminAddUserToGroupRequest request = new AdminAddUserToGroupRequest()
            .withUserPoolId(userPoolId)
            .withUsername(userName)
            .withGroupName(groupName);
        logger.info("Adding User To Group: " + request.toString());
        awsCognitoIdentityProvider.adminAddUserToGroup(request);
    }

    /**
     * Add attributes to user.
     *
     * @param userPoolId    userPoolId
     * @param userName      userName
     * @param attributes    attributes
     */
    public void updateUserAttributes(String userPoolId, String userName, List<AttributeType> attributes) {
        AdminUpdateUserAttributesRequest request = new AdminUpdateUserAttributesRequest()
            .withUserPoolId(userPoolId)
            .withUsername(userName)
            .withUserAttributes(attributes);
        logger.info("Updating User Attributes: " + request.toString());
        awsCognitoIdentityProvider.adminUpdateUserAttributes(request);
    }

    private String toRoleGroupName(Role role) {
        return String.format(ROLE_GROUP_TEMPLATE, role.getRolename());
    }

    private User createUser(String feideId, String customerId, String affiliation) {
        List<Role> roles = createRoles(affiliation);
        User user = new User(feideId, customerId, roles);
        userApi.createUser(user);
        return user;
    }

    private List<Role> createRoles(String affiliation) {
        List<Role> roles = new ArrayList<>();
        if (affiliation.contains(STAFF)) {
            roles.add(new Role(PUBLISHER));
        }
        return roles;
    }

}
