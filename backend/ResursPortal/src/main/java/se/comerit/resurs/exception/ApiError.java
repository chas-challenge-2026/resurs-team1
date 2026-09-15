package se.comerit.resurs.exception;

import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.http.HttpStatus;

import java.time.LocalDateTime;
import java.util.Map;


// for the message, use the below only if the error is safe to push to the end user.
//      Optional.ofNullable(ex.getMessage())
//       .orElse("An Unexpected Error Occured")
//
// Otherwise set an explicit message

@Schema(description = "Standard error response")
public record ApiError(
        LocalDateTime timestamp,
        String error,
        String message,
        Map<String,String> details
) { }
