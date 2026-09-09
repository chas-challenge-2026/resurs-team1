package se.comerit.resurs.dto.caseworker;

import se.comerit.resurs.persistence.model.CaseWorker;
// Vad API:et returnerar till klienten
public record CaseWorkerResponse(Long id, String name, String email) {

    public CaseWorkerResponse(CaseWorker caseWorker) {
        this(caseWorker.getId(), caseWorker.getName(), caseWorker.getEmail());
    }
}
