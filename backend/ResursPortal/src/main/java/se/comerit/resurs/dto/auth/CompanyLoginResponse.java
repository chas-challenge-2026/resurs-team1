package se.comerit.resurs.dto.auth;

public record CompanyLoginResponse(
        String orgNumber,
        String role,
        String companyName) {}
