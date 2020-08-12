package no.unit.nva.cognito.service;

import static org.apache.http.HttpStatus.SC_INTERNAL_SERVER_ERROR;
import static org.apache.http.HttpStatus.SC_OK;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.net.http.HttpClient;
import java.net.http.HttpResponse;
import java.util.Collections;
import java.util.Optional;
import no.unit.nva.cognito.model.Role;
import no.unit.nva.cognito.model.User;
import nva.commons.utils.Environment;
import nva.commons.utils.log.LogUtils;
import nva.commons.utils.log.TestAppender;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

@SuppressWarnings("unchecked")
public class UserApiClientTest {

    public static final String SAMPLE_SCHEME = "http";
    public static final String SAMPLE_HOST = "example.org";
    public static final String GARBAGE_JSON = "{{}";
    public static final String SAMPLE_USERNAME = "username";
    public static final String SAMPLE_INSTITUTION_ID = "institution.id";
    public static final String CREATOR = "Creator";

    private ObjectMapper objectMapper;
    private UserApiClient userApiClient;
    private HttpClient httpClient;
    private HttpResponse httpResponse;

    /**
     * Set up test environment.
     */
    @BeforeEach
    public void init() {
        objectMapper = new ObjectMapper();
        Environment environment = mock(Environment.class);
        when(environment.readEnv(UserApiClient.USER_API_SCHEME)).thenReturn(SAMPLE_SCHEME);
        when(environment.readEnv(UserApiClient.USER_API_HOST)).thenReturn(SAMPLE_HOST);
        httpClient = mock(HttpClient.class);
        httpResponse = mock(HttpResponse.class);

        userApiClient = new UserApiClient(httpClient, new ObjectMapper(), environment);
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
        assertThat(messages, containsString(UserApiClient.ERROR_PARSING_USER_INFORMATION));
        assertTrue(user.isEmpty());
    }

    @Test
    public void getUserReturnsEmptyOptionalOnInvalidHttpResponse() throws IOException, InterruptedException {
        final TestAppender appender = LogUtils.getTestingAppender(UserApiClient.class);
        when(httpResponse.statusCode()).thenReturn(SC_INTERNAL_SERVER_ERROR);
        when(httpClient.send(any(), any())).thenReturn(httpResponse);

        Optional<User> user = userApiClient.getUser(SAMPLE_USERNAME);

        String messages = appender.getMessages();
        assertThat(messages, containsString(UserApiClient.ERROR_PARSING_USER_INFORMATION));
        assertTrue(user.isEmpty());
    }

    @Test
    public void getUserReturnsEmptyOptionalOnHttpError() throws IOException, InterruptedException {
        final TestAppender appender = LogUtils.getTestingAppender(UserApiClient.class);
        when(httpClient.send(any(), any())).thenThrow(IOException.class);

        Optional<User> user = userApiClient.getUser(SAMPLE_USERNAME);

        String messages = appender.getMessages();
        assertThat(messages, containsString(UserApiClient.ERROR_PARSING_USER_INFORMATION));
        assertTrue(user.isEmpty());
    }

    @Test
    public void createUserReturnsCreatedUserOnSuccess() throws IOException, InterruptedException {
        when(httpResponse.body()).thenReturn(getValidJsonUser());
        when(httpResponse.statusCode()).thenReturn(SC_OK);
        when(httpClient.send(any(), any())).thenReturn(httpResponse);

        User requestUser = createUser();

        Optional<User> responseUser = userApiClient.createUser(requestUser);

        Assertions.assertTrue(responseUser.isPresent());
    }

    @Test
    public void createUserReturnsErrorOnFailure() throws IOException, InterruptedException {
        final TestAppender appender = LogUtils.getTestingAppender(UserApiClient.class);
        when(httpClient.send(any(), any())).thenThrow(IOException.class);

        User requestUser = createUser();

        Optional<User> responseUser = userApiClient.createUser(requestUser);

        assertTrue(responseUser.isEmpty());

        String messages = appender.getMessages();
        assertThat(messages, containsString(UserApiClient.ERROR_PARSING_USER_INFORMATION));
        assertTrue(responseUser.isEmpty());
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
