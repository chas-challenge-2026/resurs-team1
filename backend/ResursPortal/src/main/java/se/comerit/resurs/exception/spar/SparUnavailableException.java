package se.comerit.resurs.exception.spar;

public class SparUnavailableException extends RuntimeException {
    public SparUnavailableException(Throwable cause) {
        super("SPAR did not respond", cause);
    }
}
