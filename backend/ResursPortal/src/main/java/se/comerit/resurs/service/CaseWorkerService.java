package se.comerit.resurs.service;

import org.springframework.stereotype.Service;
import se.comerit.resurs.dto.caseworker.CaseWorkerResponse;
import se.comerit.resurs.dto.caseworker.CreateCaseWorkerRequest;
import se.comerit.resurs.dto.caseworker.UpdateCaseWorkerRequest;
import se.comerit.resurs.persistence.CaseWorkerRepository;
import se.comerit.resurs.persistence.model.CaseWorker;
import se.comerit.resurs.security.PasswordHasher;

@Service
public class CaseWorkerService {

    private final CaseWorkerRepository caseWorkerRepository;
    private final PasswordHasher passwordHasher;

    public CaseWorkerService(CaseWorkerRepository caseWorkerRepository, PasswordHasher passwordHasher) {
        this.caseWorkerRepository = caseWorkerRepository;
        this.passwordHasher = passwordHasher;
    }

    public CaseWorkerResponse create(CreateCaseWorkerRequest request) {
        if (caseWorkerRepository.findByEmail(request.email()).isPresent()) {
            throw new IllegalArgumentException("Email already in use.");
        }

        CaseWorker caseWorker = new CaseWorker();
        caseWorker.setName(request.name());
        caseWorker.setEmail(request.email());
        caseWorker.setPasswordHash(passwordHasher.md5Hash(request.password()));

        CaseWorker saved = caseWorkerRepository.save(caseWorker);
        return new CaseWorkerResponse(saved);
    }

    public CaseWorkerResponse getById(Long id) {
        CaseWorker caseWorker = caseWorkerRepository.findById(id).orElseThrow();
        return new CaseWorkerResponse(caseWorker);
    }

    public CaseWorkerResponse update(Long id, UpdateCaseWorkerRequest request) {
        CaseWorker caseWorker = caseWorkerRepository.findById(id).orElseThrow();

        caseWorkerRepository.findByEmail(request.email())
                .filter(existing -> !existing.getId().equals(id))
                .ifPresent(existing -> {
                    throw new IllegalArgumentException("Email already in use");
                });

        caseWorker.setName(request.name());
        caseWorker.setEmail(request.email());

        CaseWorker updated = caseWorkerRepository.save(caseWorker);
        return new CaseWorkerResponse(updated);
    }

    public void delete(Long id) {
        caseWorkerRepository.findById(id).orElseThrow();
        caseWorkerRepository.deleteById(id);
    }
}
