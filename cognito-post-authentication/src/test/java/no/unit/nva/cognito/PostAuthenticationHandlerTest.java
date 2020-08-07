package no.unit.nva.cognito;

import static no.unit.nva.cognito.PostAuthenticationHandler.NOT_FOUND_ERROR_MESSAGE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.amazonaws.services.cognitoidp.AWSCognitoIdentityProvider;
import com.amazonaws.services.lambda.runtime.Context;
import java.util.Collections;
import java.util.Optional;
import java.util.UUID;
import no.unit.nva.cognito.model.Event;
import no.unit.nva.cognito.model.Request;
import no.unit.nva.cognito.model.Role;
import no.unit.nva.cognito.model.User;
import no.unit.nva.cognito.model.UserAttributes;
import no.unit.nva.cognito.service.CustomerApi;
import no.unit.nva.cognito.service.UserApi;
import no.unit.nva.cognito.service.UserService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

@SuppressWarnings("unchecked")
public class PostAuthenticationHandlerTest {

    public static final String SAMPLE_ORG_NUMBER = "1234567890";
    public static final String SAMPLE_AFFILIATION = "[member, employee, staff]";
    public static final String SAMPLE_FEIDE_ID = "feideId";
    public static final String SAMPLE_CUSTOMER_ID = "http://example.org/customer/123";
    public static final String PUBLISHER = "Publisher";
    public static final String SAMPLE_USER_POOL_ID = "userPoolId";
    public static final String SAMPLE_USER_NAME = "userName";

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
        userApi = mock(UserApi.class);
        awsCognitoIdentityProvider = mock(AWSCognitoIdentityProvider.class);
        userService = new UserService(userApi, awsCognitoIdentityProvider);
        handler = new PostAuthenticationHandler(userService, customerApi);
    }

    @Test
    public void handleRequestUpdatesUserPoolWithExistingUserWhenUserIsFound() {
        UUID customerId = UUID.randomUUID();
        prepareMocksWithExistingCustomer(customerId);
        prepareMocksWithExistingUser();

        Event requestEvent = createRequestEvent();
        final Event responseEvent = handler.handleRequest(requestEvent, mock(Context.class));

        verify(userApi, times(0)).createUser(any());
        verify(awsCognitoIdentityProvider).adminAddUserToGroup(any());
        verify(awsCognitoIdentityProvider).adminUpdateUserAttributes(any());

        assertEquals(requestEvent, responseEvent);
    }

    @Test
    public void handleRequestUpdatesUserPoolWithNewUserWhenUserIsNotFound() {
        UUID customerId = UUID.randomUUID();
        prepareMocksWithExistingCustomer(customerId);
        prepareMocksWithNoUser();
        prepareMocksWithUserCreated();

        Event requestEvent = createRequestEvent();
        final Event responseEvent = handler.handleRequest(requestEvent, mock(Context.class));

        verify(userApi).createUser(any());
        verify(awsCognitoIdentityProvider).adminAddUserToGroup(any());
        verify(awsCognitoIdentityProvider).adminUpdateUserAttributes(any());

        assertEquals(requestEvent, responseEvent);
    }

    @Test
    public void handleRequestReturnsErrorWhenCustomerIsNotFound() {
        prepareMocksWithNoCustomer();

        Event event = createRequestEvent();

        IllegalStateException exception = Assertions.assertThrows(IllegalStateException.class,
            () -> handler.handleRequest(event, mock(Context.class)));

        Assertions.assertEquals(NOT_FOUND_ERROR_MESSAGE + SAMPLE_ORG_NUMBER, exception.getMessage());
    }

    @Test
    public void handleRequestReturnsErrorWhenCustomerIsCreated() {
        prepareMocksWithExistingCustomer(UUID.randomUUID());
        prepareMocksWithNoUserCreated();

        Event event = createRequestEvent();

        IllegalStateException exception = Assertions.assertThrows(IllegalStateException.class,
            () -> handler.handleRequest(event, mock(Context.class)));

        Assertions.assertEquals(UserService.USER_CREATION_ERROR_MESSAGE + SAMPLE_FEIDE_ID, exception.getMessage());
    }

    private void prepareMocksWithUserCreated() {
        when(userApi.createUser(any())).thenReturn(Optional.of(createUser()));
    }

    private void prepareMocksWithNoUserCreated() {
        when(userApi.createUser(any())).thenReturn(Optional.empty());
    }

    private void prepareMocksWithNoUser() {
        when(userApi.getUser(anyString())).thenReturn(Optional.empty());
    }

    private void prepareMocksWithExistingUser() {
        when(userApi.getUser(anyString())).thenReturn(Optional.of(createUser()));
    }

    private User createUser() {
        return new User(
            SAMPLE_FEIDE_ID,
            SAMPLE_CUSTOMER_ID,
            Collections.singletonList(new Role(PUBLISHER)));
    }

    private void prepareMocksWithExistingCustomer(UUID customerId) {
        when(customerApi.getCustomerId(anyString())).thenReturn(Optional.of(customerId.toString()));
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
        event.setUserPoolId(SAMPLE_USER_POOL_ID);
        event.setUserName(SAMPLE_USER_NAME);
        event.setRequest(request);

        return event;
    }
}
