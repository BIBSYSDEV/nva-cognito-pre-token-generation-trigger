package no.unit.nva.cognito;

import static no.unit.nva.cognito.util.EventUtils.getMap;
import static no.unit.nva.cognito.util.EventUtils.getStringValue;

import com.amazonaws.services.cognitoidp.AWSCognitoIdentityProviderClient;
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
import no.unit.nva.cognito.service.UserApiClient;
import no.unit.nva.cognito.service.UserService;
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

    public static final String COMMA_DELIMITER = ",";
    public static final String FEIDE_PREFIX = "feide:";
    public static final String NVA = "NVA";

    public static final String REQUEST = "request";
    public static final String NOT_FOUND_EXCEPTION = "No customer found for orgNumber: ";

    private UserService userService;
    private CustomerApi customerApi;

    private static final Logger logger = LoggerFactory.getLogger(PostAuthenticationHandler.class);

    /**
     * Default constructor for PostAuthenticationHandler.
     */
    @JacocoGenerated
    public PostAuthenticationHandler() {
        this(
            new UserService(
                new UserApiClient(HttpClient.newHttpClient(), new ObjectMapper(), new Environment()),
                AWSCognitoIdentityProviderClient.builder().build()
            ),
            new CustomerApiClient(HttpClient.newHttpClient(), new ObjectMapper(), new Environment())
        );

    }

    /**
     * Constructor for PostAuthenticationHandler.
     *
     * @param userService   userService
     * @param customerApi   customerApi
     */
    public PostAuthenticationHandler(UserService userService, CustomerApi customerApi) {
        this.userService = userService;
        this.customerApi = customerApi;
    }

    @Override
    public Map<String,Object> handleRequest(Map<String,Object> event, Context context) {
        String userPoolId = getStringValue(event, USER_POOL_ID);
        String userName = getStringValue(event, USER_NAME);

        Map<String,Object> userAttributes = getMap(getMap(event, REQUEST), USER_ATTRIBUTES);

        String feideId = getStringValue(userAttributes, CUSTOM_FEIDE_ID);
        String customerId = mapOrgNumberToCustomerId(getOrgNumberNoPrefix(userAttributes));
        String affiliation = getAffiliation(userAttributes);

        User user = userService.getOrCreateUser(feideId, customerId, affiliation);

        userService.updateUserAttributes(
            userPoolId,
            userName,
            createUserAttributes(userAttributes, user));

        userService.updateUserGroups(
            userPoolId,
            userName,
            user.getRoles()
        );

        return event;
    }

    private String mapOrgNumberToCustomerId(String orgNumber) {
        return customerApi.getCustomerId(orgNumber)
            .orElseThrow(() -> new IllegalStateException(NOT_FOUND_EXCEPTION + orgNumber));
    }

    private String getOrgNumberNoPrefix(Map<String, Object> userAttributes) {
        String orgNumber = getStringValue(userAttributes, CUSTOM_ORG_NUMBER);
        return OrgNumberCleaner.removeCountryPrefix(orgNumber);
    }

    private AttributeType createIdentifiersAttribute(Map<String, Object> userAttributes) {
        String feideId = getStringValue(userAttributes, CUSTOM_FEIDE_ID);
        return toAttributeType(CUSTOM_IDENTIFIERS, FEIDE_PREFIX + feideId);
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

    private String getAffiliation(Map<String, Object> userAttributes) {
        return getStringValue(userAttributes, CUSTOM_AFFILIATION);
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
