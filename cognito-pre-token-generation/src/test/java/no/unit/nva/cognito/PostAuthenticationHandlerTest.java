package no.unit.nva.cognito;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import com.amazonaws.services.cognitoidp.AWSCognitoIdentityProvider;
import com.amazonaws.services.lambda.runtime.Context;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import no.unit.nva.cognito.model.CustomerResponse;
import no.unit.nva.cognito.model.Event;
import no.unit.nva.cognito.model.Request;
import no.unit.nva.cognito.model.UserAttributes;
import no.unit.nva.cognito.service.CustomerApi;
import no.unit.nva.cognito.service.UserApi;
import no.unit.nva.cognito.service.UserApiMock;
import no.unit.nva.cognito.service.UserService;
import no.unit.nva.useraccessmanagement.dao.exceptions.InvalidEntryInternalException;
import no.unit.nva.useraccessmanagement.model.RoleDto;
import no.unit.nva.useraccessmanagement.model.UserDto;
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
    public void handleRequestUsesExistingUserWhenUserIsFound() throws InvalidEntryInternalException {
        prepareMocksWithExistingCustomer();
        prepareMocksWithExistingUser();

        Map<String, Object> requestEvent = createRequestEvent();
        final Map<String, Object> responseEvent = handler.handleRequest(requestEvent, mock(Context.class));

        verifyNumberOfAttributeUpdatesInCognito(1);

        UserDto expected = createUserWithInstitutionAndCreatorRole();
        UserDto createdUser = getUserFromMock();
        assertEquals(expected, createdUser);
        assertEquals(requestEvent, responseEvent);
    }

    @Test
    public void handleRequestCreatesUserWithUserRoleWhenNoCustomerIsFound() throws InvalidEntryInternalException {
        prepareMocksWithNoCustomer();

        Map<String, Object> requestEvent = createRequestEvent();
        final Map<String, Object> responseEvent = handler.handleRequest(requestEvent, mock(Context.class));

        verifyNumberOfAttributeUpdatesInCognito(1);

        UserDto expected = createUserWithOnlyUserRole();
        UserDto createdUser = getUserFromMock();
        assertEquals(expected, createdUser);
        assertEquals(requestEvent, responseEvent);
    }

    @Test
    public void handleRequestCreatesUserWithCreatorRoleForAffiliatedUser() throws InvalidEntryInternalException {
        prepareMocksWithExistingCustomer();

        Map<String, Object> requestEvent = createRequestEvent();
        final Map<String, Object> responseEvent = handler.handleRequest(requestEvent, mock(Context.class));

        verifyNumberOfAttributeUpdatesInCognito(1);

        UserDto expected = createUserWithInstitutionAndCreatorRole();
        UserDto createdUser = getUserFromMock();
        assertEquals(expected, createdUser);
        assertEquals(requestEvent, responseEvent);
    }

    @Test
    public void handleRequestCreatesUserWithCreatorRoleForNonAffiliatedUser() throws InvalidEntryInternalException {
        prepareMocksWithExistingCustomer();

        Map<String, Object> requestEvent = createRequestEventWithEmptyAffiliation();
        final Map<String, Object> responseEvent = handler.handleRequest(requestEvent, mock(Context.class));

        verifyNumberOfAttributeUpdatesInCognito(1);

        UserDto expected = createUserWithInstitutionAndOnlyUserRole();
        UserDto createdUser = getUserFromMock();
        assertEquals(createdUser, expected);
        assertEquals(requestEvent, responseEvent);
    }

    private void verifyNumberOfAttributeUpdatesInCognito(int numberOfUpdates) {
        verify(awsCognitoIdentityProvider, times(numberOfUpdates)).adminUpdateUserAttributes(any());
    }

    private UserDto getUserFromMock() {
        return userApi.getUser(SAMPLE_FEIDE_ID).get();
    }

    private void prepareMocksWithExistingUser() throws InvalidEntryInternalException {
        userApi.createUser(createUserWithInstitutionAndCreatorRole());
    }

    private void prepareMocksWithExistingCustomer() {
        when(customerApi.getCustomer(anyString())).thenReturn(Optional.of(new CustomerResponse(SAMPLE_CUSTOMER_ID,
            SAMPLE_CRISTIN_ID)));
    }

    private void prepareMocksWithNoCustomer() {
        when(customerApi.getCustomer(anyString())).thenReturn(Optional.empty());
    }

    private UserDto createUserWithOnlyUserRole() throws InvalidEntryInternalException {
        List<RoleDto> roles = new ArrayList<>();
        roles.add(RoleDto.newBuilder().withName(USER).build());
        return userWithRoles(roles);
    }

    private UserDto userWithRoles(List<RoleDto> roles) throws InvalidEntryInternalException {
        return UserDto.newBuilder()
            .withUsername(SAMPLE_FEIDE_ID)
            .withGivenName(SAMPLE_GIVEN_NAME)
            .withFamilyName(SAMPLE_FAMILY_NAME)
            .withRoles(roles)
            .build();
    }

    private UserDto createUserWithInstitutionAndCreatorRole() throws InvalidEntryInternalException {
        List<RoleDto> roles = new ArrayList<>();
        roles.add(RoleDto.newBuilder().withName(CREATOR).build());
        roles.add(RoleDto.newBuilder().withName(USER).build());
        return userWithInstitution(userWithRoles(roles));
    }

    private UserDto createUserWithInstitutionAndOnlyUserRole() throws InvalidEntryInternalException {
        List<RoleDto> roles = new ArrayList<>();
        roles.add(RoleDto.newBuilder().withName(USER).build());
        return userWithInstitution(userWithRoles(roles));
    }

    private UserDto userWithInstitution(UserDto user) throws InvalidEntryInternalException {
        return user.copy().withInstitution(SAMPLE_CUSTOMER_ID).build();
    }

    private Map<String, Object> createRequestEvent() {
        UserAttributes userAttributes = new UserAttributes();
        userAttributes.setFeideId(SAMPLE_FEIDE_ID);
        userAttributes.setOrgNumber(SAMPLE_ORG_NUMBER);
        userAttributes.setAffiliation(SAMPLE_AFFILIATION);
        userAttributes.setGivenName(SAMPLE_GIVEN_NAME);
        userAttributes.setFamilyName(SAMPLE_FAMILY_NAME);

        Request request = new Request();
        request.setUserAttributes(userAttributes);

        Event event = new Event();
        event.setUserPoolId(SAMPLE_USER_POOL_ID);
        event.setUserName(SAMPLE_USER_NAME);
        event.setRequest(request);

        return JsonUtils.objectMapper.convertValue(event, Map.class);
    }

    private Map<String, Object> createRequestEventWithEmptyAffiliation() {
        UserAttributes userAttributes = new UserAttributes();
        userAttributes.setFeideId(SAMPLE_FEIDE_ID);
        userAttributes.setOrgNumber(SAMPLE_ORG_NUMBER);
        userAttributes.setAffiliation(EMPTY_AFFILIATION);
        userAttributes.setGivenName(SAMPLE_GIVEN_NAME);
        userAttributes.setFamilyName(SAMPLE_FAMILY_NAME);

        Request request = new Request();
        request.setUserAttributes(userAttributes);

        Event event = new Event();
        event.setUserPoolId(SAMPLE_USER_POOL_ID);
        event.setUserName(SAMPLE_USER_NAME);
        event.setRequest(request);

        return JsonUtils.objectMapper.convertValue(event, Map.class);
    }
}
