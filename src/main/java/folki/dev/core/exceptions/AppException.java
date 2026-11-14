package folki.dev.core.exceptions;

public class AppException extends RuntimeException {
    private final String message;

    public AppException(String message) {
        super(message);
        this.message = message;
    }

    @Override
    public String getMessage() {
        return message;
    }
}
