package no.unit.nva.cognito.service;

import java.util.Optional;
import no.unit.nva.cognito.model.User;

public class UserApiMock implements UserApi {

    private User user;

    @Override
    public Optional<User> getUser(String username) {
        return Optional.ofNullable(user);
    }

    @Override
    public Optional<User> createUser(User user) {
        this.user = user;
        return Optional.ofNullable(this.user);
    }
}
