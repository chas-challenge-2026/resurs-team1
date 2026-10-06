package se.comerit.resurs.exception.companyvalidation;

import jakarta.validation.ConstraintViolation;

import java.util.Set;
import java.util.stream.Collectors;

public class InvalidRegistryResponseException extends RuntimeException {
    public InvalidRegistryResponseException(String orgNumber, Set<? extends ConstraintViolation<?>> violations) {
        super("Invalid registry response for " + orgNumber + ": " + violations.stream()
                .map(violation -> violation.getPropertyPath() + " " + violation.getMessage())
                .sorted()
                .collect(Collectors.joining(", ")));
    }
}