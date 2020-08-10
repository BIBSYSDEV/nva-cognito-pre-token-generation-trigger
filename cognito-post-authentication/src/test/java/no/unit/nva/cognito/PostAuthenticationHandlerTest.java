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
import java.util.Optional;
import no.unit.nva.cognito.model.Event;
import no.unit.nva.cognito.model.Request;
import no.unit.nva.cognito.model.Role;
import no.unit.nva.cognito.model.User;
import no.unit.nva.cognito.model.UserAttributes;
import no.unit.nva.cognito.service.CustomerApi;
import no.unit.nva.cognito.service.UserApi;
import no.unit.nva.cognito.service.UserApiTestClient;
import no.unit.nva.cognito.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

@SuppressWarnings("unchecked")
public class PostAuthenticationHandlerTest {

    public static final String SAMPLE_ORG_NUMBER = "1234567890";
    public static final String SAMPLE_AFFILIATION = "[member, employee, staff]";
    public static final String SAMPLE_FEIDE_ID = "feideId";
    public static final String SAMPLE_CUSTOMER_ID = "http://example.org/customer/123";
    public static final String CREATOR = "Creator";
    public static final String USER = "User";

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
        userApi = new UserApiTestClient();
        awsCognitoIdentityProvider = mock(AWSCognitoIdentityProvider.class);
        userService = new UserService(userApi, awsCognitoIdentityProvider);
        handler = new PostAuthenticationHandler(userService, customerApi);
    }

    @Test
    public void handleRequestUsesExistingUserWhenUserIsFound() {
        prepareMocksWithExistingCustomer();
        prepareMocksWithExistingUser();

        Event requestEvent = createRequestEvent();
        final Event responseEvent = handler.handleRequest(requestEvent, mock(Context.class));

        verify(awsCognitoIdentityProvider, times(2)).adminAddUserToGroup(any());
        verify(awsCognitoIdentityProvider).adminUpdateUserAttributes(any());

        assertEquals(userApi.getUser(SAMPLE_FEIDE_ID).get(), createUserWithCreatorRole());
        assertEquals(requestEvent, responseEvent);
    }

    @Test
    public void handleRequestCreatesUserWithUserRoleWhenNoCustomerIsFound() {
        prepareMocksWithNoCustomer();
        prepareMocksWithNoUser();

        Event requestEvent = createRequestEvent();
        final Event responseEvent = handler.handleRequest(requestEvent, mock(Context.class));

        verify(awsCognitoIdentityProvider).adminAddUserToGroup(any());
        verify(awsCognitoIdentityProvider).adminUpdateUserAttributes(any());

        assertEquals(userApi.getUser(SAMPLE_FEIDE_ID).get(), createUserWithOnlyUserRole());
        assertEquals(requestEvent, responseEvent);
    }

    @Test
    public void handleRequestCreatesUserWithCreatorRoleForStaffUser() {
        prepareMocksWithExistingCustomer();
        prepareMocksWithNoUser();

        Event requestEvent = createRequestEvent();
        final Event responseEvent = handler.handleRequest(requestEvent, mock(Context.class));

        verify(awsCognitoIdentityProvider, times(2)).adminAddUserToGroup(any());
        verify(awsCognitoIdentityProvider).adminUpdateUserAttributes(any());

        assertEquals(userApi.getUser(SAMPLE_FEIDE_ID).get(), createUserWithCreatorRole());
        assertEquals(requestEvent, responseEvent);
    }

    private void prepareMocksWithNoUser() {
        userApi.createUser(null);
    }

    private void prepareMocksWithExistingUser() {
        userApi.createUser(createUserWithCreatorRole());
    }

    private User createUserWithOnlyUserRole() {
        List<Role> roles = new ArrayList<>();
        roles.add(new Role(USER));
        return new User(
            SAMPLE_FEIDE_ID,
            null,
            roles);
    }

    private User createUserWithCreatorRole() {
        List<Role> roles = new ArrayList<>();
        roles.add(new Role(CREATOR));
        roles.add(new Role(USER));
        return new User(
            SAMPLE_FEIDE_ID,
            SAMPLE_CUSTOMER_ID,
            roles);
    }

    private void prepareMocksWithExistingCustomer() {
        when(customerApi.getCustomerId(anyString())).thenReturn(Optional.of(SAMPLE_CUSTOMER_ID));
    }

    private void prepareMocksWithNoCustomer() {
        when(customerApi.getCustomerId(anyString())).thenReturn(Optional.empty());
    }

    private Event createRequestEvent() {
        UserAttributes userAttributes = new UserAttributes();
        userAttributes.setFeideId(SAMPLE_FEIDE_ID);
        userAttributes.setOrgNumber(SAMPLE_ORG_NUMBER);
        userAttributes.setAffiliation(SAMPLE_AFFILIATION);

        Request request = new Request();
        request.setUserAttributes(userAttributes);

        Event event = new Event();
        event.setUserPoolId("userPoolId");
        event.setUserName("userName");
        event.setRequest(request);

        return event;
    }
}
