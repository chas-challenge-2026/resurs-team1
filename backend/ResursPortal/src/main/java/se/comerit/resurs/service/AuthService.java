package se.comerit.resurs.service;

import org.springframework.stereotype.Service;
import se.comerit.resurs.dto.auth.CaseWorkerLoginResponse;
import se.comerit.resurs.dto.auth.CompanyLoginResponse;
import se.comerit.resurs.exception.auth.LoginFailedException;
import se.comerit.resurs.exception.auth.LoginFailureReason;
import se.comerit.resurs.persistence.CaseWorkerRepository;
import se.comerit.resurs.persistence.CompanyRepository;
import se.comerit.resurs.persistence.model.CaseWorker;
import se.comerit.resurs.persistence.model.Company;
import se.comerit.resurs.security.PasswordHasher;

@Service
public class AuthService {
    private final CompanyRepository companyRepository;
    private final CaseWorkerRepository caseWorkerRepository;
    private final ValidationInterface validationInterface;
    private final PasswordHasher passwordHasher;

    public AuthService(CompanyRepository companyRepository, CaseWorkerRepository caseWorkerRepository,
                       PasswordHasher passwordHasher, ValidationInterface validationInterface) {
        this.companyRepository = companyRepository;
        this.caseWorkerRepository = caseWorkerRepository;
        this.validationInterface = validationInterface;
        this.passwordHasher = passwordHasher;
    }

    // BankID mock — hardcoded org numbers, real BankID integration skipped
    // TODO: replace with real BankID integration
    public CompanyLoginResponse loginCompany(String orgNumber, String signatoryName) {
        if (!validationInterface.isApproved(orgNumber)) {
            throw new LoginFailedException(LoginFailureReason.BANKID_REJECTED);
        }
            Company company = companyRepository.findByOrgNumber(orgNumber)
                    .orElseThrow(() -> new LoginFailedException(LoginFailureReason.COMPANY_NOT_FOUND));

        if (!signatoryName.equalsIgnoreCase(company.getAuthorized_signatory())) {
            throw new LoginFailedException(LoginFailureReason.BANKID_REJECTED);
        }

            return new CompanyLoginResponse(
                    company.getId(),
                    "company"
                    , company.getOrg_number(),
                    company.getCompany_name()
            );
        }

        public CaseWorkerLoginResponse loginCaseWorker(String email, String password){
            CaseWorker worker = caseWorkerRepository.findByEmail(email)
                    .orElseThrow(() -> new LoginFailedException(LoginFailureReason.BAD_CREDENTIALS));


            if (!worker.getPasswordHash().equals(passwordHasher.md5Hash(password))){
                throw new LoginFailedException(LoginFailureReason.BAD_CREDENTIALS);
            }
            return new CaseWorkerLoginResponse(
                    worker.getId(),
                    "caseWorker",
                    worker.getName(),
                    worker.getEmail()
            );
        }
}