package se.comerit.resurs.dto.backoffice;

import jakarta.validation.constraints.NotBlank;

/**
 * ApplicationCommentRequest -> det handläggaren skickar in när hen skriver en kommentar på ett ärende
 *
 * Innehåller bara själva kommentaren (comment) som är en fri text.
 * Den får inte vara tom (@NotBlank) och har heller inte någon längdgräns eftersom ingen @Size är satt.
 *
 * Representerar bara indata -> valideringen sker i BackofficeController via @Valid
 *
 */
public record ApplicationCommentRequest(@NotBlank String comment) {
}
