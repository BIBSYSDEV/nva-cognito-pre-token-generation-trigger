package no.unit.nva.cognito.service;

import com.amazonaws.services.cognitoidp.AWSCognitoIdentityProvider;
import com.amazonaws.services.cognitoidp.model.AdminUpdateUserAttributesRequest;
import com.amazonaws.services.cognitoidp.model.AttributeType;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import no.unit.nva.useraccessmanagement.exceptions.InvalidEntryInternalException;
import no.unit.nva.useraccessmanagement.model.RoleDto;
import no.unit.nva.useraccessmanagement.model.UserDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class UserService {

    public static final List<RoleDto> NO_ROLE = Collections.emptyList();
    public static final String USER = "User";
    public static final String CREATOR = "Creator";
    public static final String FACULTY = "faculty";
    public static final String STAFF = "staff";
    private static final Logger logger = LoggerFactory.getLogger(UserService.class);
    private final UserApi userApi;
    private final AWSCognitoIdentityProvider awsCognitoIdentityProvider;

    public UserService(UserApi userApi,
                       AWSCognitoIdentityProvider awsCognitoIdentityProvider) {
        this.userApi = userApi;
        this.awsCognitoIdentityProvider = awsCognitoIdentityProvider;
    }

    /**
     * Get user from user catalogue service or create new user if not found.
     *
     * @param feideId     feideId as username
     * @param givenName   givenName
     * @param familyName  familyName
     * @param customerId  customerId as institution
     * @param affiliation affiliation
     * @return the user
     */
    public UserDto getOrCreateUser(String feideId,
                                   String givenName,
                                   String familyName,
                                   Optional<String> customerId,
                                   String affiliation) {
        Optional<UserDto> existingUser = userApi.getUser(feideId);
        return existingUser.orElseGet(() -> createUser(feideId, givenName, familyName, customerId, affiliation));
    }

    /**
     * Add attributes to user.
     *
     * @param userPoolId userPoolId
     * @param userName   userName
     * @param attributes attributes
     */
    public void updateUserAttributes(String userPoolId, String userName, List<AttributeType> attributes) {
        AdminUpdateUserAttributesRequest request = new AdminUpdateUserAttributesRequest()
            .withUserPoolId(userPoolId)
            .withUsername(userName)
            .withUserAttributes(attributes);
        logger.info("Updating User Attributes: " + request.toString());
        awsCognitoIdentityProvider.adminUpdateUserAttributes(request);
    }

    private UserDto createUser(String username,
                               String givenName,
                               String familyName,
                               Optional<String> customerId,
                               String affiliation) {
        UserDto user;
        try {

            if (customerId.isPresent()) {
                user = createUserForInstitution(username, givenName, familyName, customerId.get(), affiliation);
            } else {
                user = createUserWithoutInstitution(username, givenName, familyName);
            }
        } catch (InvalidEntryInternalException e) {
            throw new RuntimeException(e);
        }
        user = userApi.createUser(user);
        return user;
    }

    private UserDto createUserWithoutInstitution(String username, String givenName, String familyName)
        throws InvalidEntryInternalException {
        return UserDto.newBuilder().withUsername(username)
            .withGivenName(givenName)
            .withFamilyName(familyName)
            .withRoles(Collections.singletonList(RoleDto.newBuilder().withName(USER).build()))
            .build();
    }

    private UserDto createUserForInstitution(String username,
                                             String givenName,
                                             String familyName,
                                             String institutionId,
                                             String affiliation) throws InvalidEntryInternalException {
        List<RoleDto> roles = createRolesFromAffiliation(affiliation);
        roles.add(RoleDto.newBuilder().withName(USER).build());
        return UserDto
            .newBuilder()
            .withUsername(username)
            .withGivenName(givenName)
            .withFamilyName(familyName)
            .withInstitution(institutionId)
            .withRoles(roles)
            .build();
    }

    /**
     * Create user roles from users give affiliation at organization.
     *
     * @param affiliation affiliation
     * @return list of roles
     * @see <a href="https://www.feide.no/attribute/edupersonaffiliation">Feide eduPersonAffiliation</a>
     */
    private List<RoleDto> createRolesFromAffiliation(final String affiliation) throws InvalidEntryInternalException {
        String lowerCaseAffiliation = affiliation.toLowerCase(Locale.getDefault());
        List<RoleDto> roles = new ArrayList<>();

        if (lowerCaseAffiliation.contains(STAFF) || lowerCaseAffiliation.contains(FACULTY)) {
            roles.add(RoleDto.newBuilder().withName(CREATOR).build());
        }

        return roles;
    }
}
