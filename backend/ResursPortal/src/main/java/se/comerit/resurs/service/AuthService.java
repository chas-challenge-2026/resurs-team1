package se.comerit.resurs.service;
import org.springframework.stereotype.Service;
import se.comerit.resurs.dto.auth.CaseWorkerLoginResponse;
import se.comerit.resurs.dto.auth.CompanyLoginResponse;
import se.comerit.resurs.exception.auth.LoginFailedException;
import se.comerit.resurs.exception.auth.LoginFailureReason;
import se.comerit.resurs.persistence.CaseWorkerRepository;
import se.comerit.resurs.persistence.model.CaseWorker;
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
 */

@Service
public class AuthService {
    private final CaseWorkerRepository caseWorkerRepository;
    private final PasswordHasher passwordHasher;
    private final BankIdService bankIdService;
    private final CompanyValidationService companyValidationService;

    public AuthService(CaseWorkerRepository caseWorkerRepository,
                       PasswordHasher passwordHasher, BankIdService bankIdService, CompanyValidationService companyValidationService) {
        this.caseWorkerRepository = caseWorkerRepository;
        this.passwordHasher = passwordHasher;
        this.bankIdService = bankIdService;
        this.companyValidationService = companyValidationService;
    }

    // BankID mock — hardcoded org numbers, real BankID integration skipped -> old comment
    // TODO: replace with real BankID integration -> old comment
    public CompanyLoginResponse loginCompany(String orgNumber, String personalNumber) {
        bankIdService.verify(personalNumber);

        CompanyValidationApiDTO companyValidationApiDTO = companyValidationService.validateCompanyExists(orgNumber);
        companyValidationService.validateSignatory(companyValidationApiDTO, personalNumber);


            return new CompanyLoginResponse(
                    orgNumber,
                    "company",
                    companyValidationApiDTO.companyName()
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