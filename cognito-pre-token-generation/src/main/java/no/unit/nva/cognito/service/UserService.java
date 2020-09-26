package no.unit.nva.cognito.service;

import com.amazonaws.services.cognitoidp.AWSCognitoIdentityProvider;
import com.amazonaws.services.cognitoidp.model.AdminUpdateUserAttributesRequest;
import com.amazonaws.services.cognitoidp.model.AttributeType;
import java.util.Objects;
import no.unit.nva.cognito.model.Role;
import no.unit.nva.cognito.api.user.model.UserDto;
import no.unit.nva.cognito.model.User;
import no.unit.nva.cognito.model.UserAttributes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class UserService {

    public static final String NO_CUSTOMER_WITH_NVA = null;
    private static final String NO_CRISTIN_ID = null;
    private final UserApi userApiService;
    private final AWSCognitoIdentityProvider awsCognitoIdentityProvider;

    public static final String USER = "User";
    public static final String CREATOR = "Creator";
    public static final String EMPLOYEE = "employee";
    public static final String MEMBER = "member";
    public static final String STAFF = "staff";

    private static final Logger logger = LoggerFactory.getLogger(UserService.class);

    public UserService(UserApi userApiService,
                       AWSCognitoIdentityProvider awsCognitoIdentityProvider) {
        this.userApiService = userApiService;
        this.awsCognitoIdentityProvider = awsCognitoIdentityProvider;
    }

    /**
     * Retreive user from user cataloge or creates it from the token's user attributes.
     * @param userPoolId        userPoolId in Cognito
     * @param cognitoUserName   cognito's username
     * @param userAttributes    updated cognito's user attributes with our custom: attributes.
     * @return User business object
     */
    public User getOrCreateUserFromToken(String userPoolId,
                                         String cognitoUserName,
                                         UserAttributes userAttributes) {

        return new User(userPoolId, cognitoUserName, userApiService
            // Can customerId from orgnumber lookup be authorative for saying this user
            // is a customer? And we can always update user object from token?
            // Wanted rule: Always up2date token for clients, eventually updated dynamodb.
            .getUser(userAttributes.getFeideId())
            .orElseGet(() -> createUser(userAttributes.getFeideId(),
                userAttributes.getGivenName(),
                userAttributes.getFamilyName(),
                userAttributes.getCustomerId(),
                userAttributes.getCristinId(),
                userAttributes.getAffiliation())),
            userAttributes,
            this);
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

    private UserDto createUser(String username,
                               String givenName,
                               String familyName,
                               String customerId,
                               String cristinId,
                               String affiliation) {
        UserDto userDto;
        if (hasCustomerAttributes(customerId, cristinId)) {
            userDto = createUserForInstitution(username, givenName, familyName, customerId, cristinId, affiliation);
        } else {
            userDto = createUserWithoutInstitution(username, givenName, familyName);
        }
        // Send async event, so eventually the user gets stored in dynamodb.
        /*try {
            userApi.createUser(user);
        } catch (CreateUserFailedException e) {
            // Allow conflicts
            if (!e.isConflictWithExistingUser()) {
                throw e;
            }
            userApi.updateUser(user);
        }*/
        return userDto;
    }

    private boolean hasCustomerAttributes(String customerId, String cristinId) {
        return Objects.nonNull(customerId) && Objects.nonNull(cristinId);
    }

    private UserDto createUserWithoutInstitution(String username, String givenName, String familyName) {
        return new UserDto(username, givenName, familyName, NO_CUSTOMER_WITH_NVA, NO_CRISTIN_ID, Collections.singletonList(new Role(USER)));
    }

    private UserDto createUserForInstitution(String username,
                                             String givenName,
                                             String familyName,
                                             String institutionId,
                                             String cristinId,
                                             String affiliation) {
        List<Role> roles = createRolesFromAffiliation(affiliation);
        roles.add(new Role(USER));
        return new UserDto(username, givenName, familyName, institutionId, cristinId, roles);
    }

    private List<Role> createRolesFromAffiliation(String affiliation) {
        List<Role> roles = new ArrayList<>();
        // TODO: Verify conditions from https://unit.atlassian.net/browse/NP-1491
        // TODO: Verify conditions from ?? (dont remember second user story)
        if (affiliation.contains(STAFF)
            || affiliation.contains(EMPLOYEE)
            || affiliation.contains(MEMBER)
        ) {
            roles.add(new Role(CREATOR));
        }

        return roles;
    }
}
