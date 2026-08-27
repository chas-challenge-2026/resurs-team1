package se.comerit.resurs.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import jakarta.servlet.http.HttpSession;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.Map;

@Controller
public class AuthController {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @GetMapping("/")
    public String root() { return "redirect:/login"; }

    @GetMapping("/login")
    public String loginPage(HttpSession session, Model model) {
        if (session.getAttribute("userId") != null) {
            String role = (String) session.getAttribute("role");
            if ("caseWorker".equals(role)) {
                return "redirect:/backoffice";
            }
            return "redirect:/apply";
        }
        model.addAttribute("error", null);
        return "login";
    }

    // BankID mock — hardcoded org numbers, real BankID integration skipped
    // TODO: replace with real BankID integration
    @PostMapping("/login/company")
    public String loginCompany(@RequestParam("orgNumber") String orgNumber,
                               HttpSession session,
                               Model model) {
        // TODO: replace with real BankID integration
        if (orgNumber.equals("556000-1234") || orgNumber.equals("556000-5678")) {
            // BankID authentication successful (mock)
            // Look up company in DB
            List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                "SELECT * FROM companies WHERE org_number = '" + orgNumber + "'"
            );
            if (rows.isEmpty()) {
                model.addAttribute("error", "Företaget hittades inte i systemet.");
                model.addAttribute("activeTab", "company");
                return "login";
            }
            Map<String, Object> company = rows.get(0);
            session.setAttribute("userId", company.get("id"));
            session.setAttribute("role", "company");
            session.setAttribute("orgNumber", orgNumber);
            session.setAttribute("companyName", company.get("company_name"));
            session.setAttribute("companyId", company.get("id"));
            return "redirect:/apply";
        } else {
            // Not in whitelist — BankID mock rejects
            model.addAttribute("error", "BankID-autentisering misslyckades. Org.nummer ej godkänt.");
            model.addAttribute("activeTab", "company");
            return "login";
        }
    }

    // Case worker login with MD5 password — SQL built with string concat (injection surface)
    // TODO: parameterize this query and use bcrypt
    @PostMapping("/login/caseWorker")
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

    @GetMapping("/logout")
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
