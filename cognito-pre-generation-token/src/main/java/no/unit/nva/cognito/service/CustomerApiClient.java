package no.unit.nva.cognito.service;

import static nva.commons.utils.attempt.Try.attempt;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;
import no.unit.nva.cognito.model.CustomerResponse;
import nva.commons.utils.Environment;
import nva.commons.utils.attempt.ConsumerWithException;
import nva.commons.utils.attempt.Failure;
import nva.commons.utils.attempt.Try;
import org.apache.http.HttpStatus;
import org.apache.http.client.utils.URIBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CustomerApiClient implements CustomerApi {

    public static final String PATH = "/customer/orgNumber/";
    public static final String CUSTOMER_API_SCHEME = "CUSTOMER_API_SCHEME";
    public static final String CUSTOMER_API_HOST = "CUSTOMER_API_HOST";

    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;
    private final String customerApiScheme;
    private final String customerApiHost;

    private static final Logger logger = LoggerFactory.getLogger(CustomerApiClient.class);

    public CustomerApiClient(HttpClient httpClient,
                             ObjectMapper objectMapper,
                             Environment environment) {
        this.httpClient = httpClient;
        this.objectMapper = objectMapper;
        this.customerApiScheme = environment.readEnv(CUSTOMER_API_SCHEME);
        this.customerApiHost = environment.readEnv(CUSTOMER_API_HOST);
    }

    @Override
    public Optional<CustomerResponse> getCustomer(String orgNumber) {
        logger.info("Requesting customer information for orgNumber: " + orgNumber);
        return fetchCustomerInformation(orgNumber)
            .stream()
            .filter(responseIsSuccessful())
            .map(tryParsingCustomer())
            .findAny()
            .flatMap(this::getValueOrLogError);
    }



    private Optional<HttpResponse<String>> fetchCustomerInformation(String orgNumber) {
        return Try.of(formUri(orgNumber))
            .map(URIBuilder::build)
            .map(this::buildHttpRequest)
            .map(this::sendHttpRequest)
            .toOptional(logResponseError());
    }

    private Function<HttpResponse<String>, Try<CustomerResponse>> tryParsingCustomer() {
        return attempt(this::parseCustomer);
    }

    private Predicate<HttpResponse<String>> responseIsSuccessful() {
        return resp -> resp.statusCode() == HttpStatus.SC_OK;
    }

    private Optional<CustomerResponse> getValueOrLogError(Try<CustomerResponse> valueTry) {
        return valueTry.toOptional(logErrorParsingCustomerInformation());
    }

    private ConsumerWithException<Failure<CustomerResponse>, RuntimeException> logErrorParsingCustomerInformation() {
        return failure -> logger.error("Error parsing customer information");
    }

    private ConsumerWithException<Failure<HttpResponse<String>>, RuntimeException> logResponseError() {
        return failure -> logger.error("Error fetching customer information");
    }

    private CustomerResponse parseCustomer(HttpResponse<String> response)
        throws JsonProcessingException {
        return objectMapper.readValue(response.body(), CustomerResponse.class);
    }

    private HttpResponse<String> sendHttpRequest(HttpRequest httpRequest) throws IOException, InterruptedException {
        return httpClient.send(httpRequest, BodyHandlers.ofString());
    }

    private URIBuilder formUri(String orgNumber) {
        return new URIBuilder()
            .setScheme(customerApiScheme)
            .setHost(customerApiHost)
            .setPath(PATH + orgNumber);
    }

    private HttpRequest buildHttpRequest(URI uri) {
        return HttpRequest.newBuilder()
            .uri(uri)
            .GET()
            .build();
    }
}