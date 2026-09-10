package se.comerit.resurs.dto;

import se.comerit.resurs.persistence.model.Company;

public record CompanyDTO (
        Long id,
        String companyName,
        String org_number,
        String authorizedSignatory
        ){

    public CompanyDTO(Company company){
         this(company.getId(), company.getCompany_name(), company.getOrg_number(), company.getAuthorized_signatory());
    }


}
