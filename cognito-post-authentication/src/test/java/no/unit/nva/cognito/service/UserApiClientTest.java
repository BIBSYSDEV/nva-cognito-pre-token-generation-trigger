package no.unit.nva.cognito.service;

import static no.unit.nva.cognito.service.UserApiClient.ERROR_FETCHING_USER_INFORMATION;
import static no.unit.nva.cognito.service.UserApiClient.ERROR_PARSING_USER_INFORMATION;
import static no.unit.nva.cognito.service.UserApiClient.USER_API_HOST;
import static no.unit.nva.cognito.service.UserApiClient.USER_API_SCHEME;
import static no.unit.nva.cognito.service.UserApiClient.USER_SERVICE_SECRET_KEY;
import static no.unit.nva.cognito.service.UserApiClient.USER_SERVICE_SECRET_NAME;
import static org.apache.http.HttpStatus.SC_INTERNAL_SERVER_ERROR;
import static org.apache.http.HttpStatus.SC_OK;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.net.http.HttpClient;
import java.net.http.HttpResponse;
import java.util.Collections;
import java.util.Optional;
import no.unit.nva.cognito.exception.CreateUserFailedException;
import no.unit.nva.cognito.model.Role;
import no.unit.nva.cognito.model.User;
import nva.commons.exceptions.ForbiddenException;
import nva.commons.utils.Environment;
import nva.commons.utils.aws.SecretsReader;
import nva.commons.utils.log.LogUtils;
import nva.commons.utils.log.TestAppender;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

@SuppressWarnings("unchecked")
public class UserApiClientTest {

    public static final String GARBAGE_JSON = "{{}";
    public static final String SAMPLE_USERNAME = "username";
    public static final String SAMPLE_INSTITUTION_ID = "institution.id";
    public static final String CREATOR = "Creator";
    public static final String THE_API_KEY = "TheApiKey";

    private ObjectMapper objectMapper;
    private UserApiClient userApiClient;
    private HttpClient httpClient;
    private SecretsReader secretsReader;
    private HttpResponse httpResponse;

    /**
     * Set up test environment.
     */
    @BeforeEach
    public void init() {
        objectMapper = new ObjectMapper();
        secretsReader = mock(SecretsReader.class);
        Environment environment = mock(Environment.class);
        when(environment.readEnv(USER_API_SCHEME)).thenReturn(USER_API_SCHEME);
        when(environment.readEnv(USER_API_HOST)).thenReturn(USER_API_HOST);
        when(environment.readEnv(USER_SERVICE_SECRET_NAME)).thenReturn(USER_SERVICE_SECRET_NAME);
        when(environment.readEnv(USER_SERVICE_SECRET_KEY)).thenReturn(USER_SERVICE_SECRET_KEY);
        httpClient = mock(HttpClient.class);
        httpResponse = mock(HttpResponse.class);

        userApiClient = new UserApiClient(httpClient, new ObjectMapper(), secretsReader, environment);
    }

    @Test
    public void getUserReturnsUserOnValidUsername() throws Exception {
        when(httpResponse.body()).thenReturn(getValidJsonUser());
        when(httpResponse.statusCode()).thenReturn(SC_OK);
        when(httpClient.send(any(), any())).thenReturn(httpResponse);

        Optional<User> user = userApiClient.getUser(SAMPLE_USERNAME);

        Assertions.assertTrue(user.isPresent());
    }

    @Test
    public void getUserReturnsEmptyOptionalOnInvalidJsonResponse() throws IOException, InterruptedException {
        final TestAppender appender = LogUtils.getTestingAppender(UserApiClient.class);
        when(httpResponse.body()).thenReturn(GARBAGE_JSON);
        when(httpResponse.statusCode()).thenReturn(SC_OK);
        when(httpClient.send(any(), any())).thenReturn(httpResponse);

        Optional<User> user = userApiClient.getUser(SAMPLE_USERNAME);


        String messages = appender.getMessages();
        assertThat(messages, containsString(ERROR_PARSING_USER_INFORMATION));
        assertTrue(user.isEmpty());
    }

    @Test
    public void getUserReturnsEmptyOptionalOnInvalidHttpResponse() throws IOException, InterruptedException {
        final TestAppender appender = LogUtils.getTestingAppender(UserApiClient.class);
        when(httpResponse.statusCode()).thenReturn(SC_INTERNAL_SERVER_ERROR);
        when(httpClient.send(any(), any())).thenReturn(httpResponse);

        Optional<User> user = userApiClient.getUser(SAMPLE_USERNAME);

        String messages = appender.getMessages();
        assertThat(messages, containsString(ERROR_PARSING_USER_INFORMATION));
        assertTrue(user.isEmpty());
    }

    @Test
    public void getUserReturnsEmptyOptionalOnHttpError() throws IOException, InterruptedException {
        final TestAppender appender = LogUtils.getTestingAppender(UserApiClient.class);
        when(httpClient.send(any(), any())).thenThrow(IOException.class);

        Optional<User> user = userApiClient.getUser(SAMPLE_USERNAME);

        String messages = appender.getMessages();
        assertThat(messages, containsString(ERROR_PARSING_USER_INFORMATION));
        assertTrue(user.isEmpty());
    }

    @Test
    public void createUserReturnsCreatedUserOnSuccess() throws IOException, InterruptedException, ForbiddenException {
        when(httpResponse.body()).thenReturn(getValidJsonUser());
        when(httpResponse.statusCode()).thenReturn(SC_OK);
        when(httpClient.send(any(), any())).thenReturn(httpResponse);
        prepareMocksWithSecret();

        User requestUser = createUser();

        User responseUser = userApiClient.createUser(requestUser);

        Assertions.assertNotNull(responseUser);
    }

    private void prepareMocksWithSecret() throws ForbiddenException {
        when(secretsReader.fetchSecret(anyString(), anyString())).thenReturn(THE_API_KEY);
    }

    @Test
    public void createUserReturnsErrorOnFailure() throws IOException, InterruptedException {
        final TestAppender appender = LogUtils.getTestingAppender(UserApiClient.class);
        when(httpClient.send(any(), any())).thenThrow(IOException.class);

        User requestUser = createUser();

        Exception exception = assertThrows(CreateUserFailedException.class,
            () -> userApiClient.createUser(requestUser));

        assertEquals(UserApiClient.CREATE_USER_ERROR_MESSAGE, exception.getMessage());
        String messages = appender.getMessages();
        assertThat(messages, containsString(ERROR_FETCHING_USER_INFORMATION));
    }

    public String getValidJsonUser() throws JsonProcessingException {
        return objectMapper.writeValueAsString(createUser());
    }

    private User createUser() {
        return new User(
            SAMPLE_USERNAME,
            SAMPLE_INSTITUTION_ID, Collections.singletonList(new Role(CREATOR))
        );
    }
}
