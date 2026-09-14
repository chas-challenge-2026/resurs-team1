package se.comerit.resurs.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;

@RestControllerAdvice
public class GlobalExceptionHandler {



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
