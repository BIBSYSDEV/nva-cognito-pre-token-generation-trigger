package no.unit.nva.cognito.service;

import com.amazonaws.services.cognitoidp.model.AttributeType;
import java.util.Optional;
import no.unit.nva.cognito.api.user.model.UserDto;

public interface UserApi {

    Optional<UserDto> getUser(String username);

    UserDto createUser(UserDto userDto);

    UserDto updateUser(UserDto userDto);

}
