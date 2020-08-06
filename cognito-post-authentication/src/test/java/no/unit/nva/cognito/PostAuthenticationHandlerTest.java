package no.unit.nva.cognito;

import static no.unit.nva.cognito.PostAuthenticationHandler.NOT_FOUND_ERROR_MESSAGE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.amazonaws.services.cognitoidp.AWSCognitoIdentityProvider;
import com.amazonaws.services.lambda.runtime.Context;
import java.util.Optional;
import java.util.UUID;
import no.unit.nva.cognito.model.Event;
import no.unit.nva.cognito.model.Request;
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

    private CustomerApi customerApi;
    private UserApi userApi;
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
        handler = new PostAuthenticationHandler(new UserService(userApi, awsCognitoIdentityProvider), customerApi);
    }

    @Test
    public void handleRequestReturnsEventOnInput() {
        UUID customerId = UUID.randomUUID();
        prepareMocksWithValidResponse(customerId);

        Event requestEvent = createRequestEvent();
        Event responseEvent = handler.handleRequest(requestEvent, mock(Context.class));

        verify(awsCognitoIdentityProvider).adminAddUserToGroup(any());
        verify(awsCognitoIdentityProvider).adminUpdateUserAttributes(any());

        assertEquals(requestEvent, responseEvent);
    }

    @Test
    public void handleRequestThrowsExceptionOnMissingCustomer() {
        prepareMocksWithEmptyResponse();

        Event event = createRequestEvent();

        IllegalStateException exception = Assertions.assertThrows(IllegalStateException.class,
            () -> handler.handleRequest(event, mock(Context.class)));

        Assertions.assertEquals(NOT_FOUND_ERROR_MESSAGE + SAMPLE_ORG_NUMBER, exception.getMessage());
    }

    private void prepareMocksWithValidResponse(UUID customerId) {
        when(customerApi.getCustomerId(anyString())).thenReturn(Optional.of(customerId.toString()));
    }

    private void prepareMocksWithEmptyResponse() {
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
