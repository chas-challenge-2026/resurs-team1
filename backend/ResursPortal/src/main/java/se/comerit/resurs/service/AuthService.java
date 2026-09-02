package se.comerit.resurs.service;

import org.springframework.stereotype.Service;
import se.comerit.resurs.dto.auth.CompanyLoginResponse;
import se.comerit.resurs.exception.auth.LoginFailedException;
import se.comerit.resurs.exception.auth.LoginFailureReason;
import se.comerit.resurs.persistence.CompanyRepository;
import se.comerit.resurs.persistence.model.Company;


import java.util.Set;


@Service
public class AuthService {


    // BankID-mock — TODO: ersätt med riktig BankID-integration
    private static final Set<String> BANKID_APPROVED_ORG_NUMBERS =
            Set.of("556000-1234", "556000-5678");

    private final CompanyRepository companyRepository;

    public AuthService(CompanyRepository companyRepository) {
        this.companyRepository = companyRepository;
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



        // TODO: replace with real BankID integration


        // Case worker login with MD5 password — SQL built with string concat (injection surface)
        // TODO: parameterize this query and use bcrypt
 /*   public String loginCaseWorker(@RequestParam("email") String email,
                                  @RequestParam("password") String password,
                                  HttpSession session,
                                  Model model) {
        String md5 = md5Hash(password);
        // SQL injection surface: email is directly concatenated
        String sql = "SELECT * FROM case_workers WHERE email = '" + email + "' AND password_md5 = '" + md5 + "'";
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql);
        if (!rows.isEmpty()) {
            Map<String, Object> worker = rows.get(0);
            session.setAttribute("userId", worker.get("id"));
            session.setAttribute("role", "caseWorker");
            session.setAttribute("workerName", worker.get("name"));
            session.setAttribute("workerEmail", worker.get("email"));
            return "redirect:/backoffice";
        } else {
            model.addAttribute("error", "Felaktigt användarnamn eller lösenord.");
            model.addAttribute("activeTab", "caseWorker");
            return "login";
        }
    }

    public String logout(HttpSession session) {
        session.invalidate();
        return "redirect:/login";
    }
    // MD5 — weak, but matches DB seed
    // TODO: migrate to bcrypt before go-live
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
*/
    }

