package no.unit.nva.cognito.service;

import java.util.Optional;
import no.unit.nva.cognito.model.User;

public interface UserApi {

    Optional<User> getUser(String username);

    void createUser(User user);
}
