package se.comerit.resurs.dto.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record CaseWorkerLoginRequest(@NotBlank @Email String email,
                                     @NotBlank String password){}
