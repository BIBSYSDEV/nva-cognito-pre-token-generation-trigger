package no.unit.nva.cognito;

import static no.unit.nva.cognito.service.UserApiMock.FIRST_ACCESS_RIGHT;
import static no.unit.nva.cognito.service.UserApiMock.SAMPLE_ACCESS_RIGHTS;
import static no.unit.nva.cognito.service.UserApiMock.SECOND_ACCESS_RIGHT;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import com.amazonaws.services.cognitoidp.AWSCognitoIdentityProvider;
import com.amazonaws.services.cognitoidp.model.AdminUpdateUserAttributesRequest;
import com.amazonaws.services.cognitoidp.model.AdminUpdateUserAttributesResult;
import com.amazonaws.services.cognitoidp.model.AttributeType;
import com.amazonaws.services.lambda.runtime.Context;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import no.unit.nva.cognito.model.CustomerResponse;
import no.unit.nva.cognito.model.Event;
import no.unit.nva.cognito.model.Request;
import no.unit.nva.cognito.model.UserAttributes;
import no.unit.nva.cognito.service.CustomerApi;
import no.unit.nva.cognito.service.UserApi;
import no.unit.nva.cognito.service.UserApiMock;
import no.unit.nva.cognito.service.UserService;
import no.unit.nva.useraccessmanagement.exceptions.InvalidEntryInternalException;
import no.unit.nva.useraccessmanagement.model.RoleDto;
import no.unit.nva.useraccessmanagement.model.UserDto;
import nva.commons.utils.JsonUtils;
import nva.commons.utils.SingletonCollector;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.invocation.InvocationOnMock;

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
    public static final AdminUpdateUserAttributesResult UNUSED_RESULT = null;
    private final AtomicReference<List<AttributeType>> attributeTypesBuffer = new AtomicReference<>();
    private CustomerApi customerApi;
    private UserApi userApi;
    private UserService userService;
    private PostAuthenticationHandler handler;
    private AWSCognitoIdentityProvider awsCognitoIdentityProvider;
    private Context mockContext;

    public PostAuthenticationHandlerTest() {
        mockContext = mock(Context.class);
    }

    /**
     * Set up test environment.
     */
    @BeforeEach
    public void init() {
        customerApi = mock(CustomerApi.class);
        userApi = new UserApiMock();
        awsCognitoIdentityProvider = mockAwsIdentityProvider();
        attributeTypesBuffer.set(null);
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
        mockContext = mock(Context.class);
        final Map<String, Object> responseEvent = handler.handleRequest(requestEvent, mockContext);

        verifyNumberOfAttributeUpdatesInCognito(1);

        UserDto expected = createUserWithInstitutionAndCreatorRole();
        UserDto createdUser = getUserFromMock();
        assertEquals(expected, createdUser);
        assertEquals(requestEvent, responseEvent);
    }

    @Test
    public void handleRequestAddsAccessRightsAttributesToUserPoolAttributesForUserWithRole()
        throws InvalidEntryInternalException {
        prepareMocksWithExistingUser();
        Map<String, Object> requestEvent = createRequestEvent();
        handler.handleRequest(requestEvent, mockContext);
        verifyNumberOfAttributeUpdatesInCognito(1);

        String accessRight = extractAccessRightsFromUserAttributes();

        assertThat(accessRight, containsString(FIRST_ACCESS_RIGHT));
        assertThat(accessRight, containsString(SECOND_ACCESS_RIGHT));
    }

    @Test
    public void handleRequestAddsAccessRightsAsCsvWhenAccessRightsAreMoreThanOne()
        throws InvalidEntryInternalException {
        prepareMocksWithExistingUser();
        Map<String, Object> requestEvent = createRequestEvent();
        handler.handleRequest(requestEvent, mockContext);
        verifyNumberOfAttributeUpdatesInCognito(1);

        String accessRightsString = extractAccessRightsFromUserAttributes();
        Set<String> accessRights = toSet(accessRightsString);
        assertThat(accessRights, is((equalTo(SAMPLE_ACCESS_RIGHTS))));
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

    private Set<String> toSet(String csv) {
        String[] values = csv.split(PostAuthenticationHandler.COMMA_DELIMITER);
        return Arrays.stream(values).collect(Collectors.toSet());
    }

    private String extractAccessRightsFromUserAttributes() {
        return attributeTypesBuffer.get()
            .stream()
            .filter(attr -> attr.getName().equals(PostAuthenticationHandler.CUSTOM_APPLICATION_ACCESS_RIGHTS))
            .map(AttributeType::getValue)
            .collect(SingletonCollector.collect());
    }

    private AWSCognitoIdentityProvider mockAwsIdentityProvider() {
        AWSCognitoIdentityProvider provider = mock(AWSCognitoIdentityProvider.class);
        when(provider.adminUpdateUserAttributes(any(AdminUpdateUserAttributesRequest.class)))
            .thenAnswer(this::storeUserAttributes);

        return provider;
    }

    private AdminUpdateUserAttributesResult storeUserAttributes(InvocationOnMock invocation) {
        AdminUpdateUserAttributesRequest request = invocation.getArgument(0);
        attributeTypesBuffer.set(request.getUserAttributes());
        return UNUSED_RESULT;
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
        roles.add(createRole(USER));
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
        roles.add(createRole(CREATOR));
        roles.add(createRole(USER));
        return userWithInstitution(userWithRoles(roles));
    }

    private RoleDto createRole(String creator) throws InvalidEntryInternalException {
        return RoleDto.newBuilder()
            .withName(creator)
            .withAccessRights(SAMPLE_ACCESS_RIGHTS)
            .build();
    }

    private UserDto createUserWithInstitutionAndOnlyUserRole() throws InvalidEntryInternalException {
        List<RoleDto> roles = new ArrayList<>();
        roles.add(createRole(USER));
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
