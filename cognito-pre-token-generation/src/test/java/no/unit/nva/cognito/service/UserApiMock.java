package no.unit.nva.cognito.service;

import static no.unit.nva.cognito.service.UserApiClient.CREATE_USER_ERROR_MESSAGE;
import java.util.Optional;
import no.unit.nva.cognito.exception.CreateUserFailedException;
import no.unit.nva.useraccessmanagement.model.UserDto;

public class UserApiMock implements UserApi {

    private UserDto user;

    @Override
    public Optional<UserDto> getUser(String username) {
        return Optional.ofNullable(user);
    }

    @Override
    public UserDto createUser(UserDto user) {
        this.user = user;
        if (user != null) {
            return user;
        } else {
            throw new CreateUserFailedException(CREATE_USER_ERROR_MESSAGE);
        }
    }
}
