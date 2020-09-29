package no.unit.nva.cognito.model;

import static no.unit.nva.cognito.PreTokenGenerationTriggerHandler.COMMA_DELIMITER;
import static no.unit.nva.cognito.PreTokenGenerationTriggerHandler.FEIDE_PREFIX;
import static no.unit.nva.cognito.PreTokenGenerationTriggerHandler.NVA;

import com.amazonaws.services.cognitoidp.model.AttributeType;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import no.unit.nva.cognito.api.lambda.event.UserAttributes;
import no.unit.nva.cognito.api.user.model.UserDto;
import no.unit.nva.cognito.exception.CreateUserFailedException;
import no.unit.nva.cognito.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class User {

    public static final String CUSTOM_ATTRIBUTE_APPLICATION_ROLES = "custom:applicationRoles";
    public static final String CUSTOM_ATTRIBUTE_APPLICATION = "custom:application";
    public static final String CUSTOM_ATTRIBUTE_IDENTIFIERS = "custom:identifiers";
    public static final String CUSTOM_ATTRIBUTE_CUSTOMER_ID = "custom:customerId";
    public static final String CUSTOM_ATTRIBUTE_CRISTIN_ID = "custom:cristinId";
    private static final Logger logger = LoggerFactory.getLogger(User.class);
    private final UserAttributes userAttributes;
    private final boolean newUser;

    private String userPoolId;
    private String cognitoUsername;

    private UserDto apiUser;
    private final UserService userService;

    public User(String userPoolId,
                String cognitoUsername,
                UserDto apiUser,
                UserAttributes userAttributes,
                boolean isNewUser,
                UserService userService) {
        this.userPoolId = userPoolId;
        this.cognitoUsername = cognitoUsername;
        this.apiUser = apiUser;
        this.userAttributes = userAttributes;
        this.newUser = isNewUser;
        this.userService = userService;
    }

    public String getUserPoolId() {
        return userPoolId;
    }

    public void setUserPoolId(String userPoolId) {
        this.userPoolId = userPoolId;
    }

    public String getCognitoUsername() {
        return cognitoUsername;
    }

    public void setCognitoUsername(String cognitoUsername) {
        this.cognitoUsername = cognitoUsername;
    }

    public UserDto getApiUser() {
        return apiUser;
    }

    public void setApiUser(UserDto apiUser) {
        this.apiUser = apiUser;
    }

    public boolean hasCustomerAttributes() {
        return getApiUser().getCristinId() != null && getApiUser().getInstitution() != null;
    }

    /*public void updateCustomAttributesInUserPool() {
        userService.updateUserAttributes(
            userPoolId,
            cognitoUsername,
            getAttributeTypesToUpdate(userAttributes));
    }*/

    public void notifyUserManagementIfNewUser() {
        // TODO: Send aync message to usermgmt
        // so user mgmt can pick up and run API calls for updating:
        // - Cognito User pool (authorative idp)
        // - Dynamodb UsersAndRoles table.  (authorative users)

        // Now temporary fire a thread with fire and forget ..
        // TODO Remove this! Fix implementation in UserManagement.
        // No gurantee this code finishes!
        if (newUser) {
            new Thread(() -> {
                try {
                    userService.temporaryFireAndForgetCreateUser(getApiUser());
                } catch (CreateUserFailedException e) {
                    // Allow conflicts
                    if (!e.isConflictWithExistingUser()) {
                        throw e;
                    }
                    userService.temporaryFireAndForgetUpdateUser(getApiUser());
                }
            }).start();
        }

    }

    private List<AttributeType> getAttributeTypesToUpdate(UserAttributes userAttributes) {
        List<AttributeType> userAttributeTypes = new ArrayList<>();

        if (userAttributes.getCustomerId() != null) {
            userAttributeTypes.add(
                toAttributeType(CUSTOM_ATTRIBUTE_CUSTOMER_ID, userAttributes.getCustomerId()));
        }
        if (userAttributes.getCristinId() != null) {
            userAttributeTypes.add(
                toAttributeType(CUSTOM_ATTRIBUTE_CRISTIN_ID, userAttributes.getCristinId()));
        }
        userAttributeTypes.add(toAttributeType(CUSTOM_ATTRIBUTE_APPLICATION, NVA));
        userAttributeTypes.add(toAttributeType(CUSTOM_ATTRIBUTE_IDENTIFIERS, FEIDE_PREFIX + userAttributes.getFeideId()));

        String applicationRoles = toRolesString(getRoles());
        logger.info("applicationRoles: " + applicationRoles);
        userAttributeTypes.add(toAttributeType(CUSTOM_ATTRIBUTE_APPLICATION_ROLES, applicationRoles));

        return userAttributeTypes;
    }

    private List<Role> getRoles() {
        return apiUser.getRoles();
    }

    private AttributeType toAttributeType(String name, String value) {
        AttributeType attributeType = new AttributeType();
        attributeType.setName(name);
        attributeType.setValue(value);
        return attributeType;
    }

    private String toRolesString(List<Role> roles) {
        return roles
            .stream()
            .map(Role::getRolename)
            .collect(Collectors.joining(COMMA_DELIMITER));
    }
}
