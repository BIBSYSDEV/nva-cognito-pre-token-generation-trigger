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
import no.unit.nva.cognito.exception.CreateUserFailedException;
import no.unit.nva.useraccessmanagement.model.UserDto;
import nva.commons.exceptions.ForbiddenException;
import nva.commons.utils.Environment;
import nva.commons.utils.JacocoGenerated;
import nva.commons.utils.SingletonCollector;
import nva.commons.utils.attempt.Failure;
import nva.commons.utils.attempt.Try;
import nva.commons.utils.aws.SecretsReader;
import org.apache.http.HttpStatus;
import org.apache.http.client.utils.URIBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class UserApiClient implements UserApi {

    public static final String PATH = "/users-roles-internal/service/users";
    public static final String USER_API_SCHEME = "USER_API_SCHEME";
    public static final String USER_API_HOST = "USER_API_HOST";
    public static final String ERROR_PARSING_USER_INFORMATION = "Error parsing user information";
    public static final String ERROR_FETCHING_USER_INFORMATION = "Error fetching user information";
    public static final String CREATE_USER_ERROR_MESSAGE = "Error creating user in user catalogue";
    public static final String USER_SERVICE_SECRET_NAME = "USER_SERVICE_SECRET_NAME";
    public static final String USER_SERVICE_SECRET_KEY = "USER_SERVICE_SECRET_KEY";
    public static final String AUTHORIZATION = "Authorization";
    public static final String DELIMITER = "/";
    private static final Logger logger = LoggerFactory.getLogger(UserApiClient.class);
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;
    private final SecretsReader secretsReader;
    private final String userServiceSecretName;
    private final String userServiceSecretKey;
    private final String userApiScheme;
    private final String userApiHost;

    public UserApiClient(HttpClient httpClient,
                         ObjectMapper objectMapper,
                         SecretsReader secretsReader,
                         Environment environment) {
        this.httpClient = httpClient;
        this.objectMapper = objectMapper;
        this.secretsReader = secretsReader;
        this.userApiScheme = environment.readEnvOpt(USER_API_SCHEME).orElse("https");
        this.userApiHost = environment.readEnvOpt(USER_API_HOST).orElse("api.dev.nva.aws.unit.no");
        this.userServiceSecretName = environment.readEnvOpt(USER_SERVICE_SECRET_NAME).orElse("UserCatalogueApiKey");
        this.userServiceSecretKey = environment.readEnvOpt(USER_SERVICE_SECRET_KEY).orElse("ApiKey");
    }

    @Override
    public Optional<UserDto> getUser(String username) {
        logger.info("Requesting user information for username: " + username);

        return fetchUserInformation(username)
            .stream()
            .filter(this::responseIsSuccessful)
            .map(this::tryParsingUser)
            .collect(SingletonCollector.tryCollect())
            .flatMap(this::flattenNestedAttempts)
            .toOptional(this::logErrorParsingUserInformation);
    }

    @Override
    @JacocoGenerated
    public UserDto createUser(UserDto user) {
        logger.info("Requesting user creation for username: " + user.getUsername());
        return createNewUser(user)
            .stream()
            .filter(this::responseIsSuccessful)
            .map(this::tryParsingUser)
            .collect(SingletonCollector.tryCollect())
            .flatMap(this::flattenNestedAttempts)
            .orElseThrow(this::logErrorAndReturnException);
    }

    private CreateUserFailedException logErrorAndReturnException(Failure<UserDto> failure) {
        logger.error(failure.getException().getMessage(), failure.getException());
        return new CreateUserFailedException(CREATE_USER_ERROR_MESSAGE);
    }

    private Try<UserDto> flattenNestedAttempts(Try<UserDto> attempt) {
        return attempt;
    }

    private Optional<HttpResponse<String>> createNewUser(UserDto user) {
        return attempt(() -> formUri())
            .map(URIBuilder::build)
            .map(uri -> buildCreateUserRequest(uri, user))
            .map(this::sendHttpRequest)
            .toOptional(failure -> logResponseError(failure));
    }

    private Optional<HttpResponse<String>> fetchUserInformation(String username) {
        Optional<HttpResponse<String>> response = attempt(() -> formUri(username))
            .map(URIBuilder::build)
            .map(this::buildGetUserRequest)
            .map(this::sendHttpRequest)
            .toOptional(failure -> logResponseError(failure));
        return response;
    }

    private Try<UserDto> tryParsingUser(HttpResponse<String> response) {
        Try<UserDto> user = attempt(() -> parseUser(response));
        return user;
    }

    private boolean responseIsSuccessful(HttpResponse<String> response) {
        return response.statusCode() == HttpStatus.SC_OK;
    }

    private void logErrorParsingUserInformation(Failure<UserDto> failure) {
        logger.error(ERROR_PARSING_USER_INFORMATION, failure.getException());
    }

    private void logResponseError(Failure<HttpResponse<String>> failure) {
        logger.error(ERROR_FETCHING_USER_INFORMATION, failure.getException());
    }

    private UserDto parseUser(HttpResponse<String> response)
        throws JsonProcessingException {
        return objectMapper.readValue(response.body(), UserDto.class);
    }

    private HttpResponse<String> sendHttpRequest(HttpRequest httpRequest) throws IOException, InterruptedException {
        return httpClient.send(httpRequest, BodyHandlers.ofString());
    }

    private URIBuilder formUri(String username) {
        return new URIBuilder()
            .setScheme(userApiScheme)
            .setHost(userApiHost)
            .setPath(String.join(DELIMITER, PATH, username));
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

    private HttpRequest buildCreateUserRequest(URI uri, UserDto user)
        throws JsonProcessingException, ForbiddenException {
        return HttpRequest.newBuilder()
            .uri(uri)
            .header(AUTHORIZATION, secretsReader.fetchSecret(userServiceSecretName, userServiceSecretKey))
            .POST(BodyPublishers.ofString(objectMapper.writeValueAsString(user)))
            .build();
    }
}
