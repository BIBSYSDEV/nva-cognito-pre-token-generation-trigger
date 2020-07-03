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
import java.util.Optional;
import no.unit.nva.cognito.service.CustomerApi;
import no.unit.nva.cognito.service.CustomerApiClient;
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
    public static final String PUBLISHER_GROUP = "PublisherGroup";
    public static final String STAFF = "staff";
    public static final String REQUEST = "request";
    public static final String COMMA_DELIMITER = ",";

    private final CustomerApi customerApi;
    private final AWSCognitoIdentityProvider awsCognitoIdentityProvider;

    private static final Logger logger = LoggerFactory.getLogger(PostAuthenticationHandler.class);

    /**
     * Default constructor for PostAuthenticationHandler.
     */
    @JacocoGenerated
    public PostAuthenticationHandler() {
        this(
            new CustomerApiClient(HttpClient.newHttpClient(), new ObjectMapper(), new Environment()),
            AWSCognitoIdentityProviderClient.builder().build()
        );

    }

    /**
     * Constructor for PostAuthenticationHandler.
     *
     * @param customerApi   customerApi
     * @param awsCognitoIdentityProvider    awsCognitoIdentityProvider
     */
    public PostAuthenticationHandler(CustomerApi customerApi, AWSCognitoIdentityProvider awsCognitoIdentityProvider) {
        this.customerApi = customerApi;
        this.awsCognitoIdentityProvider = awsCognitoIdentityProvider;
    }

    @Override
    public Map<String,Object> handleRequest(Map<String,Object> event, Context context) {
        String userPoolId = getStringValue(event, USER_POOL_ID);
        String userName = getStringValue(event, USER_NAME);

        Map<String,Object> userAttributes = getMap(getMap(event, REQUEST), USER_ATTRIBUTES);

        updateUserAttributes(
            userPoolId,
            userName,
            createUserAttributes(userAttributes));

        if (getAffiliation(userAttributes).contains(STAFF)) {
            addUserToGroup(userPoolId, userName, PUBLISHER_GROUP);
        } else {
            logger.info("No staff affiliation for publisher group");
        }

        return event;
    }

    private List<AttributeType> createUserAttributes(Map<String, Object> userAttributes) {
        List<AttributeType> userAttributeTypes = new ArrayList<>();

        createCustomerIdAttribute(userAttributes)
            .ifPresent(customerIdAttribute -> userAttributeTypes.add(customerIdAttribute));
        userAttributeTypes.add(toAttributeType(CUSTOM_APPLICATION, NVA));
        userAttributeTypes.add(createIdentifiersAttribute(userAttributes));

        if (getAffiliation(userAttributes).contains(STAFF)) {
            String applicationRoles = String.join(COMMA_DELIMITER, PUBLISHER);
            logger.info("applicationRoles: " + applicationRoles);
            userAttributeTypes.add(toAttributeType(
                CUSTOM_APPLICATION_ROLES, applicationRoles
                )
            );
        } else {
            logger.info("No staff affiliation for publisher role");
        }

        return userAttributeTypes;
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

    private Optional<AttributeType> createCustomerIdAttribute(Map<String, Object> userAttributes) {
        String orgNumber = getStringValue(userAttributes, CUSTOM_ORG_NUMBER);
        String orgNumberNoPrefix = OrgNumberCleaner.removeCountryPrefix(orgNumber);

        return customerApi.getCustomerId(orgNumberNoPrefix)
            .stream()
            .map(customerId -> toAttributeType(CUSTOM_CUSTOMER_ID, customerId))
            .findFirst();
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
