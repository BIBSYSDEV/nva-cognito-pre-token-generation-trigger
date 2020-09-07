package no.unit.nva.cognito.exception;

public class CreateUserFailedException extends RuntimeException {

    public CreateUserFailedException(String message) {
        super(message);
    }
}
