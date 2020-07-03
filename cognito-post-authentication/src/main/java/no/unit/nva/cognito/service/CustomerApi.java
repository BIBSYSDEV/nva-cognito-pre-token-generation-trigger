package no.unit.nva.cognito.service;

import java.util.Optional;

public interface CustomerApi {

    Optional<String> getCustomerId(String orgNumber);
}
