package no.unit.nva.cognito.service;

import static no.unit.nva.cognito.service.UserApiClient.COULD_NOT_CREATE_USER_ERROR_MESSAGE;
import static nva.commons.core.attempt.Try.attempt;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import no.unit.nva.cognito.exception.BadGatewayException;
import no.unit.nva.useraccessmanagement.model.RoleDto;
import no.unit.nva.useraccessmanagement.model.UserDto;

public class UserApiMock implements UserApi {

    public static final String FIRST_ACCESS_RIGHT = "APPROVE_DOI_REQUEST";
    public static final String SECOND_ACCESS_RIGHT = "REJECT_DOI_REQUEST";
    public static final Set<String> SAMPLE_ACCESS_RIGHTS = Set.of(FIRST_ACCESS_RIGHT, SECOND_ACCESS_RIGHT);
    private UserDto user;

    @Override
    public Optional<UserDto> getUser(String username) {
        return Optional.ofNullable(user);
    }

    @Override
    public UserDto createUser(UserDto user) {
        UserDto updatedUser = updateRolesWithAccessRights(user);
        this.user = updatedUser;

        if (user != null) {
            return user;
        } else {
            throw new BadGatewayException(COULD_NOT_CREATE_USER_ERROR_MESSAGE);
        }
    }

    private UserDto updateRolesWithAccessRights(UserDto user) {
        List<RoleDto> updatedRoles = updateRoles(user.getRoles());
        return attempt(() -> user.copy().withRoles(updatedRoles).build()).toOptional().orElseThrow();
    }

    private List<RoleDto> updateRoles(List<RoleDto> roles) {
        return roles.stream().map(this::addAccessRights).collect(Collectors.toList());
    }

    private RoleDto addAccessRights(RoleDto role) {
        return attempt(() -> role.copy().withAccessRights(SAMPLE_ACCESS_RIGHTS).build())
            .toOptional().orElseThrow();
    }
}
