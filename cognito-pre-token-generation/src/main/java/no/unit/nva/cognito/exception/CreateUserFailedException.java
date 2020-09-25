package no.unit.nva.cognito.exception;

public class CreateUserFailedException extends RuntimeException {

    public static final String ERROR_MESSAGE_FORMAT = "%s (isConflict: %s)";
    private final boolean isConflict;

    public CreateUserFailedException(String message, boolean isConflict) {
        super(String.format(ERROR_MESSAGE_FORMAT, message, isConflict));
        this.isConflict = isConflict;
    }

    public boolean isConflictWithExistingUser() {
        return isConflict;
    }

}
