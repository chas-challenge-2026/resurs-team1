package se.comerit.resurs.persistence.listener;

import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import se.comerit.resurs.persistence.model.CaseWorker;
import se.comerit.resurs.persistence.model.Company;
import se.comerit.resurs.util.HMACHasher;

public class CaseworkerSearchKeyListener {

    private final HMACHasher hasher;

    public CaseworkerSearchKeyListener(HMACHasher hasher) {
        this.hasher = hasher;
    }

    @PrePersist
    @PreUpdate
    public void updateSearchHash(CaseWorker worker) {
        worker.setSearchKeyEmail(
                hasher.encode(worker.getEmail())
        );
    }
}
