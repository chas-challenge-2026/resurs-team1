package se.comerit.resurs.dto.caseworker;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
// Vad klienten skickar in för att ändra namn/email
public record UpdateCaseWorkerRequest(
        @NotBlank  String name,
        @NotBlank @Email String email) {
}
