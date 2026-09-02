package se.comerit.resurs.service;

import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Service;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import se.comerit.resurs.dto.auth.CompanyLoginRequest;
import se.comerit.resurs.dto.auth.CompanyLoginResponse;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.Map;


@Service
public class AuthService {

    private final

    @GetMapping("/")
    public String root() { return "redirect:/login"; }


    // BankID mock — hardcoded org numbers, real BankID integration skipped
    // TODO: replace with real BankID integration
    public CompanyLoginResponse loginCompany(CompanyLoginRequest loginRequest) {




        // TODO: replace with real BankID integration


        }

    // Case worker login with MD5 password — SQL built with string concat (injection surface)
    // TODO: parameterize this query and use bcrypt
    public String loginCaseWorker(@RequestParam("email") String email,
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

}
