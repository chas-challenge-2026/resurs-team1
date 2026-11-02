package se.comerit.resurs.service;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import se.comerit.resurs.dto.auth.CaseWorkerLoginResponse;
import se.comerit.resurs.dto.auth.CompanyLoginResponse;
import se.comerit.resurs.dto.bankid.BankIdVerificationResult;
import se.comerit.resurs.dto.companyvalidation.CompanyValidationApiDTO;
import se.comerit.resurs.exception.auth.LoginFailedException;
import se.comerit.resurs.exception.auth.LoginFailureReason;
import se.comerit.resurs.persistence.CaseWorkerRepository;
import se.comerit.resurs.persistence.model.CaseWorker;
import se.comerit.resurs.persistence.model.Role;
import se.comerit.resurs.util.HMACHasher;

import java.util.Set;
import java.util.stream.Collectors;


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
    private final CompanyValidationService validationService;
    private final CaseWorkerRepository caseWorkerRepository;
    private final PasswordEncoder passwordEncoder;
    private final BankIdService bankIdService;
    private final HMACHasher hasher;


    public AuthService(BankIdService bankIdService, CompanyValidationService validationService, CaseWorkerRepository caseWorkerRepository, PasswordEncoder passwordEncoder, HMACHasher hasher) {
        this.bankIdService = bankIdService;
        this.validationService = validationService;
        this.caseWorkerRepository = caseWorkerRepository;
        this.passwordEncoder = passwordEncoder;
        this.hasher = hasher;
    }

    // BankID mock — hardcoded org numbers, real BankID integration skipped -> old comment
    // TODO: replace with real BankID integration -> old comment
    public CompanyLoginResponse loginCompany(String orgNumber, String personalNumber) {
         BankIdVerificationResult verified = bankIdService.verify(personalNumber);
         CompanyValidationApiDTO company = validationService.validateCompanyExists(orgNumber);
         validationService.validateSignatory(company, personalNumber);

        return new CompanyLoginResponse(
                orgNumber,
                "company",
                company.companyName(),
                verified.name()
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
        CaseWorker worker = caseWorkerRepository.findBySearchKeyEmail(hasher.encode(email))
                .orElseThrow(() -> new LoginFailedException(LoginFailureReason.BAD_CREDENTIALS));


        if (!passwordEncoder.matches(password, worker.getPasswordHash())) {
            throw new LoginFailedException(LoginFailureReason.BAD_CREDENTIALS);
        }

        Set<String> rolesSet = worker.getRoles().stream().map(Role::getName).collect(Collectors.toSet());
        rolesSet.add("ROLE_CASEWORKER");

        return new CaseWorkerLoginResponse(
                worker.getId(),
                rolesSet,
                worker.getName(),
                worker.getEmail()
        );
    }
}