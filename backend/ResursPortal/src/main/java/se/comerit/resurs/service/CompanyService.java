package se.comerit.resurs.service;

import org.springframework.stereotype.Service;
import se.comerit.resurs.dto.CompanyDTO;
import se.comerit.resurs.persistence.CompanyRepository;
import se.comerit.resurs.persistence.model.Company;

@Service
public class CompanyService {

    private final CompanyRepository companyRepository;


    public CompanyService(CompanyRepository companyRepository) {
        this.companyRepository = companyRepository;
    }

    public CompanyDTO createCompany(String companyName, String org_number, String authorizedSignatory){
        return new CompanyDTO(companyRepository.save(new Company(org_number,companyName,authorizedSignatory)));
    }

    public CompanyDTO getCompanyFromOrgNumber(String orgNumber){
        return new CompanyDTO(companyRepository.findByOrgNumber(orgNumber).orElseThrow());
    }

}
