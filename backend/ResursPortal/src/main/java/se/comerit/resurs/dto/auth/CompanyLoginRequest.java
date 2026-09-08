package se.comerit.resurs.dto.auth;

import jakarta.validation.constraints.NotBlank;

public record CompanyLoginRequest(@NotBlank String orgNumber){}
