package se.comerit.resurs.dto.auth;

public record CompanyLoginResponse(Long userId,
                                   String role,
                                   String orgNumber,
                                   String companyName) {}
