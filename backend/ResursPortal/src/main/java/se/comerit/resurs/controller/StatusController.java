package se.comerit.resurs.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import se.comerit.resurs.dto.status.StatusDetails;
import se.comerit.resurs.service.StatusService;

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
@RequestMapping("api/status")
public class StatusController {


    private final StatusService service;

    public StatusController(StatusService service) {
        this.service = service;
    }

    @GetMapping("/{applicationId}")
    public ResponseEntity<StatusDetails> showStatus(@PathVariable("applicationId") Long applicationId) {
        return ResponseEntity.ok(service.showStatus(applicationId));

    }
}
