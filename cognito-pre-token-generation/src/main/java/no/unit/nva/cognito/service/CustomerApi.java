package no.unit.nva.cognito.service;

import java.util.Optional;
import no.unit.nva.cognito.api.lambda.event.CustomerResponse;

public interface CustomerApi {

    Optional<CustomerResponse> getCustomer(String orgNumber);
}
