package se.comerit.resurs.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import jakarta.servlet.http.HttpSession;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import se.comerit.resurs.dto.status.StatusDetails;
import se.comerit.resurs.service.StatusService;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * StatusController – Visar ansökningsstatus med hårdkodade ETAer.
 *
 * Anti-patterns:
 *  - Hårdkodade ETAer ("2 dagar", "3 dagar") oavsett faktiskt tillstånd
 *  - JdbcTemplate direkt i kontrollern
 *  - Session check copy-pasteat
 *  - Statussteg beräknas inte dynamiskt — alltid samma ordning
 */
@RestController
@RequestMapping("/status")
public class StatusController {


    private final StatusService service;

    public StatusController(StatusService service) {
        this.service = service;
    }

    @GetMapping("/{applicationId}")
    public ResponseEntity<StatusDetails> showStatus(@PathVariable("applicationId") Long applicationId,
                                    HttpSession session,
                                    Model model) {
        // Session check copy-pasted in every method — should be an interceptor
        if (session.getAttribute("userId") == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();


        /*
        if (apps.isEmpty()) {
            return "redirect:/applications";
        }*/
        return ResponseEntity.ok(service.showStatus(applicationId));

    }

}
