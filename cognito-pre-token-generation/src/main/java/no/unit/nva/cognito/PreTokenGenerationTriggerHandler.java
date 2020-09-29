package no.unit.nva.cognito;

import static no.unit.nva.cognito.model.User.*;

import com.amazonaws.services.cognitoidp.AWSCognitoIdentityProviderClient;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestStreamHandler;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.http.HttpClient;
import java.util.Map;
import no.unit.nva.cognito.api.lambda.event.ClaimsOverrideDetails;
import no.unit.nva.cognito.api.lambda.event.CognitoPreTokenGenerationResponse;
import no.unit.nva.cognito.api.lambda.event.Event;
import no.unit.nva.cognito.model.User;
import no.unit.nva.cognito.service.CustomerApiClient;
import no.unit.nva.cognito.service.UserApiClient;
import no.unit.nva.cognito.service.UserService;
import nva.commons.utils.Environment;
import nva.commons.utils.JacocoGenerated;
import nva.commons.utils.JsonUtils;
import nva.commons.utils.aws.SecretsReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PreTokenGenerationTriggerHandler implements RequestStreamHandler {

    public static final String COMMA_DELIMITER = ",";
    public static final String FEIDE_PREFIX = "feide:";
    public static final String NVA = "NVA";
    private static final Logger logger = LoggerFactory.getLogger(PreTokenGenerationTriggerHandler.class);
    public static final String TRIGGER_SOURCE__TOKEN_GENERATION_PREFIX = "TokenGeneration_";
    public static final String TRIGGER_SOURCE__TOKEN_GENERATION_REFRESH_TOKENS = "TokenGeneration_RefreshTokens";
    public static final ObjectMapper objectMapper = JsonUtils.objectMapper;
    private final UserService userService;

    @JacocoGenerated
    public PreTokenGenerationTriggerHandler() {
        this(newUserService());
    }

    public PreTokenGenerationTriggerHandler(UserService userService) {
        this.userService = userService;
    }

    @JacocoGenerated
    protected static CustomerApiClient newCustomerApiClient() {
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
            AWSCognitoIdentityProviderClient.builder().build(),
            newCustomerApiClient()
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
    public void handleRequest(InputStream input, OutputStream output, Context context) throws IOException {
        Event event = parseEventFromInput(input);
        logger.info("event from source: " + event.getTriggerSource());

        var cognitoUserPoolId = event.getUserPoolId();
        var cognitoUsername = event.getUserName();

        User user = userService.createUserFromToken(
            cognitoUserPoolId,
            cognitoUsername,
            event.getRequest().getUserAttributes());
        user.notifyUserManagementIfNewUser();

        if (event.getTriggerSource() != null && event.getTriggerSource()
            .startsWith(TRIGGER_SOURCE__TOKEN_GENERATION_PREFIX)) {

            var response = new CognitoPreTokenGenerationResponse();

            if (user.hasCustomerAttributes()) {
                var claimsToAddOrOverride = new ClaimsOverrideDetails();
                claimsToAddOrOverride.setClaimsToAddOrOverride(Map.of(
                    CUSTOM_ATTRIBUTE_CUSTOMER_ID, user.getApiUser().getInstitution(),
                    CUSTOM_ATTRIBUTE_CRISTIN_ID, user.getApiUser().getCristinId()
                ));
                response.setClaimsOverrideDetails(claimsToAddOrOverride);
            }
            event.setResponse(response);
        }
        objectMapper.writeValue(output, event);
    }

    /**
     * Using ObjectMapper to convert input to Event because we are interested in only some input properties but have not
     * way of telling Lambda's JSON parser to ignore the rest.
     *
     * @param input event json as map
     * @return event
     */
    private Event parseEventFromInput(InputStream input) throws IOException {
        return objectMapper.readValue(input, Event.class);
    }
}
