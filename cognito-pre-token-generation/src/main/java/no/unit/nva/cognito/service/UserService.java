package no.unit.nva.cognito.service;

import com.amazonaws.services.cognitoidp.AWSCognitoIdentityProvider;
import com.amazonaws.services.cognitoidp.model.AdminUpdateUserAttributesRequest;
import com.amazonaws.services.cognitoidp.model.AttributeType;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import no.unit.nva.cognito.model.Role;
import no.unit.nva.cognito.model.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class UserService {

    public static final String NO_ROLE = null;
    private final UserApi userApi;
    private final AWSCognitoIdentityProvider awsCognitoIdentityProvider;

    public static final String USER = "User";
    public static final String CREATOR = "Creator";
    public static final String FACULTY = "faculty";
    public static final String STAFF = "staff";

    private static final Logger logger = LoggerFactory.getLogger(UserService.class);

    public UserService(UserApi userApi,
                       AWSCognitoIdentityProvider awsCognitoIdentityProvider) {
        this.userApi = userApi;
        this.awsCognitoIdentityProvider = awsCognitoIdentityProvider;
    }

    /**
     * Get user from user catalogue service or create new user if not found.
     *
     * @param feideId       feideId as username
     * @param givenName     givenName
     * @param familyName    familyName
     * @param customerId    customerId as institution
     * @param affiliation   affiliation
     * @return  the user
     */
    public User getOrCreateUser(String feideId,
                                String givenName,
                                String familyName,
                                Optional<String> customerId,
                                String affiliation) {
        return userApi
            .getUser(feideId)
            .orElseGet(() -> createUser(feideId, givenName, familyName, customerId, affiliation));
    }

    /**
     * Add attributes to user.
     *
     * @param userPoolId    userPoolId
     * @param userName      userName
     * @param attributes    attributes
     */
    public void updateUserAttributes(String userPoolId, String userName, List<AttributeType> attributes) {
        AdminUpdateUserAttributesRequest request = new AdminUpdateUserAttributesRequest()
            .withUserPoolId(userPoolId)
            .withUsername(userName)
            .withUserAttributes(attributes);
        logger.info("Updating User Attributes: " + request.toString());
        awsCognitoIdentityProvider.adminUpdateUserAttributes(request);
    }

    private User createUser(String username,
                            String givenName,
                            String familyName,
                            Optional<String> customerId,
                            String affiliation) {
        User user;
        if (customerId.isPresent()) {
            user = createUserForInstitution(username, givenName, familyName, customerId.get(), affiliation);
        } else {
            user = createUserWithoutInstitution(username, givenName, familyName);
        }
        userApi.createUser(user);
        return user;
    }

    private User createUserWithoutInstitution(String username, String givenName, String familyName) {
        return new User(username, givenName, familyName, NO_ROLE, Collections.singletonList(new Role(USER)));
    }

    private User createUserForInstitution(String username,
                                          String givenName,
                                          String familyName,
                                          String institutionId,
                                          String affiliation) {
        List<Role> roles = createRolesFromAffiliation(affiliation);
        roles.add(new Role(USER));
        return new User(username, givenName, familyName, institutionId, roles);
    }

    /**
     * Create user roles from users give affiliation at organization.
     * @see <a href="https://www.feide.no/attribute/edupersonaffiliation">Feide eduPersonAffiliation</a>
     *
     * @param affiliation   affiliation
     * @return  list of roles
     */
    private List<Role> createRolesFromAffiliation(final String affiliation) {
        String lowerCaseAffiliation = affiliation.toLowerCase(Locale.getDefault());
        List<Role> roles = new ArrayList<>();

        if (lowerCaseAffiliation.contains(STAFF) || lowerCaseAffiliation.contains(FACULTY)) {
            roles.add(new Role(CREATOR));
        }

        return roles;
    }

}
