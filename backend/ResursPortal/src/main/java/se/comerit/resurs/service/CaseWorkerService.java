package se.comerit.resurs.service;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import se.comerit.resurs.dto.caseworker.CaseWorkerResponse;
import se.comerit.resurs.dto.caseworker.CreateCaseWorkerRequest;
import se.comerit.resurs.dto.caseworker.UpdateCaseWorkerRequest;
import se.comerit.resurs.exception.EmailAlreadyInUseException;
import se.comerit.resurs.persistence.CaseWorkerRepository;
import se.comerit.resurs.persistence.model.CaseWorker;

@Service
public class CaseWorkerService {

    private final CaseWorkerRepository caseWorkerRepository;
    private final PasswordEncoder passwordEncoder;


    public CaseWorkerService(CaseWorkerRepository caseWorkerRepository, PasswordEncoder passwordEncoder) {
        this.caseWorkerRepository = caseWorkerRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public CaseWorkerResponse create(CreateCaseWorkerRequest request) {
        if (caseWorkerRepository.findByEmail(request.email()).isPresent()) {
            throw new IllegalArgumentException("Email already in use.");
        }

        CaseWorker caseWorker = new CaseWorker();
        caseWorker.setName(request.name());
        caseWorker.setEmail(request.email());
        caseWorker.setPasswordHash(passwordEncoder.encode(request.password()));

        CaseWorker saved = caseWorkerRepository.save(caseWorker);
        return new CaseWorkerResponse(saved);
    }

    public CaseWorkerResponse getById(Long id) {
        CaseWorker caseWorker = caseWorkerRepository.findById(id).orElseThrow();
        return new CaseWorkerResponse(caseWorker);
    }

    public CaseWorkerResponse update(Long id, UpdateCaseWorkerRequest request) {
        CaseWorker caseWorker = caseWorkerRepository.findById(id).orElseThrow();


        caseWorker.setName(request.name());
        caseWorker.setEmail(request.email());

        CaseWorker updated;
        try{
            updated = caseWorkerRepository.save(caseWorker);
        } catch (DataIntegrityViolationException ex){
            throw new EmailAlreadyInUseException("Email already in use");
        }

        return new CaseWorkerResponse(updated);
    }

    public void delete(Long id) {
        caseWorkerRepository.findById(id).orElseThrow();
        caseWorkerRepository.deleteById(id);
    }
}
