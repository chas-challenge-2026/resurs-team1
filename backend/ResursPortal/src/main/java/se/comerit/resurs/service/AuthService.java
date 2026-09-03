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


import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Set;


@Service
public class AuthService {


    // BankID-mock — TODO: ersätt med riktig BankID-integration
    private static final Set<String> BANKID_APPROVED_ORG_NUMBERS =
            Set.of("556000-1234", "556000-5678");

    private final CompanyRepository companyRepository;
    private final CaseWorkerRepository caseWorkerRepository;

    public AuthService(CompanyRepository companyRepository, CaseWorkerRepository caseWorkerRepository) {
        this.companyRepository = companyRepository;
        this.caseWorkerRepository = caseWorkerRepository;
    }


    // BankID mock — hardcoded org numbers, real BankID integration skipped
    // TODO: replace with real BankID integration
    public CompanyLoginResponse loginCompany(String orgNumber) {
        if (!BANKID_APPROVED_ORG_NUMBERS.contains(orgNumber)) {
            throw new LoginFailedException(LoginFailureReason.BANKID_REJECTED);
        }
            Company company = companyRepository.findByOrgNumber(orgNumber)
                    .orElseThrow(() -> new LoginFailedException(LoginFailureReason.COMPANY_NOT_FOUND));

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


            if (!worker.getPasswordHash().equals(md5Hash(password))){
                throw new LoginFailedException(LoginFailureReason.BAD_CREDENTIALS);
            }
            return new CaseWorkerLoginResponse(
                    worker.getId(),
                    "caseWorker",
                    worker.getName(),
                    worker.getEmail()
            );
        }

        // TODO: parameterize this query and use bcrypt

    private String md5Hash(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] hash = md.digest(input.getBytes());
            StringBuilder sb = new StringBuilder();
            for (byte b : hash) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("MD5 not available", e);
        }
    }
}



