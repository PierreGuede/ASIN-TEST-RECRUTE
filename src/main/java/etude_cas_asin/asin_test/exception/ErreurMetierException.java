package etude_cas_asin.asin_test.exception;

public class ErreurMetierException extends RuntimeException {
    private final int statusCode;
    private final String message;

    public ErreurMetierException(int statusCode, String message) {
        super(message);
        this.statusCode = statusCode;
        this.message = message;
    }

    public int getStatusCode() {
        return statusCode;
    }

    @Override
    public String getMessage() {
        return message;
    }
}
