package se.comerit.resurs.exception.auth;

public class LoginFailedException extends RuntimeException {

    private final LoginFailureReason reason;

    public LoginFailedException(LoginFailureReason reason) {
        super(reason.name());
        this.reason = reason;
    }

    public LoginFailureReason reason(){
        return reason;
    }
}
