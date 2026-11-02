package se.comerit.resurs.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import se.comerit.resurs.dto.audit.ChainVerificationResponse;
import se.comerit.resurs.service.AuditService;

//TODO make this admin only?
@RestController
@RequestMapping("api/audit")
public class AuditController {

    private final AuditService service;
    public AuditController(AuditService service) {
        this.service = service;
    }

    @GetMapping("/{id}")
    public ResponseEntity<ChainVerificationResponse> verifyChain(@PathVariable Long id){
        return ResponseEntity.ok(service.verifyAuditChain(id));
    }


}
