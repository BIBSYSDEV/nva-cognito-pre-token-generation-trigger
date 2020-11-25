package no.unit.nva.cognito.service;

import static java.util.Collections.singletonList;
import static no.unit.nva.cognito.service.UserApiClient.COULD_NOT_CREATE_USER_ERROR_MESSAGE;
import static no.unit.nva.cognito.service.UserApiClient.COULD_NOT_FETCH_USER_ERROR_MESSAGE;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.net.http.HttpClient;
import java.net.http.HttpResponse;
import java.util.Optional;
import no.unit.nva.cognito.exception.BadGatewayException;
import no.unit.nva.useraccessmanagement.exceptions.InvalidEntryInternalException;
import no.unit.nva.useraccessmanagement.model.RoleDto;
import no.unit.nva.useraccessmanagement.model.UserDto;
import nva.commons.exceptions.ForbiddenException;
import nva.commons.utils.Environment;
import nva.commons.utils.aws.SecretsReader;
import nva.commons.utils.log.LogUtils;
import nva.commons.utils.log.TestAppender;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

@SuppressWarnings("unchecked")
public class UserApiClientTest {

    public static final String GARBAGE_JSON = "{{}";
    public static final String SAMPLE_USERNAME = "user@name";
    public static final String SAMPLE_INSTITUTION_ID = "institution.id";
    public static final String CREATOR = "Creator";
    public static final String THE_API_KEY = "TheApiKey";
    public static final String SAMPLE_API_SCHEME = "http";
    public static final String SAMPLE_API_HOST = "example.org";
    public static final String SAMPLE_FAMILY_NAME = "familyName";
    public static final String SAMPLE_GIVEN_NAME = "givenName";

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
        when(environment.readEnv(USER_API_SCHEME)).thenReturn(SAMPLE_API_SCHEME);
        when(environment.readEnv(USER_API_HOST)).thenReturn(SAMPLE_API_HOST);
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

        Optional<UserDto> user = userApiClient.getUser(SAMPLE_USERNAME);

        Assertions.assertTrue(user.isPresent());
    }

    @Test
    public void getUserReturnsEmptyOptionalOnInvalidJsonResponse()
        throws IOException, InterruptedException, BadGatewayException {
        final TestAppender appender = LogUtils.getTestingAppender(UserApiClient.class);
        when(httpResponse.body()).thenReturn(GARBAGE_JSON);
        when(httpResponse.statusCode()).thenReturn(SC_OK);
        when(httpClient.send(any(), any())).thenReturn(httpResponse);

        Executable action = () -> userApiClient.getUser(SAMPLE_USERNAME);

        assertThrows(IllegalStateException.class, action);
        String messages = appender.getMessages();
        assertThat(messages, containsString(ERROR_PARSING_USER_INFORMATION));
    }

    @Test
    public void getUserReturnsEmptyOptionalOnInvalidHttpResponse()
        throws IOException, InterruptedException, BadGatewayException {
        final TestAppender logAppendeer = LogUtils.getTestingAppender(UserApiClient.class);
        when(httpResponse.statusCode()).thenReturn(SC_INTERNAL_SERVER_ERROR);
        when(httpClient.send(any(), any())).thenReturn(httpResponse);

        Executable action = () -> userApiClient.getUser(SAMPLE_USERNAME);

        assertThrows(BadGatewayException.class, action);
        assertThat(logAppendeer.getMessages(), containsString(COULD_NOT_FETCH_USER_ERROR_MESSAGE));
    }

    @Test
    public void getUserReturnsEmptyOptionalOnHttpError() throws IOException, InterruptedException, BadGatewayException {
        final TestAppender appender = LogUtils.getTestingAppender(UserApiClient.class);
        when(httpClient.send(any(), any())).thenThrow(IOException.class);

        Executable action = () -> userApiClient.getUser(SAMPLE_USERNAME);
        assertThrows(BadGatewayException.class, action);
        String messages = appender.getMessages();
        assertThat(messages, containsString(COULD_NOT_FETCH_USER_ERROR_MESSAGE));
    }

    @Test
    public void createUserReturnsCreatedUserOnSuccess()
        throws IOException, InterruptedException, ForbiddenException, InvalidEntryInternalException,
               BadGatewayException {
        when(httpResponse.body()).thenReturn(getValidJsonUser());
        when(httpResponse.statusCode()).thenReturn(SC_OK);
        when(httpClient.send(any(), any())).thenReturn(httpResponse);
        prepareMocksWithSecret();

        UserDto requestUser = createUser();

        UserDto responseUser = userApiClient.createUser(requestUser);

        Assertions.assertNotNull(responseUser);
    }

    @Test
    public void createUserReturnsErrorOnFailure()
        throws IOException, InterruptedException, InvalidEntryInternalException {
        final TestAppender appender = LogUtils.getTestingAppender(UserApiClient.class);
        when(httpClient.send(any(), any())).thenThrow(IOException.class);

        UserDto requestUser = createUser();

        Exception exception = assertThrows(BadGatewayException.class,
            () -> userApiClient.createUser(requestUser));

        assertEquals(COULD_NOT_CREATE_USER_ERROR_MESSAGE, exception.getMessage());
        String messages = appender.getMessages();
        assertThat(messages, containsString(COULD_NOT_CREATE_USER_ERROR_MESSAGE));
    }

    public String getValidJsonUser() throws JsonProcessingException, InvalidEntryInternalException {
        return objectMapper.writeValueAsString(createUser());
    }

    private void prepareMocksWithSecret() throws ForbiddenException {
        when(secretsReader.fetchSecret(anyString(), anyString())).thenReturn(THE_API_KEY);
    }

    private UserDto createUser() throws InvalidEntryInternalException {
        return UserDto.newBuilder()
            .withRoles(singletonList(RoleDto.newBuilder().withName(CREATOR).build()))
            .withUsername(SAMPLE_USERNAME)
            .withInstitution(SAMPLE_INSTITUTION_ID)
            .withGivenName(SAMPLE_GIVEN_NAME)
            .withFamilyName(SAMPLE_FAMILY_NAME)
            .build();
    }
}
