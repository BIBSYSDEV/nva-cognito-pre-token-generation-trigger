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
import java.util.Map;
import java.util.Optional;
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
import nva.commons.utils.JsonUtils;
import nva.commons.utils.aws.SecretsReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PostAuthenticationHandler implements RequestHandler<Map<String, Object>, Map<String, Object>> {

    public static final String CUSTOM_APPLICATION_ROLES = "custom:applicationRoles";
    public static final String CUSTOM_APPLICATION = "custom:application";
    public static final String CUSTOM_CUSTOMER_ID = "custom:customerId";
    public static final String CUSTOM_IDENTIFIERS = "custom:identifiers";

    public static final String COMMA_DELIMITER = ",";
    public static final String FEIDE_PREFIX = "feide:";
    public static final String NVA = "NVA";

    private final UserService userService;
    private final CustomerApi customerApi;

    private static final Logger logger = LoggerFactory.getLogger(PostAuthenticationHandler.class);

    @JacocoGenerated
    public PostAuthenticationHandler() {
        this(newUserService(), newCustomerApiClient());
    }

    public PostAuthenticationHandler(UserService userService, CustomerApi customerApi) {
        this.userService = userService;
        this.customerApi = customerApi;
    }

    @JacocoGenerated
    private static CustomerApiClient newCustomerApiClient() {
        return new CustomerApiClient(
            HttpClient.newHttpClient(),
            new ObjectMapper(),
            new Environment());
    }

    @JacocoGenerated
    private static SecretsReader defaultSecretsReader() {
        return new SecretsReader();
    }

    @JacocoGenerated
    private static UserService newUserService() {
        return new UserService(
            defaultUserApiClient(),
            AWSCognitoIdentityProviderClient.builder().build()
        );
    }

    @JacocoGenerated
    private static UserApiClient defaultUserApiClient() {
        return new UserApiClient(
            HttpClient.newHttpClient(),
            new ObjectMapper(),
            defaultSecretsReader(),
            new Environment());
    }

    @Override
    public Map<String, Object> handleRequest(Map<String, Object> input, Context context) {

        Event event = JsonUtils.objectMapper.convertValue(input, Event.class);

        String userPoolId = event.getUserPoolId();
        String userName = event.getUserName();

        UserAttributes userAttributes = event.getRequest().getUserAttributes();

        User user = getUserFromCatalogueOrAddUser(userAttributes);

        updateUserDetailsInUserPool(userPoolId, userName, userAttributes, user);

        return input;
    }

    private void updateUserDetailsInUserPool(String userPoolId, String userName, UserAttributes userAttributes,
                                             User user) {
        userService.updateUserAttributes(
            userPoolId,
            userName,
            createUserAttributes(userAttributes, user));
    }

    private User getUserFromCatalogueOrAddUser(UserAttributes userAttributes) {
        String feideId = userAttributes.getFeideId();
        Optional<String> customerId = mapOrgNumberToCustomerId(removeCountryPrefix(userAttributes.getOrgNumber()));
        String affiliation = userAttributes.getAffiliation();
        return userService.getOrCreateUser(feideId, customerId, affiliation);
    }

    private Optional<String> mapOrgNumberToCustomerId(String orgNumber) {
        return customerApi.getCustomerId(orgNumber);
    }

    private List<AttributeType> createUserAttributes(UserAttributes userAttributes, User user) {
        List<AttributeType> userAttributeTypes = new ArrayList<>();

        if (user.getInstitution() != null) {
            userAttributeTypes.add(toAttributeType(CUSTOM_CUSTOMER_ID, user.getInstitution()));
        }
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
