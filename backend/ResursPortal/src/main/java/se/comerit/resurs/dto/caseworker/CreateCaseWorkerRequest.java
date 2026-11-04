package se.comerit.resurs.dto.caseworker;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
// Vad klienten skickar in för att skapa ett konto
public record CreateCaseWorkerRequest(
        @NotBlank String name,
        @NotBlank @Email String email,
        @NotBlank String password) {
}
