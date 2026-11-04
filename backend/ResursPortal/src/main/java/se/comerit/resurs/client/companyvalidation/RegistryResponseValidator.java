package se.comerit.resurs.client.companyvalidation;


import jakarta.validation.ConstraintViolation;
import org.springframework.stereotype.Component;
import se.comerit.resurs.exception.companyvalidation.InvalidRegistryResponseException;

import jakarta.validation.Validator;
import java.util.Set;

@Component
public class RegistryResponseValidator {

    private final Validator validator;

    public RegistryResponseValidator(Validator validator){
        this.validator = validator;
    }

    public <T> T validate(T response, String orgNumber) {
        Set<ConstraintViolation<T>> violations = validator.validate(response);
        if (!violations.isEmpty()) {
            throw new InvalidRegistryResponseException(orgNumber, violations);
        }
        return response;
    }
}
