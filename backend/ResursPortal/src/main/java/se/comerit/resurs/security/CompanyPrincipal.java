package se.comerit.resurs.security;

public record CompanyPrincipal(String orgNumber, String companyName, String personalNumber, String authorized_signatory) {
}
