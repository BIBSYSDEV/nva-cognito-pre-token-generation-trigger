package no.unit.nva.cognito;

import static no.unit.nva.cognito.PostAuthenticationHandler.CUSTOM_AFFILIATION;
import static no.unit.nva.cognito.PostAuthenticationHandler.CUSTOM_FEIDE_ID;
import static no.unit.nva.cognito.PostAuthenticationHandler.CUSTOM_ORG_NUMBER;
import static no.unit.nva.cognito.PostAuthenticationHandler.NOT_FOUND_EXCEPTION;
import static no.unit.nva.cognito.PostAuthenticationHandler.REQUEST;
import static no.unit.nva.cognito.PostAuthenticationHandler.USER_ATTRIBUTES;
import static no.unit.nva.cognito.PostAuthenticationHandler.USER_NAME;
import static no.unit.nva.cognito.PostAuthenticationHandler.USER_POOL_ID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.amazonaws.services.cognitoidp.AWSCognitoIdentityProvider;
import com.amazonaws.services.lambda.runtime.Context;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
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

        Map<String, Object> requestEvent = createRequestEvent();
        Map<String, Object> responseEvent = handler.handleRequest(requestEvent, mock(Context.class));

        verify(awsCognitoIdentityProvider).adminAddUserToGroup(any());
        verify(awsCognitoIdentityProvider).adminUpdateUserAttributes(any());

        assertEquals(requestEvent, responseEvent);
    }

    @Test
    public void handleRequestThrowsExceptionOnMissingCustomer() {
        prepareMocksWithEmptyResponse();

        Map<String, Object> requestEvent = createRequestEvent();

        IllegalStateException exception = Assertions.assertThrows(IllegalStateException.class,
            () -> handler.handleRequest(requestEvent, mock(Context.class)));

        Assertions.assertEquals(NOT_FOUND_EXCEPTION + SAMPLE_ORG_NUMBER, exception.getMessage());
    }

    private void prepareMocksWithValidResponse(UUID customerId) {
        when(customerApi.getCustomerId(anyString())).thenReturn(Optional.of(customerId.toString()));
    }

    private void prepareMocksWithEmptyResponse() {
        when(customerApi.getCustomerId(anyString())).thenReturn(Optional.empty());
    }

    private Map<String, Object> createRequestEvent() {
        return Map.of(
            USER_POOL_ID, "userPoolId",
            USER_NAME, "userName",
            REQUEST, Map.of(
                USER_ATTRIBUTES, Map.of(
                    CUSTOM_ORG_NUMBER, SAMPLE_ORG_NUMBER,
                    CUSTOM_AFFILIATION, SAMPLE_AFFILIATION,
                    CUSTOM_FEIDE_ID, SAMPLE_FEIDE_ID
                )
            )
        );
    }
}
