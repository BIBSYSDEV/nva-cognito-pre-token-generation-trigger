package no.unit.nva.cognito;

import static no.unit.nva.cognito.util.OrgNumberCleaner.removeCountryPrefix;

import com.amazonaws.services.cognitoidp.AWSCognitoIdentityProviderClient;
import com.amazonaws.services.cognitoidp.model.AttributeType;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.http.HttpClient;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import no.unit.nva.cognito.model.Event;
import no.unit.nva.cognito.model.Role;
import no.unit.nva.cognito.model.User;
import no.unit.nva.cognito.model.UserAttributes;
import no.unit.nva.cognito.service.CustomerApi;
import no.unit.nva.cognito.service.CustomerApiClient;
import no.unit.nva.cognito.service.UserApiClient;
import no.unit.nva.cognito.service.UserService;
import nva.commons.utils.Environment;
import nva.commons.utils.JacocoGenerated;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PostAuthenticationHandler implements RequestHandler<Event, Event> {

    public static final String CUSTOM_APPLICATION_ROLES = "custom:applicationRoles";
    public static final String CUSTOM_APPLICATION = "custom:application";
    public static final String CUSTOM_CUSTOMER_ID = "custom:customerId";
    public static final String CUSTOM_IDENTIFIERS = "custom:identifiers";

    public static final String COMMA_DELIMITER = ",";
    public static final String FEIDE_PREFIX = "feide:";
    public static final String NVA = "NVA";

    public static final String NOT_FOUND_ERROR_MESSAGE = "No customer found for orgNumber: ";

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
    public Event handleRequest(Event event, Context context) {
        String userPoolId = event.getUserPoolId();
        String userName = event.getUserName();

        UserAttributes userAttributes = event.getRequest().getUserAttributes();

        String feideId = userAttributes.getFeideId();
        String customerId = mapOrgNumberToCustomerId(removeCountryPrefix(userAttributes.getOrgNumber()));
        String affiliation = userAttributes.getAffiliation();

        User user = getUserFromCatalogueOrAddUser(feideId, customerId, affiliation);

        updateUserDetailsInUserPool(userPoolId, userName, userAttributes, user);
        updateUserGroupsInUserPool(userPoolId, userName, user);

        return event;
    }

    private void updateUserGroupsInUserPool(String userPoolId, String userName, User user) {
        userService.updateUserGroups(
            userPoolId,
            userName,
            user.getRoles()
        );
    }

    private void updateUserDetailsInUserPool(String userPoolId, String userName, UserAttributes userAttributes,
                                             User user) {
        userService.updateUserAttributes(
            userPoolId,
            userName,
            createUserAttributes(userAttributes, user));
    }

    private User getUserFromCatalogueOrAddUser(String feideId, String customerId, String affiliation) {
        return userService.getOrCreateUser(feideId, customerId, affiliation);
    }

    private String mapOrgNumberToCustomerId(String orgNumber) {
        return customerApi.getCustomerId(orgNumber)
            .orElseThrow(() -> new IllegalStateException(NOT_FOUND_ERROR_MESSAGE + orgNumber));
    }

    private List<AttributeType> createUserAttributes(UserAttributes userAttributes, User user) {
        List<AttributeType> userAttributeTypes = new ArrayList<>();

        userAttributeTypes.add(toAttributeType(CUSTOM_CUSTOMER_ID, user.getInstitution()));
        userAttributeTypes.add(toAttributeType(CUSTOM_APPLICATION, NVA));
        userAttributeTypes.add(toAttributeType(CUSTOM_IDENTIFIERS, FEIDE_PREFIX + userAttributes.getFeideId()));

        String applicationRoles = toRolesString(user.getRoles());
        logger.info("applicationRoles: " + applicationRoles);
        userAttributeTypes.add(toAttributeType(
            CUSTOM_APPLICATION_ROLES, applicationRoles
            )
        );

        return userAttributeTypes;
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
