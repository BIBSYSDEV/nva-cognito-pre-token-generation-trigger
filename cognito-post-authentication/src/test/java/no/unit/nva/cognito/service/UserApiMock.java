package no.unit.nva.cognito.service;

import static no.unit.nva.cognito.service.UserApiClient.CREATE_USER_ERROR_MESSAGE;

import java.util.Optional;
import no.unit.nva.cognito.exception.CreateUserFailedException;
import no.unit.nva.cognito.model.User;

public class UserApiMock implements UserApi {

    private User user;

    @Override
    public Optional<User> getUser(String username) {
        return Optional.ofNullable(user);
    }

    @Override
    public User createUser(User user) {
        this.user = user;
        if (user != null) {
            return user;
        } else {
            throw new CreateUserFailedException(CREATE_USER_ERROR_MESSAGE);
        }
    }
}
