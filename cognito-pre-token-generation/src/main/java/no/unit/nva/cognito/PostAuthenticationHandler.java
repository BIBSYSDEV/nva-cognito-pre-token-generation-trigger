package no.unit.nva.cognito;

import static no.unit.nva.cognito.util.OrgNumberCleaner.removeCountryPrefix;

import com.amazonaws.services.cognitoidp.AWSCognitoIdentityProviderClient;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.net.http.HttpClient;
import java.util.Map;
import java.util.Optional;
import no.unit.nva.cognito.model.CustomerResponse;
import no.unit.nva.cognito.model.Event;
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
    public static final String CUSTOM_CRISTIN_ID = "custom:cristinId";

    public static final String COMMA_DELIMITER = ",";
    public static final String FEIDE_PREFIX = "feide:";
    public static final String NVA = "NVA";
    private static final Logger logger = LoggerFactory.getLogger(PostAuthenticationHandler.class);
    public static final String TRIGGER_SOURCE__TOKEN_GENERATION_PREFIX = "TokenGeneration_";
    public static final String TRIGGER_SOURCE__TOKEN_GENERATION_REFRESH_TOKENS = "TokenGeneration_RefreshTokens";
    private final UserService userService;
    private final CustomerApi customerApi;

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

        Event event = parseEventFromInput(input);
        logger.info("event from source: " + event.getTriggerSource());
        //if (!TRIGGER_SOURCE__TOKEN_GENERATION_HOSTED_AUTH.equals(event.getTriggerSource())) {
        //    logger.info("Quick return: " + event.getTriggerSource());
        //    return input;
        //}
        String userPoolId = event.getUserPoolId();
        String userName = event.getUserName();

        UserAttributes userAttributes = event.getRequest().getUserAttributes();

        Optional<CustomerResponse> customer = mapOrgNumberToCustomer(
            removeCountryPrefix(userAttributes.getOrgNumber()));

        Optional<String> customerId = customer.map(CustomerResponse::getCustomerId);
        Optional<String> cristinId = customer.map(CustomerResponse::getCristinId);

        customerId.ifPresent(userAttributes::setCustomerId);
        cristinId.ifPresent(userAttributes::setCristinId);

        User user = userService.getOrCreateUserFromToken(
            userPoolId,
            userName,
            userAttributes
        );
        user.updateCustomAttributesInUserPool();

        if (TRIGGER_SOURCE__TOKEN_GENERATION_PREFIX.startsWith(event.getTriggerSource())) {
            if (customerId.isPresent() && cristinId.isPresent()) {
                ObjectNode claimsToAddOrOverride = JsonUtils.objectMapper.createObjectNode();
                customerId.ifPresent(v -> claimsToAddOrOverride.put("custom:customerId", v));
                cristinId.ifPresent(v -> claimsToAddOrOverride.put("custom:cristinId", v));
                customerId.ifPresent(v -> claimsToAddOrOverride.put("customerId", v));
                cristinId.ifPresent(v -> claimsToAddOrOverride.put("cristinId", v));
                var claimsOverrideDetails = JsonUtils.objectMapper.createObjectNode()
                    .set("claimsToAddOrOverride", claimsToAddOrOverride);

                input.put("response", JsonUtils.objectMapper.createObjectNode()
                    .set("claimsOverrideDetails", claimsOverrideDetails));
            } else {
                input.put("response", JsonUtils.objectMapper.createObjectNode());
            }
        }
        return input;

    }

    /**
     * Using ObjectMapper to convert input to Event because we are interested in only some input properties but have not
     * way of telling Lambda's JSON parser to ignore the rest.
     *
     * @param input event json as map
     * @return event
     */
    private Event parseEventFromInput(Map<String, Object> input) {
        return JsonUtils.objectMapper.convertValue(input, Event.class);
    }

    private Optional<CustomerResponse> mapOrgNumberToCustomer(String orgNumber) {
        return customerApi.getCustomer(orgNumber);
    }
}
