package no.unit.nva.cognito.service;

import static no.unit.nva.cognito.util.OrgNumberCleaner.removeCountryPrefix;

import com.amazonaws.services.cognitoidp.AWSCognitoIdentityProvider;
import com.amazonaws.services.cognitoidp.model.AdminUpdateUserAttributesRequest;
import com.amazonaws.services.cognitoidp.model.AttributeType;
import java.util.Objects;
import no.unit.nva.cognito.api.lambda.event.CustomerResponse;
import no.unit.nva.cognito.model.Role;
import no.unit.nva.cognito.api.user.model.UserDto;
import no.unit.nva.cognito.model.User;
import no.unit.nva.cognito.api.lambda.event.UserAttributes;
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

    // https://www.feide.no/attribute/edupersonaffiliation Documentation about Feide affiliations.
    public static final String FEIDE_AFFILIATION_EMPLOYEE = "employee";
    public static final String FEIDE_AFFILIATION_MEMBER = "member";
    public static final String FEIDE_AFFILIATION_STAFF = "staff";
    public static final String FEIDE_AFFILIATION_FACULTY = "faculty";

    private static final Logger logger = LoggerFactory.getLogger(UserService.class);
    private final CustomerApiClient customerApi;

    public UserService(UserApi userApiService,
                       AWSCognitoIdentityProvider awsCognitoIdentityProvider,
                       CustomerApiClient customerApiClient) {
        this.userApiService = userApiService;
        this.awsCognitoIdentityProvider = awsCognitoIdentityProvider;
        this.customerApi = customerApiClient;
    }

    /**
     * Creates user from token
     * @param userPoolId        userPoolId in Cognito
     * @param cognitoUserName   cognito's username
     * @param originalUserAttributes cognito's user attributes (coming from event which source is cognito's user pool)
     * @return User business object
     */
    public User createUserFromToken(String userPoolId,
                                    String cognitoUserName,
                                    UserAttributes originalUserAttributes) {

        UserAttributes maybeUpdatedProperties = originalUserAttributes.getDeepCopy();
        var newUser = false;
        if (!maybeUpdatedProperties.hasCustomerAttributes()) {
            newUser = true;
            var maybeCustomer = customerApi.getCustomer(
                removeCountryPrefix(maybeUpdatedProperties.getOrgNumber()));

            var maybeCustomerId = maybeCustomer.map(CustomerResponse::getCustomerId);
            var maybeCristinId = maybeCustomer.map(CustomerResponse::getCristinId);

            maybeCustomerId.ifPresent(maybeUpdatedProperties::setCustomerId);
            maybeCristinId.ifPresent(maybeUpdatedProperties::setCristinId);
        }


        var apiUser = createUser(maybeUpdatedProperties.getFeideId(),
            maybeUpdatedProperties.getGivenName(),
            maybeUpdatedProperties.getFamilyName(),
            maybeUpdatedProperties.getCustomerId(),
            maybeUpdatedProperties.getCristinId(),
            maybeUpdatedProperties.getAffiliation());


        return new User(userPoolId,
            cognitoUserName,
            apiUser,
            maybeUpdatedProperties,
            newUser,
            this);
    }

    /**
     * Deprecated and should be refactored into UserManagement to react on events.
     * @param userDto
     */
    @Deprecated
    public void temporaryFireAndForgetCreateUser(UserDto userDto) {
        userApiService.createUser(userDto);
    }

    /**
     * Deprecated and should be refactored into UserManagement to react on events.
     * @param userDto
     */
    @Deprecated
    public void temporaryFireAndForgetUpdateUser(UserDto userDto) {
        userApiService.updateUser(userDto);
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
        return userDto;
    }

    private boolean hasCustomerAttributes(String customerId, String cristinId) {
        return Objects.nonNull(customerId) && Objects.nonNull(cristinId);
    }

    private UserDto createUserWithoutInstitution(String username, String givenName, String familyName) {
        return new UserDto(username,
            givenName,
            familyName,
            NO_CUSTOMER_WITH_NVA,
            NO_CRISTIN_ID,
            Collections.singletonList(new Role(USER)));
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
        // https://unit.atlassian.net/browse/NP-1491
        if (isScientificOrTechnicalAdministrativeEmployee(affiliation)) {
            roles.add(new Role(CREATOR));
        }

        return roles;
    }

    /**
     * Check if affiliation attribute tells us this is a scientific employee or technical administrative employee.
     * @see <a href="https://unit.atlassian.net/browse/NP-1491">https://unit.atlassian.net/browse/NP-1491</a>
     * @see
     * <a href="https://www.feide.no/attribute/edupersonaffiliation">https://www.feide.no/attribute/edupersonaffiliation</a>
     * @param feideAffiliation
     * @return <code>true</code> if scientific employee or technical administrative employee.
     */
    private boolean isScientificOrTechnicalAdministrativeEmployee(String feideAffiliation) {
        return feideAffiliation.contains(FEIDE_AFFILIATION_MEMBER)
            && feideAffiliation.contains(FEIDE_AFFILIATION_EMPLOYEE) && (feideAffiliation.contains(FEIDE_AFFILIATION_FACULTY)
            || feideAffiliation.contains(
            FEIDE_AFFILIATION_STAFF));
    }
}
