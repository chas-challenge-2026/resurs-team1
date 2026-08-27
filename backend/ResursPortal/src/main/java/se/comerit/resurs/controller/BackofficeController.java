package se.comerit.resurs.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import jakarta.servlet.http.HttpSession;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

/**
 * BackofficeController – Handläggargränssnitt för manuell granskning.
 *
 * Anti-patterns:
 *  - JdbcTemplate direkt i kontrollern
 *  - Audit log uppdateras via JSON string manipulation
 *  - Ingen e-postnotifiering vid beslut
 *  - Session check copy-pasteat
 *  - Ingen pagination — hämtar ALLA ansökningar i REVIEW
 */
@Controller
public class BackofficeController {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @GetMapping("/backoffice")
    public String backofficeOverview(HttpSession session, Model model) {
        // Session check copy-pasted in every method — should be an interceptor
        if (session.getAttribute("userId") == null) return "redirect:/login";
        if (!"caseWorker".equals(session.getAttribute("role"))) return "redirect:/login";

        // Hämtar ALLA UNDER_REVIEW — ingen pagination, ingen sortering, inget index
        // TODO: lägg till pagination och index på status-kolumnen
        List<Map<String, Object>> reviewApps = jdbcTemplate.queryForList(
            "SELECT a.id, a.requested_amount, a.purpose, a.status, a.created_at, " +
            "a.scoring_result, a.decision_reason, c.company_name, c.org_number " +
            "FROM applications a JOIN companies c ON a.company_id = c.id " +
            "WHERE a.status = 'UNDER_REVIEW' ORDER BY a.created_at ASC"
        );

        // Also get approved/rejected for history — same query pattern, no reuse
        List<Map<String, Object>> decidedApps = jdbcTemplate.queryForList(
            "SELECT a.id, a.requested_amount, a.purpose, a.status, a.decision, a.created_at, " +
            "a.updated_at, c.company_name, c.org_number " +
            "FROM applications a JOIN companies c ON a.company_id = c.id " +
            "WHERE a.status IN ('APPROVED', 'REJECTED') ORDER BY a.updated_at DESC LIMIT 20"
        );

        model.addAttribute("reviewApplications", reviewApps);
        model.addAttribute("decidedApplications", decidedApps);
        model.addAttribute("workerName", session.getAttribute("workerName"));
        model.addAttribute("reviewCount", reviewApps.size());
        return "backoffice";
    }

    @PostMapping("/backoffice/decide")
    public String decide(@RequestParam("applicationId") Long applicationId,
                         @RequestParam("decision") String decision,
                         @RequestParam(value = "comment", defaultValue = "") String comment,
                         HttpSession session,
                         Model model) {
        // Session check copy-pasted in every method — should be an interceptor
        if (session.getAttribute("userId") == null) return "redirect:/login";
        if (!"caseWorker".equals(session.getAttribute("role"))) return "redirect:/login";

        if (!"APPROVED".equals(decision) && !"REJECTED".equals(decision)) {
            return "redirect:/backoffice";
        }

        String workerName = (String) session.getAttribute("workerName");
        String newStatus = "APPROVED".equals(decision) ? "APPROVED" : "REJECTED";

        // Update application status and decision
        jdbcTemplate.update(
            "UPDATE applications SET status = ?, decision = ?, updated_at = NOW() WHERE id = ?",
            newStatus,
            decision,
            applicationId
        );

        // Append to audit log JSON blob — same string manipulation as elsewhere
        // No email notification sent — TODO: skicka e-post till företaget
        String auditEntry = "{\"ts\":\"" + LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
            + "\",\"action\":\"MANUAL_DECISION\",\"decision\":\"" + decision
            + "\",\"worker\":\"" + workerName.replace("\"", "'") + "\""
            + (comment.isEmpty() ? "" : ",\"comment\":\"" + comment.replace("\"", "'") + "\"")
            + "}";

        String currentLog = jdbcTemplate.queryForObject(
            "SELECT audit_log FROM applications WHERE id = ?",
            String.class,
            applicationId
        );

        String updatedLog;
        if (currentLog == null || currentLog.equals("[]")) {
            updatedLog = "[" + auditEntry + "]";
        } else {
            updatedLog = currentLog.substring(0, currentLog.lastIndexOf("]")) + "," + auditEntry + "]";
        }

        jdbcTemplate.update(
            "UPDATE applications SET audit_log = ? WHERE id = ?",
            updatedLog,
            applicationId
        );

        // No email notification — TODO: implement email via Spring Mail in v2
        // TODO: notify company via email when decision is made

        return "redirect:/backoffice";
    }

    @GetMapping("/backoffice/application/{id}")
    public String viewApplicationDetail(
            @RequestParam(value = "id", required = false) Long pathId,
            @org.springframework.web.bind.annotation.PathVariable("id") Long id,
            HttpSession session,
            Model model) {
        // Session check copy-pasted in every method — should be an interceptor
        if (session.getAttribute("userId") == null) return "redirect:/login";
        if (!"caseWorker".equals(session.getAttribute("role"))) return "redirect:/login";

        List<Map<String, Object>> apps = jdbcTemplate.queryForList(
            "SELECT a.*, c.company_name, c.org_number, c.authorized_signatory " +
            "FROM applications a JOIN companies c ON a.company_id = c.id WHERE a.id = ?",
            id
        );

        if (apps.isEmpty()) {
            return "redirect:/backoffice";
        }

        Map<String, Object> app = apps.get(0);
        model.addAttribute("application", app);
        model.addAttribute("auditLogRaw", app.get("audit_log"));
        model.addAttribute("workerName", session.getAttribute("workerName"));

        List<Map<String, Object>> docs = jdbcTemplate.queryForList(
            "SELECT * FROM documents WHERE application_id = ? ORDER BY uploaded_at DESC",
            id
        );
        model.addAttribute("documents", docs);

        return "backoffice_detail";
    }
}
