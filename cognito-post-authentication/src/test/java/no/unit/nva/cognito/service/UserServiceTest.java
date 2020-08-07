package no.unit.nva.cognito.service;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalToIgnoringCase;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.amazonaws.services.cognitoidp.AWSCognitoIdentityProvider;
import java.util.Optional;
import no.unit.nva.cognito.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

public class UserServiceTest {

    public static final String PUBLISHER = "Publisher";
    private UserApi userApi;
    private AWSCognitoIdentityProvider cognitoIdentityProvider;
    private UserService userService;

    public static final String SAMPLE_AFFILIATION = "[member, employee, staff]";
    public static final String SAMPLE_NO_STAFF_AFFILIATION = "[member, employee]";
    public static final String SAMPLE_FEIDE_ID = "feideId";
    public static final String SAMPLE_CUSTOMER_ID = "http://example.org/customer/123";

    /**
     * Set up environment.
     */
    @BeforeEach
    public void init() {
        userApi = mock(UserApi.class);
        cognitoIdentityProvider = mock(AWSCognitoIdentityProvider.class);
        userService = new UserService(userApi, cognitoIdentityProvider);
    }

    @Test
    public void createUserAddsPublisherRoleWhenUserIsStaff() {
        when(userApi.createUser(Mockito.any())).thenReturn(Optional.of(mock(User.class)));
        User user = userService.getOrCreateUser(SAMPLE_FEIDE_ID, SAMPLE_CUSTOMER_ID, SAMPLE_AFFILIATION);
        assertThat(user.getRoles().get(0).getRolename(), equalToIgnoringCase(PUBLISHER));
    }

    @Test
    public void createUserNoRoleRoleWhenUserIsNonStaff() {
        when(userApi.createUser(Mockito.any())).thenReturn(Optional.of(mock(User.class)));
        User user = userService.getOrCreateUser(SAMPLE_FEIDE_ID, SAMPLE_CUSTOMER_ID, SAMPLE_NO_STAFF_AFFILIATION);
        assertThat(user.getRoles(), is(empty()));
    }

}
