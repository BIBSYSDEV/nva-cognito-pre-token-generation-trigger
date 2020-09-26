package no.unit.nva.cognito.model;

import static no.unit.nva.cognito.PostAuthenticationHandler.COMMA_DELIMITER;
import static no.unit.nva.cognito.PostAuthenticationHandler.CUSTOM_APPLICATION;
import static no.unit.nva.cognito.PostAuthenticationHandler.CUSTOM_APPLICATION_ROLES;
import static no.unit.nva.cognito.PostAuthenticationHandler.CUSTOM_CRISTIN_ID;
import static no.unit.nva.cognito.PostAuthenticationHandler.CUSTOM_CUSTOMER_ID;
import static no.unit.nva.cognito.PostAuthenticationHandler.CUSTOM_IDENTIFIERS;
import static no.unit.nva.cognito.PostAuthenticationHandler.FEIDE_PREFIX;
import static no.unit.nva.cognito.PostAuthenticationHandler.NVA;

import com.amazonaws.services.cognitoidp.model.AttributeType;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import no.unit.nva.cognito.api.user.model.UserDto;
import no.unit.nva.cognito.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class User {
    private static final Logger logger = LoggerFactory.getLogger(User.class);
    private final UserAttributes userAttributes;

    private String userPoolId;
    private String cognitoUsername;

    private UserDto apiUser;
    private UserService userService;

    public User(String userPoolId, String cognitoUsername, UserDto apiUser, UserAttributes userAttributes, UserService userService) {
        this.userPoolId = userPoolId;
        this.cognitoUsername = cognitoUsername;
        this.apiUser = apiUser;
        this.userAttributes = userAttributes;
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

    public void updateCustomAttributesInUserPool() {
        userService.updateUserAttributes(
            userPoolId,
            cognitoUsername,
            getAttributeTypesToUpdate(userAttributes));
    }

    private List<AttributeType> getAttributeTypesToUpdate(UserAttributes userAttributes) {
        List<AttributeType> userAttributeTypes = new ArrayList<>();

        if (userAttributes.getCustomerId() != null) {
            userAttributeTypes.add(
                toAttributeType(CUSTOM_CUSTOMER_ID, userAttributes.getCustomerId()));
        }
        if (userAttributes.getCristinId() != null) {
            userAttributeTypes.add(
                toAttributeType(CUSTOM_CRISTIN_ID, userAttributes.getCristinId()));
        }
        userAttributeTypes.add(toAttributeType(CUSTOM_APPLICATION, NVA));
        userAttributeTypes.add(toAttributeType(CUSTOM_IDENTIFIERS, FEIDE_PREFIX + userAttributes.getFeideId()));

        String applicationRoles = toRolesString(getRoles());
        logger.info("applicationRoles: " + applicationRoles);
        userAttributeTypes.add(toAttributeType(CUSTOM_APPLICATION_ROLES, applicationRoles));

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
