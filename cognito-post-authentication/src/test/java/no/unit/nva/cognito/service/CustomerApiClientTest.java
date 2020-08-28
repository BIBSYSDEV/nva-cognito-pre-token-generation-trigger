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
import no.unit.nva.cognito.model.CustomerResponse;
import nva.commons.utils.Environment;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

@SuppressWarnings("unchecked")
public class CustomerApiClientTest {

    public static final String HTTP = "http";
    public static final String EXAMPLE_ORG = "example.org";
    public static final String ORG_NUMBER = "orgNumber";
    public static final String GARBAGE_JSON = "{{}";
    public static final String SAMPLE_ID = "http://link.to.id";

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
    public void getCustomerReturnsCustomerIdentifierOnInput() throws IOException, InterruptedException {
        when(httpResponse.body()).thenReturn(customerResponseWithIdentifier());
        when(httpResponse.statusCode()).thenReturn(SC_OK);
        when(httpClient.send(any(), any())).thenReturn(httpResponse);

        Optional<CustomerResponse> customer = customerApiClient.getCustomer(ORG_NUMBER);

        assertEquals(SAMPLE_ID, customer.get().getIdentifier());
    }

    @Test
    public void getCustomerReturnsCristinIdOnInput() throws IOException, InterruptedException {
        when(httpResponse.body()).thenReturn(customerResponseWithCristinId());
        when(httpResponse.statusCode()).thenReturn(SC_OK);
        when(httpClient.send(any(), any())).thenReturn(httpResponse);

        Optional<CustomerResponse> customer = customerApiClient.getCustomer(ORG_NUMBER);

        assertEquals(SAMPLE_ID, customer.get().getCristinId());
    }

    @Test
    public void getCustomerReturnsEmptyOptionalOnInvalidJsonResponse() throws IOException, InterruptedException {
        when(httpResponse.body()).thenReturn(GARBAGE_JSON);
        when(httpResponse.statusCode()).thenReturn(SC_OK);
        when(httpClient.send(any(), any())).thenReturn(httpResponse);

        Optional<CustomerResponse> customer = customerApiClient.getCustomer(ORG_NUMBER);

        assertTrue(customer.isEmpty());
    }

    @Test
    public void getCustomerReturnsEmptyOptionalOnInvalidHttpResponse() throws IOException, InterruptedException {
        when(httpClient.send(any(), any())).thenThrow(IOException.class);

        Optional<CustomerResponse> customer = customerApiClient.getCustomer(ORG_NUMBER);

        assertTrue(customer.isEmpty());
    }

    private String customerResponseWithIdentifier() {
        return "{\"identifier\":\"" + SAMPLE_ID + "\"}";
    }

    private String customerResponseWithCristinId() {
        return "{\"cristinId\":\"" + SAMPLE_ID + "\"}";
    }
}
