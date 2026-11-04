package se.comerit.resurs.service;

import org.springframework.stereotype.Service;
import se.comerit.resurs.dto.CompanyDTO;
import se.comerit.resurs.persistence.CompanyRepository;
import se.comerit.resurs.persistence.model.Company;
import se.comerit.resurs.util.HMACHasher;

@Service
public class CompanyService {

    private final CompanyRepository companyRepository;
    private final HMACHasher hasher;


    public CompanyService(CompanyRepository companyRepository, HMACHasher hasher) {
        this.companyRepository = companyRepository;
        this.hasher = hasher;
    }

    //Largely unused as we propagate the company entity from application, Could still be useful to keep.
    public Company createCompany(String companyName, String org_number, String authorizedSignatory){
        // INSERT company — PII in plaintext, no encryption
        // TODO: encrypt PII before go-live

        return companyRepository.save(new Company(org_number,companyName,authorizedSignatory));
    }

    public CompanyDTO getCompanyFromOrgNumber(String orgNumber){
        return new CompanyDTO(companyRepository.findBySearchKeyOrgNumber(hasher.encode(orgNumber)).orElseThrow());
    }

}
