package se.comerit.resurs.persistence.listener;

import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import se.comerit.resurs.persistence.model.ApplicationSignature;
import se.comerit.resurs.persistence.model.Signatory;
import se.comerit.resurs.util.HMACHasher;

public class SignatureSearchKeyListener {

    private final HMACHasher hasher;

    public SignatureSearchKeyListener(HMACHasher hasher) {
        this.hasher = hasher;
    }

    @PrePersist
    @PreUpdate
    public void updateSearchToHash(Signatory signature){
        signature.setSearchKeyPersonalNumber(hasher.encode(signature.getPersonalNumber()));
    }
}
