package se.comerit.resurs.controller;

import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import se.comerit.resurs.dto.caseworker.CaseWorkerResponse;
import se.comerit.resurs.dto.caseworker.CreateCaseWorkerRequest;
import se.comerit.resurs.dto.caseworker.UpdateCaseWorkerRequest;
import se.comerit.resurs.service.CaseWorkerService;
/**
 * TODO: We need to create a Admin role that manages CRUD on caseworkers, a casual caseworker should not have this
 *  permission.
 * */
@RestController
@RequestMapping("/api/caseworkers")
public class CaseWorkerController {
    private final CaseWorkerService caseWorkerService;

    public CaseWorkerController(CaseWorkerService caseWorkerService) {
        this.caseWorkerService = caseWorkerService;
    }

    @PostMapping
    public ResponseEntity<CaseWorkerResponse> create(@Valid @RequestBody CreateCaseWorkerRequest request,
                                                     HttpSession session) {
        if (session.getAttribute("userId") == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        if (!"caseWorker".equals(session.getAttribute("role"))) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        return ResponseEntity.status(HttpStatus.CREATED).body(caseWorkerService.create(request));
    }

    @GetMapping("/{id}")
    public ResponseEntity<CaseWorkerResponse> getById(@PathVariable("id") Long id, HttpSession session) {
        if (session.getAttribute("userId") == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        if (!"caseWorker".equals(session.getAttribute("role"))) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
            return ResponseEntity.ok(caseWorkerService.getById(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<CaseWorkerResponse> update(@PathVariable("id") Long id,
                                                     @Valid @RequestBody UpdateCaseWorkerRequest request,
                                                     HttpSession session) {
        if (session.getAttribute("userId") == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        if (!"caseWorker".equals(session.getAttribute("role"))) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        return ResponseEntity.ok(caseWorkerService.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable("id") Long id, HttpSession session) {
        if (session.getAttribute("userId") == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        if (!"caseWorker".equals(session.getAttribute("role"))) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        caseWorkerService.delete(id);
        return ResponseEntity.noContent().build();
    }

}
