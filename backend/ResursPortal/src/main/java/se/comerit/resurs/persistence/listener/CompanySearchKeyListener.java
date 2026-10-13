package se.comerit.resurs.persistence.listener;

import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import se.comerit.resurs.persistence.model.Company;
import se.comerit.resurs.util.HMACHasher;

public class CompanySearchKeyListener {

    private final HMACHasher hasher;

    public CompanySearchKeyListener(HMACHasher hasher) {
        this.hasher = hasher;
    }

    @PrePersist
    @PreUpdate
    public void updateSearchHash(Company company) {
        company.setSearchKeyOrgNumber(
                hasher.encode(company.getOrg_number())
        );
    }
}
