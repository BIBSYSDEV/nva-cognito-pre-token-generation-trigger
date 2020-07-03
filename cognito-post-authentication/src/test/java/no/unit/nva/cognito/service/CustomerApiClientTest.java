package no.unit.nva.cognito.service;

import static org.apache.http.HttpStatus.SC_OK;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.net.http.HttpClient;
import java.net.http.HttpResponse;
import java.util.Optional;
import java.util.UUID;
import nva.commons.utils.Environment;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

@SuppressWarnings("unchecked")
public class CustomerApiClientTest {

    public static final String HTTP = "http";
    public static final String EXAMPLE_ORG = "example.org";
    public static final String ORG_NUMBER = "orgNumber";
    public static final String GARBAGE_JSON = "{{}";

    private CustomerApiClient customerApiClient;
    private HttpClient httpClient;
    private HttpResponse httpResponse;

    /**
     * Set up test environment.
     */
    @BeforeEach
    public void init() {
        Environment environment = mock(Environment.class);
        when(environment.readEnv(CustomerApiClient.CUSTOMER_API_SCHEME)).thenReturn(HTTP);
        when(environment.readEnv(CustomerApiClient.CUSTOMER_API_HOST)).thenReturn(EXAMPLE_ORG);
        httpClient = mock(HttpClient.class);
        httpResponse = mock(HttpResponse.class);

        customerApiClient = new CustomerApiClient(httpClient, new ObjectMapper(), environment);
    }

    @Test
    public void getCustomerIdReturnsCustomerIdOnInput() throws IOException, InterruptedException {
        UUID uuid = UUID.randomUUID();
        when(httpResponse.body()).thenReturn("{\"identifier\":\"" + uuid.toString() + "\"}");
        when(httpResponse.statusCode()).thenReturn(SC_OK);
        when(httpClient.send(any(), any())).thenReturn(httpResponse);

        Optional<String> customerId = customerApiClient.getCustomerId(ORG_NUMBER);

        assertEquals(uuid.toString(), customerId.get());
    }

    @Test
    public void getCustomerIdReturnsEmptyOptionalOnInvalidJsonResponse() throws IOException, InterruptedException {
        when(httpResponse.body()).thenReturn(GARBAGE_JSON);
        when(httpResponse.statusCode()).thenReturn(SC_OK);
        when(httpClient.send(any(), any())).thenReturn(httpResponse);

        Optional<String> customerId = customerApiClient.getCustomerId(ORG_NUMBER);

        assertTrue(customerId.isEmpty());
    }

    @Test
    public void getCustomerIdReturnsEmptyOptionalOnInvalidHttpResponse() throws IOException, InterruptedException {
        when(httpClient.send(any(), any())).thenThrow(IOException.class);

        Optional<String> customerId = customerApiClient.getCustomerId(ORG_NUMBER);

        assertTrue(customerId.isEmpty());
    }
}
