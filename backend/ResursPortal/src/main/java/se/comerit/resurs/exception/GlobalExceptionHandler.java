package se.comerit.resurs.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import se.comerit.resurs.exception.auth.LoginFailedException;
import se.comerit.resurs.exception.companyvalidation.CompanyRegistryUnavailableException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.comerit.resurs.exception.companyvalidation.CompanyValidationFailedException;
import se.comerit.resurs.exception.companyvalidation.CompanyValidationFailureReason;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;

@RestControllerAdvice
public class GlobalExceptionHandler {


    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ApiError> handleMissingRequestParam(Exception ex) {
        ApiError body = new ApiError(
                LocalDateTime.now(),
                "Missing Parameter",
                Optional.ofNullable(ex.getMessage())
                        .orElse("Missing Parameter"),
                null
        );
        return new ResponseEntity<>(body, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(DocumentStorageException.class)
    public ResponseEntity<ApiError> handleDocumentUpload(Exception ex) {
        ApiError body = new ApiError(
                LocalDateTime.now(),
                "Upload Failed",
                Optional.ofNullable(ex.getMessage())
                        .orElse("Failed to upload file"),
                null
        );
        return new ResponseEntity<>(body, HttpStatus.INTERNAL_SERVER_ERROR);
    }


    @ExceptionHandler(LoginFailedException.class)
    public ResponseEntity<ApiError> handleBadCredentials(Exception ex) {
        ApiError body = new ApiError(
                LocalDateTime.now(),
                "Login Failed",
                Optional.ofNullable(ex.getMessage())
                        .orElse("Bad Credentials"),
                null
        );
        return new ResponseEntity<>(body, HttpStatus.UNAUTHORIZED);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiError> handleIllegalArgument(Exception ex) {
        ApiError body = new ApiError(
                LocalDateTime.now(),
                "Illegal Argument",
                Optional.ofNullable(ex.getMessage())
                        .orElse("Illegal arguments were passed"),
                null
        );
        return new ResponseEntity<>(body, HttpStatus.BAD_REQUEST);
    }


    @ExceptionHandler(NoSuchElementException.class)
    public ResponseEntity<ApiError> handleNoSuchElement(Exception ex) {
        ApiError body = new ApiError(
                LocalDateTime.now(),
                "No element with that ID",
                Optional.ofNullable(ex.getMessage())
                        .orElse("An Unexpected Error Occured"),
                null
        );
        return new ResponseEntity<>(body, HttpStatus.NOT_FOUND);
    }

    //Future proofing for Jakarta validation annotation
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleValidationException(MethodArgumentNotValidException ex) {

        Map<String, String> fieldErrors = new HashMap<>();

        // 1. Field-level errors (Jakarta annotations, @NotNull etc)
        ex.getBindingResult().getFieldErrors().forEach(error ->
                fieldErrors.put(error.getField(), error.getDefaultMessage())
        );

        // 2. Global / class-level ( custom validator)
        ex.getBindingResult().getGlobalErrors().forEach(error ->
                fieldErrors.put("global", error.getDefaultMessage())
        );

        ApiError body = new ApiError(
                LocalDateTime.now(),
                "Validation Error",
                "Request Validation Failed",
                fieldErrors);

        return new ResponseEntity<>(body, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleGenericException(Exception ex) {
        ApiError body = new ApiError(
                LocalDateTime.now(),
                "Internal Server Error",
                Optional.ofNullable(ex.getMessage())
                        .orElse("An Unexpected Error Occured"),
                null
        );
        return new ResponseEntity<>(body, HttpStatus.INTERNAL_SERVER_ERROR);
    }
    @ExceptionHandler(CompanyRegistryUnavailableException.class)
    public ResponseEntity<ApiError> handleRegisterUnavailableException(
            CompanyRegistryUnavailableException ex
    ) {
        ApiError body = new ApiError(
                LocalDateTime.now(),
                "COMPANY_REGISTRY_UNAVAILABLE",
                "The company registry is temporarily unavailable. Please try again shortly.",
                null
        );
        return new ResponseEntity<>(body, HttpStatus.SERVICE_UNAVAILABLE);
    }

    @ExceptionHandler(CompanyValidationFailedException.class)
    public ResponseEntity<ApiError> handleCompanyValidationFailed(CompanyValidationFailedException ex) {

        HttpStatus status = switch (ex.reason()) {
            case COMPANY_NOT_FOUND -> HttpStatus.NOT_FOUND;
            case NOT_AUTHORIZED_SIGNATORY, REQUIRES_JOINT_SIGNATURE -> HttpStatus.FORBIDDEN;
        };

        String message = switch (ex.reason()) {
            case COMPANY_NOT_FOUND -> "No company is registered with that organisation number";
            case NOT_AUTHORIZED_SIGNATORY -> "You are not a registered signatory for this company";
            case REQUIRES_JOINT_SIGNATURE -> "This company must be signed for jointly and cannot be signed by one person alone";
        };

        ApiError body = new ApiError(LocalDateTime.now(), ex.reason().name(), message, null);
        return new ResponseEntity<>(body, status);
    }


}
