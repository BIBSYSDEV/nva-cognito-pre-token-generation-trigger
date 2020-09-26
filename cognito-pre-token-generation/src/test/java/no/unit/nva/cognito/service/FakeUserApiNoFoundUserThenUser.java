package no.unit.nva.cognito.service;

import static no.unit.nva.cognito.service.UserApiClient.CREATE_USER_ERROR_MESSAGE;

import java.util.Optional;
import no.unit.nva.cognito.exception.CreateUserFailedException;
import no.unit.nva.cognito.api.user.model.UserDto;

/**
 * Please someone refactor this crap. Fake is there for demostrating the following weird shit:
 * * Post trigger on new Cognito user
 * * It tries to gets users, get not found.
 * * goes to elseGet logic and upserts user
 * * tries to create first, gets conflict because now dynamodb is eventually consistent...
 * * check if conflicts, yai , lets actually allow the upsert..
 * * go upsert..
 *
 */
public class FakeUserApiNoFoundUserThenUser implements UserApi {
    public static final boolean IS_CONFLICT = true;
    private UserDto userDto;
    private boolean firstCallGetUser = true;
    private boolean firstCallCreateUser = true;


    @Override
    public Optional<UserDto> getUser(String username) {
        if (firstCallGetUser) {
            firstCallGetUser = false;
            return Optional.empty();
        }
        return Optional.ofNullable(userDto);
    }

    @Override
    public UserDto createUser(UserDto userDto) {
        if (firstCallCreateUser) {
            firstCallCreateUser = false;
        } else {
            this.userDto = userDto;
        }

        if (this.userDto != null) {
            return this.userDto;
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
