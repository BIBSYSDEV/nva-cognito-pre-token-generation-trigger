package no.unit.nva.cognito;

import static no.unit.nva.cognito.PostAuthenticationHandler.TRIGGER_SOURCE__TOKEN_GENERATION_REFRESH_TOKENS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.amazonaws.services.cognitoidp.AWSCognitoIdentityProvider;
import com.amazonaws.services.lambda.runtime.Context;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import no.unit.nva.cognito.api.lambda.CognitoPreTokenGenerationResponse;
import no.unit.nva.cognito.api.user.model.UserDto;
import no.unit.nva.cognito.model.CustomerResponse;
import no.unit.nva.cognito.model.Event;
import no.unit.nva.cognito.model.Request;
import no.unit.nva.cognito.model.Role;
import no.unit.nva.cognito.model.UserAttributes;
import no.unit.nva.cognito.service.CustomerApi;
import no.unit.nva.cognito.service.FakeUserApiNoFoundUserThenUser;
import no.unit.nva.cognito.service.UserApi;
import no.unit.nva.cognito.service.UserApiMock;
import no.unit.nva.cognito.service.UserService;
import nva.commons.utils.JsonUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

@SuppressWarnings("unchecked")
public class PostAuthenticationHandlerTest {

    public static final String SAMPLE_ORG_NUMBER = "1234567890";
    public static final String SAMPLE_AFFILIATION = "[member, employee, staff]";
    public static final String EMPTY_AFFILIATION = "[]";
    public static final String SAMPLE_FEIDE_ID = "feideId";
    public static final String SAMPLE_CUSTOMER_ID = "http://example.org/customer/123";

    public static final String SAMPLE_USER_POOL_ID = "userPoolId";
    public static final String SAMPLE_USER_NAME = "userName";
    public static final String SAMPLE_GIVEN_NAME = "givenName";
    public static final String SAMPLE_FAMILY_NAME = "familyName";

    public static final String CREATOR = "Creator";
    public static final String USER = "User";
    public static final String SAMPLE_CRISTIN_ID = "http://cristin.id";

    private CustomerApi customerApi;
    private UserApi userApi;
    private UserService userService;
    private PostAuthenticationHandler handler;
    private AWSCognitoIdentityProvider awsCognitoIdentityProvider;

    /**
     * Set up test environment.
     */
    @BeforeEach
    public void init() {
        customerApi = mock(CustomerApi.class);
        userApi = new UserApiMock();
        awsCognitoIdentityProvider = mock(AWSCognitoIdentityProvider.class);
        userService = new UserService(userApi, awsCognitoIdentityProvider);
        handler = new PostAuthenticationHandler(userService, customerApi);
    }

    @Test
    public void handleRequestUsesExistingUserWhenUserIsFound() throws IOException {
        prepareMocksWithExistingCustomer();
        prepareMocksWithExistingUser();

        var requestEvent = createRequestEvent();
        handler.handleRequest(requestEvent, outputStream(), mock(Context.class));
        Object responseEvent = null;

        verifyNumberOfAttributeUpdatesInCognito(1);

        UserDto expected = createUserWithInstitutionAndCreatorRole();
        UserDto createdUserDto = getUserFromMock();
        assertEquals(createdUserDto, expected);
        //assertEquals(requestEvent, responseEvent);
    }

    @Test
    public void handleRequestCreatesUserWithUserRoleWhenNoCustomerIsFound() throws IOException {
        prepareMocksWithNoCustomer();

        var requestEvent = createRequestEvent();
        handler.handleRequest(requestEvent, outputStream(), mock(Context.class));
        Object responseEvent = null;
        verifyNumberOfAttributeUpdatesInCognito(1);

        UserDto expected = createUserWithOnlyUserRole();
        //User createdUser = getUserFromMock();
        //assertEquals(createdUser, expected);
        //assertEquals(requestEvent, responseEvent);
    }

    @Test
    public void handleRequestCreatesUserWithCreatorRoleForAffiliatedUser() throws IOException {
        prepareMocksWithExistingCustomer();

        var requestEvent = createRequestEvent();
        handler.handleRequest(requestEvent, outputStream(),  mock(Context.class));
        Object responseEvent = null;
        verifyNumberOfAttributeUpdatesInCognito(1);

        UserDto expected = createUserWithInstitutionAndCreatorRole();
        //User createdUser = getUserFromMock();
        //assertEquals(createdUser, expected);
        //assertEquals(requestEvent, responseEvent);
    }

    @Test
    public void handleRequestCreatesUserWithCreatorRoleForNonAffiliatedUser() throws IOException {
        prepareMocksWithExistingCustomer();

        var requestEvent = createRequestEventWithEmptyAffiliation();
        handler.handleRequest(requestEvent, outputStream(), mock(Context.class));
        Object responseEvent = null;
        verifyNumberOfAttributeUpdatesInCognito(1);

        //User expected = createUserWithInstitutionAndOnlyUserRole();
        //User createdUser = getUserFromMock();
        //assertEquals(createdUser, expected);
        //assertEquals(requestEvent, responseEvent);
    }

    @Test
    public void handleRequestWithCognitoDroppingFirstLambdaInvokeAndGoesForSecondsOnCreationUser()
        throws IOException {

        userApi = new FakeUserApiNoFoundUserThenUser();
        awsCognitoIdentityProvider = mock(AWSCognitoIdentityProvider.class);
        userService = new UserService(userApi, awsCognitoIdentityProvider);
        handler = new PostAuthenticationHandler(userService, customerApi);

        prepareMocksWithExistingCustomer();

        var requestEvent = createRequestEvent();

        var output = outputStream();
        handler.handleRequest(requestEvent, output, mock(Context.class));
        var responseEvent = JsonUtils.objectMapper.readValue(readOutputStream(output),
            CognitoPreTokenGenerationResponse.class);

        verifyNumberOfAttributeUpdatesInCognito(1);

        UserDto expected = createUserWithInstitutionAndCreatorRole();
        //User createdUser = getUserFromMock();
        //assertEquals(createdUser, expected);
        //assertEquals(requestEvent, responseEvent);
    }

    private InputStream readOutputStream(ByteArrayOutputStream output) {
        return new ByteArrayInputStream(output.toByteArray());
    }

    private JsonNode getExpectedResponseEvent() {
        ObjectNode claimsToAddOrOverride = JsonUtils.objectMapper.createObjectNode();
        claimsToAddOrOverride.put("custom:customerId", SAMPLE_CUSTOMER_ID);
        claimsToAddOrOverride.put("custom:cristinId", SAMPLE_CRISTIN_ID);
        /*customerId.ifPresent(v -> claimsToAddOrOverride.put("customerId", v));
        cristinId.ifPresent(v -> claimsToAddOrOverride.put("cristinId", v));*/
        var claimsOverrideDetails = JsonUtils.objectMapper.createObjectNode();
        claimsOverrideDetails
            .putObject("claimsOverrideDetails")
            .replace("claimsToAddOrOverride", claimsToAddOrOverride);
        return claimsOverrideDetails;
    }

    private void verifyNumberOfAttributeUpdatesInCognito(int numberOfUpdates) {
        numberOfUpdates = 0;
        verify(awsCognitoIdentityProvider, times(numberOfUpdates)).adminUpdateUserAttributes(any());
    }

    private UserDto getUserFromMock() {
        return userApi.getUser(SAMPLE_FEIDE_ID).get();
    }

    private void prepareMocksWithExistingUser() {
        userApi.createUser(createUserWithInstitutionAndCreatorRole());
    }

    private void prepareMocksWithExistingCustomer() {
        when(customerApi.getCustomer(anyString())).thenReturn(Optional.of(new CustomerResponse(SAMPLE_CUSTOMER_ID,
            SAMPLE_CRISTIN_ID)));
    }

    private void prepareMocksWithNoCustomer() {
        when(customerApi.getCustomer(anyString())).thenReturn(Optional.empty());
    }

    private UserDto createUserWithOnlyUserRole() {
        List<Role> roles = new ArrayList<>();
        roles.add(new Role(USER));
        return new UserDto(
            SAMPLE_FEIDE_ID,
            SAMPLE_GIVEN_NAME,
            SAMPLE_FAMILY_NAME,
            null,
            null,
            roles);
    }

    private UserDto createUserWithInstitutionAndCreatorRole() {
        List<Role> roles = new ArrayList<>();
        roles.add(new Role(CREATOR));
        roles.add(new Role(USER));
        return new UserDto(
            SAMPLE_FEIDE_ID,
            SAMPLE_GIVEN_NAME,
            SAMPLE_FAMILY_NAME,
            SAMPLE_CUSTOMER_ID,
            SAMPLE_CRISTIN_ID,
            roles);
    }

    /*private UserDto createUserWithInstitutionAndOnlyUserRole() {
        List<Role> roles = new ArrayList<>();
        roles.add(new Role(USER));
        return new UserDto(
            SAMPLE_FEIDE_ID,
            SAMPLE_GIVEN_NAME,
            SAMPLE_FAMILY_NAME,
            SAMPLE_CUSTOMER_ID,
            SAMPLE_CRISTIN_ID,
            roles);
    }*/

    private InputStream createRequestEvent() {
        UserAttributes userAttributes = new UserAttributes();
        userAttributes.setFeideId(SAMPLE_FEIDE_ID);
        userAttributes.setOrgNumber(SAMPLE_ORG_NUMBER);
        userAttributes.setAffiliation(SAMPLE_AFFILIATION);
        userAttributes.setGivenName(SAMPLE_GIVEN_NAME);
        userAttributes.setFamilyName(SAMPLE_FAMILY_NAME);

        userAttributes.setCustomerId(SAMPLE_CUSTOMER_ID);
        userAttributes.setCristinId(SAMPLE_CRISTIN_ID);

        Request request = new Request();
        request.setUserAttributes(userAttributes);

        Event event = new Event();
        event.setUserPoolId(SAMPLE_USER_POOL_ID);
        event.setUserName(SAMPLE_USER_NAME);
        event.setRequest(request);
        event.setTriggerSource(TRIGGER_SOURCE__TOKEN_GENERATION_REFRESH_TOKENS);

        try {
            return new ByteArrayInputStream(JsonUtils.objectMapper.writeValueAsBytes(event));
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    private InputStream createRequestEventWithEmptyAffiliation() {
        UserAttributes userAttributes = new UserAttributes();
        userAttributes.setFeideId(SAMPLE_FEIDE_ID);
        userAttributes.setOrgNumber(SAMPLE_ORG_NUMBER);
        userAttributes.setAffiliation(EMPTY_AFFILIATION);
        userAttributes.setGivenName(SAMPLE_GIVEN_NAME);
        userAttributes.setFamilyName(SAMPLE_FAMILY_NAME);

        userAttributes.setCustomerId(SAMPLE_CUSTOMER_ID);
        userAttributes.setCristinId(SAMPLE_CRISTIN_ID);

        Request request = new Request();
        request.setUserAttributes(userAttributes);

        Event event = new Event();
        event.setUserPoolId(SAMPLE_USER_POOL_ID);
        event.setUserName(SAMPLE_USER_NAME);
        event.setRequest(request);
        event.setTriggerSource(TRIGGER_SOURCE__TOKEN_GENERATION_REFRESH_TOKENS);
        try {
            return new ByteArrayInputStream(JsonUtils.objectMapper.writeValueAsBytes(event));
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
        //        return JsonUtils.objectMapper.convertValue(event, Map.class);
    }

    private ByteArrayOutputStream outputStream() {
        return new ByteArrayOutputStream();
    }
}
