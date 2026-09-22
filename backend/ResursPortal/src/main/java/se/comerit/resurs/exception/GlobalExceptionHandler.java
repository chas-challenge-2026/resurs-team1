package se.comerit.resurs.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import se.comerit.resurs.exception.auth.LoginFailedException;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;

@RestControllerAdvice
public class GlobalExceptionHandler {


    @ExceptionHandler(ObjectOptimisticLockingFailureException.class)
    public ResponseEntity<ApiError> handleOptimisticLockingFailure() {
        ApiError body = new ApiError(
                LocalDateTime.now(),
                "Update unsuccessful",
                "Resource was modified by another instance. Please refresh and try again.",
                null
        );
        return new ResponseEntity<>(body, HttpStatus.CONFLICT);
    }

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


}
