package no.unit.nva.cognito.service;

import static no.unit.nva.cognito.service.UserApiClient.CREATE_USER_ERROR_MESSAGE;

import java.util.Optional;
import no.unit.nva.cognito.exception.CreateUserFailedException;
import no.unit.nva.cognito.api.user.model.UserDto;

public class UserApiMock implements UserApi {
    public static final boolean IS_CONFLICT = true;
    private UserDto userDto;

    @Override
    public Optional<UserDto> getUser(String username) {
        return Optional.ofNullable(userDto);
    }

    @Override
    public UserDto createUser(UserDto userDto) {
        this.userDto = userDto;
        if (userDto != null) {
            return userDto;
        } else {
            throw new CreateUserFailedException(CREATE_USER_ERROR_MESSAGE, IS_CONFLICT);
        }
    }

    @Override
    public UserDto updateUser(UserDto userDto) {
        this.userDto = userDto;
        return userDto;
    }
}
