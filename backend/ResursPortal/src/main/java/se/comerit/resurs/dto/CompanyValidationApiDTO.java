package se.comerit.resurs.dto;

//DTO meant to mock the structure of company validation from external API
public record CompanyValidationApiDTO (
        String orgNumber,
        String name,
        String authorizedSignatory,
        Boolean active
){


}
