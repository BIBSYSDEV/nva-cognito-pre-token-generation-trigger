package no.unit.nva.cognito;

import com.amazonaws.services.cognitoidp.AWSCognitoIdentityProvider;
import com.amazonaws.services.cognitoidp.AWSCognitoIdentityProviderClient;
import com.amazonaws.services.cognitoidp.model.AdminAddUserToGroupRequest;
import com.amazonaws.services.cognitoidp.model.AdminUpdateUserAttributesRequest;
import com.amazonaws.services.cognitoidp.model.AttributeType;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.http.HttpClient;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import no.unit.nva.cognito.model.Role;
import no.unit.nva.cognito.model.User;
import no.unit.nva.cognito.service.CustomerApi;
import no.unit.nva.cognito.service.CustomerApiClient;
import no.unit.nva.cognito.service.UserApi;
import no.unit.nva.cognito.service.UserApiClient;
import no.unit.nva.cognito.util.OrgNumberCleaner;
import nva.commons.utils.Environment;
import nva.commons.utils.JacocoGenerated;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PostAuthenticationHandler implements RequestHandler<Map<String,Object>, Map<String,Object>> {

    public static final String USER_ATTRIBUTES = "userAttributes";
    public static final String USER_NAME = "userName";
    public static final String USER_POOL_ID = "userPoolId";

    public static final String CUSTOM_AFFILIATION = "custom:affiliation";
    public static final String CUSTOM_APPLICATION_ROLES = "custom:applicationRoles";
    public static final String CUSTOM_APPLICATION = "custom:application";
    public static final String CUSTOM_CUSTOMER_ID = "custom:customerId";
    public static final String CUSTOM_FEIDE_ID = "custom:feideId";
    public static final String CUSTOM_IDENTIFIERS = "custom:identifiers";
    public static final String CUSTOM_ORG_NUMBER = "custom:orgNumber";

    public static final String FEIDE_PREFIX = "feide:";
    public static final String NVA = "NVA";
    public static final String PUBLISHER = "Publisher";
    public static final String STAFF = "staff";
    public static final String REQUEST = "request";
    public static final String COMMA_DELIMITER = ",";
    public static final String ROLE_GROUP_TEMPLATE = "%sGroup";

    private final CustomerApi customerApi;
    private final UserApi userApi;
    private final AWSCognitoIdentityProvider awsCognitoIdentityProvider;

    private static final Logger logger = LoggerFactory.getLogger(PostAuthenticationHandler.class);

    /**
     * Default constructor for PostAuthenticationHandler.
     */
    @JacocoGenerated
    public PostAuthenticationHandler() {
        this(
            new CustomerApiClient(HttpClient.newHttpClient(), new ObjectMapper(), new Environment()),
            new UserApiClient(HttpClient.newHttpClient(), new ObjectMapper(), new Environment()),
            AWSCognitoIdentityProviderClient.builder().build()
        );

    }

    /**
     * Constructor for PostAuthenticationHandler.
     *
     * @param customerApi   customerApi
     * @param userApi   userApi
     * @param awsCognitoIdentityProvider    awsCognitoIdentityProvider
     */
    public PostAuthenticationHandler(CustomerApi customerApi, UserApi userApi,
                                     AWSCognitoIdentityProvider awsCognitoIdentityProvider) {
        this.customerApi = customerApi;
        this.userApi = userApi;
        this.awsCognitoIdentityProvider = awsCognitoIdentityProvider;
    }

    @Override
    public Map<String,Object> handleRequest(Map<String,Object> event, Context context) {
        String userPoolId = getStringValue(event, USER_POOL_ID);
        String userName = getStringValue(event, USER_NAME);

        Map<String,Object> userAttributes = getMap(getMap(event, REQUEST), USER_ATTRIBUTES);

        String feideId = getStringValue(userAttributes, CUSTOM_FEIDE_ID);
        String customerId = mapOrgNumberToCustomerId(getOrgNumberNoPrefix(userAttributes));
        String affiliation = getAffiliation(userAttributes);

        User user = getOrCreateUser(feideId, customerId, affiliation);

        updateUserAttributes(
            userPoolId,
            userName,
            createUserAttributes(userAttributes, user));

        updateUserGroups(
            userPoolId,
            userName,
            user.getRoles()
        );

        return event;
    }

    private void updateUserGroups(String userPoolId, String userName, List<Role> roles) {
        roles.stream().forEach(role -> addUserToGroup(userPoolId, userName, toRoleGroupName(role)));
    }

    private String toRoleGroupName(Role role) {
        return String.format(ROLE_GROUP_TEMPLATE, role.getRolename());
    }

    private User getOrCreateUser(String feideId, String customerId, String affiliation) {
        return userApi
            .getUser(feideId)
            .orElse(createUser(feideId, customerId, affiliation));
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

    private String getOrgNumberNoPrefix(Map<String, Object> userAttributes) {
        String orgNumber = getStringValue(userAttributes, CUSTOM_ORG_NUMBER);
        return OrgNumberCleaner.removeCountryPrefix(orgNumber);
    }

    private String mapOrgNumberToCustomerId(String orgNumber) {
        return customerApi.getCustomerId(orgNumber)
            .stream()
            .findFirst()
            .orElseThrow(() -> new IllegalStateException("No customer found for orgNumber: " + orgNumber));
    }

    private List<AttributeType> createUserAttributes(Map<String, Object> userAttributes, User user) {
        List<AttributeType> userAttributeTypes = new ArrayList<>();

        userAttributeTypes.add(toAttributeType(CUSTOM_CUSTOMER_ID, user.getInstitution()));
        userAttributeTypes.add(toAttributeType(CUSTOM_APPLICATION, NVA));
        userAttributeTypes.add(createIdentifiersAttribute(userAttributes));

        String applicationRoles = toRolesString(user.getRoles());
        logger.info("applicationRoles: " + applicationRoles);
        userAttributeTypes.add(toAttributeType(
            CUSTOM_APPLICATION_ROLES, applicationRoles
            )
        );

        return userAttributeTypes;
    }

    private String toRolesString(List<Role> roles) {
        return roles
            .stream()
            .map(Role::getRolename)
            .collect(Collectors.joining(COMMA_DELIMITER));
    }

    private void addUserToGroup(String userPoolId, String userName, String groupName) {
        AdminAddUserToGroupRequest request = new AdminAddUserToGroupRequest()
            .withUserPoolId(userPoolId)
            .withUsername(userName)
            .withGroupName(groupName);
        logger.info("Adding User To Group: " + request.toString());
        awsCognitoIdentityProvider.adminAddUserToGroup(request);
    }

    private void updateUserAttributes(String userPoolId, String userName, List<AttributeType> attributes) {
        AdminUpdateUserAttributesRequest request = new AdminUpdateUserAttributesRequest()
            .withUserPoolId(userPoolId)
            .withUsername(userName)
            .withUserAttributes(attributes);
        logger.info("Updating User Attributes: " + request.toString());
        awsCognitoIdentityProvider.adminUpdateUserAttributes(request);
    }

    private String getAffiliation(Map<String, Object> userAttributes) {
        return getStringValue(userAttributes, CUSTOM_AFFILIATION);
    }

    private AttributeType createIdentifiersAttribute(Map<String, Object> userAttributes) {
        String feideId = getStringValue(userAttributes, CUSTOM_FEIDE_ID);
        return toAttributeType(CUSTOM_IDENTIFIERS, FEIDE_PREFIX + feideId);
    }

    private AttributeType toAttributeType(String name, String value) {
        AttributeType attributeType = new AttributeType();
        attributeType.setName(name);
        attributeType.setValue(value);
        return attributeType;
    }

    @SuppressWarnings("unchecked")
    private Map<String,Object> getMap(Map<String,Object> map, String key) {
        return (Map<String,Object>)map.get(key);
    }

    @SuppressWarnings("unchecked")
    private String getStringValue(Map<String,Object> map, String key) {
        return (String)map.get(key);
    }

}
