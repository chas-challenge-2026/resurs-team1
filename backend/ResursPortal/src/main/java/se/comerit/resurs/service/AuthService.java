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
import se.comerit.resurs.dto.CompanyValidationApiDTO;

/**
 * AuthService -> hanterar inloggning för företag och handläggare.
 *
 * Ansvarar för: binder ihop BankID-verifieringen (BankIdService) och företagsvalidering (CompanyValidationService)
 * till ett enda inloggningsbeslut. Den hanterar också handläggarinloggning.
 *
 * Inte ansvarig för: hur BankID eller CompanyValidation kommer fram till sina resultat -> deras undantag släpps igenom
 * obehandlade, samma mönster som i övriga services i projektet.
 *
 * Kommentaren stämmer när CompanyValidation har mergat in.
 */

@Service
public class AuthService {
    private final CompanyRepository companyRepository;
    private final CaseWorkerRepository caseWorkerRepository;
    private final PasswordHasher passwordHasher;
    private final BankIdService bankIdService;
//    private final CompanyValidationService companyValidationService;

    public AuthService(CaseWorkerRepository caseWorkerRepository, CompanyRepository companyRepository,
                       PasswordHasher passwordHasher, BankIdService bankIdService) {
        this.caseWorkerRepository = caseWorkerRepository;
        this.companyRepository = companyRepository;
        this.passwordHasher = passwordHasher;
        this.bankIdService = bankIdService;
    }

    // BankID mock — hardcoded org numbers, real BankID integration skipped -> old comment
    // TODO: replace with real BankID integration -> old comment
    public CompanyLoginResponse loginCompany(String orgNumber, String personalNumber) {
        Company company = companyRepository.findByOrgNumber(orgNumber)
                .orElseThrow(() -> new LoginFailedException(LoginFailureReason.COMPANY_NOT_FOUND));
        if (!bankIdService.verify(personalNumber).name().equalsIgnoreCase(company.getAuthorized_signatory())) {
            throw new LoginFailedException(LoginFailureReason.BANKID_REJECTED);
        }

        return new CompanyLoginResponse(
                orgNumber,
                "company",
                company.getCompany_name()
        );
    }
    // TODO: Ersätt med CompanyValidationService.validateSignatory() när feature/company-validation är mergad

//        bankIdService.verify(personalNumber);
//        CompanyValidationApiDTO companyValidationApiDTO = companyValidationService.validateCompanyExists(orgNumber);
//        companyValidationService.validateSignatory(companyValidationApiDTO, personalNumber);
//
//
//            return new CompanyLoginResponse(
//                    orgNumber,
//                    "company",
//                    companyValidationApiDTO.companyName()
//            );

    public CaseWorkerLoginResponse loginCaseWorker(String email, String password) {
        CaseWorker worker = caseWorkerRepository.findByEmail(email)
                .orElseThrow(() -> new LoginFailedException(LoginFailureReason.BAD_CREDENTIALS));


        if (!worker.getPasswordHash().equals(passwordHasher.md5Hash(password))) {
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