package se.comerit.resurs.controller;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import se.comerit.resurs.dto.caseworker.CaseWorkerResponse;
import se.comerit.resurs.dto.caseworker.CreateCaseWorkerRequest;
import se.comerit.resurs.dto.caseworker.UpdateCaseWorkerRequest;
import se.comerit.resurs.service.CaseWorkerService;
/**
 * TODO: We need to create a Admin role that manages CRUD on caseworkers, a casual caseworker should not have this
 *  permission.
 * */
@PreAuthorize("hasRole('CASE_WORKER')")
@RestController
@RequestMapping("/api/caseworkers")
public class CaseWorkerController {
    private final CaseWorkerService caseWorkerService;

    public CaseWorkerController(CaseWorkerService caseWorkerService) {
        this.caseWorkerService = caseWorkerService;
    }

    @PostMapping
    public ResponseEntity<CaseWorkerResponse> create(@Valid @RequestBody CreateCaseWorkerRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(caseWorkerService.create(request));
    }

    @GetMapping("/{id}")
    public ResponseEntity<CaseWorkerResponse> getById(@PathVariable("id") Long id) {
            return ResponseEntity.ok(caseWorkerService.getById(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<CaseWorkerResponse> update(@PathVariable("id") Long id, @Valid @RequestBody UpdateCaseWorkerRequest request) {
        return ResponseEntity.ok(caseWorkerService.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable("id") Long id) {
        caseWorkerService.delete(id);
        return ResponseEntity.noContent().build();
    }

}
