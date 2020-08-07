package no.unit.nva.cognito.service;

import static nva.commons.utils.attempt.Try.attempt;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.util.Optional;
import java.util.function.Predicate;
import no.unit.nva.cognito.model.User;
import nva.commons.utils.Environment;
import nva.commons.utils.JacocoGenerated;
import nva.commons.utils.SingletonCollector;
import nva.commons.utils.attempt.Failure;
import nva.commons.utils.attempt.Try;
import org.apache.http.HttpStatus;
import org.apache.http.client.utils.URIBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class UserApiClient implements UserApi {

    public static final String PATH = "/users/";
    public static final String USER_API_SCHEME = "USER_API_SCHEME";
    public static final String USER_API_HOST = "USER_API_HOST";
    public static final String ERROR_PARSING_USER_INFORMATION = "Error parsing user information";
    public static final String ERROR_FETCHING_USER_INFORMATION = "Error fetching user information";

    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;
    private final String userApiScheme;
    private final String userApiHost;

    private static final Logger logger = LoggerFactory.getLogger(UserApiClient.class);

    public UserApiClient(HttpClient httpClient, ObjectMapper objectMapper, Environment environment) {
        this.httpClient = httpClient;
        this.objectMapper = objectMapper;
        this.userApiScheme = environment.readEnv(USER_API_SCHEME);
        this.userApiHost = environment.readEnv(USER_API_HOST);
    }

    @Override
    public Optional<User> getUser(String username) {
        logger.info("Requesting user information for username: " + username);
        return fetchUserInformation(username)
            .stream()
            .filter(responseIsSuccessful())
            .map(this::tryParsingUser)
            .collect(SingletonCollector.tryCollect())
            .flatMap(this::flattenNestedAttempts)
            .toOptional(this::logErrorParsingUserInformation);
    }

    @Override
    @JacocoGenerated
    public Optional<User> createUser(User user) {
        logger.info("Requesting user creation for username: " + user.getUsername());
        return createNewUser(user)
            .stream()
            .filter(responseIsSuccessful())
            .map(this::tryParsingUser)
            .collect(SingletonCollector.tryCollect())
            .flatMap(this::flattenNestedAttempts)
            .toOptional(this::logErrorParsingUserInformation);
    }

    private Try<User> flattenNestedAttempts(Try<User> attempt) {
        return attempt;
    }

    private Optional<HttpResponse<String>> createNewUser(User user) {
        return attempt(() -> formUri())
            .map(URIBuilder::build)
            .map(uri -> buildCreateUserRequest(uri, user))
            .map(this::sendHttpRequest)
            .toOptional(failure -> logResponseError(failure));
    }

    private Optional<HttpResponse<String>> fetchUserInformation(String username) {
        return attempt(() -> formUri(username))
            .map(URIBuilder::build)
            .map(this::buildGetUserRequest)
            .map(this::sendHttpRequest)
            .toOptional(failure -> logResponseError(failure));
    }

    private Try<User> tryParsingUser(HttpResponse<String> response) {
        return attempt(() -> parseUser(response));
    }

    private Predicate<HttpResponse<String>> responseIsSuccessful() {
        return resp -> resp.statusCode() == HttpStatus.SC_OK;
    }

    private void logErrorParsingUserInformation(Failure<User> failure) {
        logger.error(ERROR_PARSING_USER_INFORMATION, failure.getException());
    }

    private void logResponseError(Failure<HttpResponse<String>> failure) {
        logger.error(ERROR_FETCHING_USER_INFORMATION, failure.getException());
    }

    private User parseUser(HttpResponse<String> response)
        throws JsonProcessingException {
        return objectMapper.readValue(response.body(), User.class);
    }

    private HttpResponse<String> sendHttpRequest(HttpRequest httpRequest) throws IOException, InterruptedException {
        return httpClient.send(httpRequest, BodyHandlers.ofString());
    }

    private URIBuilder formUri(String username) {
        return new URIBuilder()
            .setScheme(userApiScheme)
            .setHost(userApiHost)
            .setPath(PATH + username);
    }

    private URIBuilder formUri() {
        return new URIBuilder()
            .setScheme(userApiScheme)
            .setHost(userApiHost)
            .setPath(PATH);
    }

    private HttpRequest buildGetUserRequest(URI uri) {
        return HttpRequest.newBuilder()
            .uri(uri)
            .GET()
            .build();
    }

    private HttpRequest buildCreateUserRequest(URI uri, User user) throws JsonProcessingException {
        return HttpRequest.newBuilder()
            .uri(uri)
            .POST(BodyPublishers.ofString(objectMapper.writeValueAsString(user)))
            .build();
    }

}
