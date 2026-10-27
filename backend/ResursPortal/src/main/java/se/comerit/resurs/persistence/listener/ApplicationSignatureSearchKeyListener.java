package se.comerit.resurs.persistence.listener;

import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import se.comerit.resurs.persistence.model.ApplicationSignature;
import se.comerit.resurs.util.HMACHasher;

public class ApplicationSignatureSearchKeyListener {

    private final HMACHasher hasher;

    public ApplicationSignatureSearchKeyListener(HMACHasher hasher) {
        this.hasher = hasher;
    }

    @PrePersist
    @PreUpdate
    public void updateSearchToHash(ApplicationSignature signature){
        signature.setSearchKeyPersonalNumber(hasher.encode(signature.getPersonalNumber()));
    }
}
