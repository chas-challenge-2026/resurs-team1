package se.comerit.resurs.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record ContactDetails(
        @NotBlank String name,
        @NotBlank @Email String email,
        @NotBlank String phoneNumber

) {
}
