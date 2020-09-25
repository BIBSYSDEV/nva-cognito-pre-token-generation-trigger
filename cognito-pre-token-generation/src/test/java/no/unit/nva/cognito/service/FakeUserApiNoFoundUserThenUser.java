package no.unit.nva.cognito.service;

import static no.unit.nva.cognito.service.UserApiClient.CREATE_USER_ERROR_MESSAGE;

import java.util.Optional;
import no.unit.nva.cognito.exception.CreateUserFailedException;
import no.unit.nva.cognito.model.User;

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
    private User user;
    private boolean firstCallGetUser = true;
    private boolean firstCallCreateUser = true;


    @Override
    public Optional<User> getUser(String username) {
        if (firstCallGetUser) {
            firstCallGetUser = false;
            return Optional.empty();
        }
        return Optional.ofNullable(user);
    }

    @Override
    public User createUser(User user) {
        if (firstCallCreateUser) {
            firstCallCreateUser = false;
        } else {
            this.user = user;
        }

        if (this.user != null) {
            return this.user;
        } else {
            throw new CreateUserFailedException(CREATE_USER_ERROR_MESSAGE, IS_CONFLICT);
        }
    }

    @Override
    public User updateUser(User user) {
        this.user = user;
        return user;
    }
}
