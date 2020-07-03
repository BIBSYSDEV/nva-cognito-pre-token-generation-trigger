package no.unit.nva.cognito;

import static no.unit.nva.cognito.PostAuthenticationHandler.CUSTOM_AFFILIATION;
import static no.unit.nva.cognito.PostAuthenticationHandler.CUSTOM_FEIDE_ID;
import static no.unit.nva.cognito.PostAuthenticationHandler.CUSTOM_ORG_NUMBER;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

@SuppressWarnings("unchecked")
public class PostAuthenticationHandlerTest {

    private CustomerApi customerApi;
    private PostAuthenticationHandler handler;
    private AWSCognitoIdentityProvider awsCognitoIdentityProvider;

    /**
     * Set up test environment.
     */
    @BeforeEach
    public void init() {
        customerApi = mock(CustomerApi.class);
        awsCognitoIdentityProvider = mock(AWSCognitoIdentityProvider.class);
        handler = new PostAuthenticationHandler(customerApi, awsCognitoIdentityProvider);
    }

    @Test
    public void handleRequestReturnsEventOnInput() {
        UUID customerId = UUID.randomUUID();
        Map<String,Object> requestEvent = Map.of(
            USER_POOL_ID, "userPoolId",
            USER_NAME, "userName",
            REQUEST, Map.of(
                USER_ATTRIBUTES, Map.of(
                    CUSTOM_ORG_NUMBER, "orgNumber",
                    CUSTOM_AFFILIATION,"[member, employee, staff]",
                    CUSTOM_FEIDE_ID, "feideId"
                )
            )
        );
        when(customerApi.getCustomerId(anyString())).thenReturn(Optional.of(customerId.toString()));
        Map<String, Object> responseEvent = handler.handleRequest(requestEvent, mock(Context.class));

        verify(awsCognitoIdentityProvider).adminAddUserToGroup(any());
        verify(awsCognitoIdentityProvider).adminUpdateUserAttributes(any());

        assertEquals(requestEvent, responseEvent);
    }
    
}
