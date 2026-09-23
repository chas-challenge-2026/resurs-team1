package se.comerit.resurs.exception.companyvalidation;

import se.comerit.resurs.exception.auth.LoginFailureReason;

public class CompanyValidationFailedException extends RuntimeException {
    private final CompanyValidationFailureReason reason;

    public CompanyValidationFailedException (CompanyValidationFailureReason reason){
        super(reason.name());
        this.reason = reason;
    }
    public CompanyValidationFailureReason reason(){
        return reason;
    }
}



