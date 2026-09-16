package se.comerit.resurs.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import se.comerit.resurs.client.companyvalidation.CompanyValidationClient;
import se.comerit.resurs.dto.companyvalidation.CompanyValidationApiDTO;
import se.comerit.resurs.enums.SigningRight;
import se.comerit.resurs.exception.companyvalidation.CompanyValidationFailedException;
import se.comerit.resurs.exception.companyvalidation.CompanyValidationFailureReason;

@Service
public class CompanyValidationService {

    @Autowired
    private final CompanyValidationClient client;

    public CompanyValidationService(CompanyValidationClient client){
        this.client = client;
    }

    public CompanyValidationApiDTO validateCompanyExists(String orgNumber) {
        return client.lookup(orgNumber)
                .orElseThrow(() -> new CompanyValidationFailedException(
                        CompanyValidationFailureReason.COMPANY_NOT_FOUND));
    }
    public void validateSignatory(CompanyValidationApiDTO company, String personalNumber) {
        CompanyValidationApiDTO.Signatory signatory = company.signatories().stream()
                .filter(s -> s.personalNumber().equals(personalNumber))
                .findFirst()
                .orElseThrow(() -> new CompanyValidationFailedException(
                        CompanyValidationFailureReason.NOT_AUTHORIZED_SIGNATORY));

        if (signatory.signingRight() != SigningRight.ALONE) {
            throw new CompanyValidationFailedException(
                    CompanyValidationFailureReason.REQUIRES_JOINT_SIGNATURE);
        }
    }
}

