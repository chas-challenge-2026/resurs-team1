package se.comerit.resurs.service;

import jakarta.transaction.Transactional;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import se.comerit.resurs.dto.caseworker.CaseWorkerResponse;
import se.comerit.resurs.dto.caseworker.CreateCaseWorkerRequest;
import se.comerit.resurs.dto.caseworker.UpdateCaseWorkerRequest;
import se.comerit.resurs.exception.EmailAlreadyInUseException;
import se.comerit.resurs.persistence.CaseWorkerRepository;
import se.comerit.resurs.persistence.RoleRepository;
import se.comerit.resurs.persistence.model.CaseWorker;
import se.comerit.resurs.persistence.model.Role;
import se.comerit.resurs.util.HMACHasher;

@Service
public class CaseWorkerService {

    private final CaseWorkerRepository caseWorkerRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final HMACHasher hasher;


    public CaseWorkerService(CaseWorkerRepository caseWorkerRepository, RoleRepository roleRepository, PasswordEncoder passwordEncoder, HMACHasher hasher) {
        this.caseWorkerRepository = caseWorkerRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
        this.hasher = hasher;
    }

    public CaseWorkerResponse create(CreateCaseWorkerRequest request) {
        if (caseWorkerRepository.findBySearchKeyEmail(hasher.encode(request.email())).isPresent()) {
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

    //No transactional dirtychecking here in order to handle the exceptions on the save()
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

    @Transactional
    public void giveRole(String email, String roleName){

        Role roleToAdd = roleRepository.findByName(roleName).orElseThrow();
        CaseWorker recipient = caseWorkerRepository.findBySearchKeyEmail(hasher.encode(email)).orElseThrow();
        recipient.getRoles().add(roleToAdd);
    }

}
