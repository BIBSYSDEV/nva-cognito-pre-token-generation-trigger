package no.unit.nva.cognito.service;

public class TemporaryUnavailableException extends RuntimeException {

    public TemporaryUnavailableException(String message) {
        super(message);
    }
}
