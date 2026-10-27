package se.comerit.resurs.persistence.model;

import jakarta.persistence.*;
import jakarta.persistence.criteria.CriteriaBuilder;
import se.comerit.resurs.persistence.converter.PiiAttributeConverter;
import se.comerit.resurs.persistence.listener.ApplicationSignatureSearchKeyListener;

import java.time.Instant;

@Entity
@EntityListeners(ApplicationSignatureSearchKeyListener.class)
@Table(name = "application_signatures",
        uniqueConstraints = @UniqueConstraint(columnNames = {"application_id", "search_key_personal_number"}))
public class ApplicationSignature {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "application_id")
    private CreditApplication application;

    @Column(name = "personal_number", nullable = false)
    @Convert(converter = PiiAttributeConverter.class)
    private String personalNumber;

    @Column(name = "search_key_personal_number", nullable = false)
    private byte[]searchKeyPersonalNumber;

    @Column(name = "signer_name", nullable = false)
    @Convert(converter = PiiAttributeConverter.class)
    private String signerName;

    @Column(name = "signed_at", nullable = false)
    private Instant signedAt;

    protected ApplicationSignature(){
    }
    public ApplicationSignature(CreditApplication application, String personalNumber, String signerName) {
        this.application = application;
        this.personalNumber = personalNumber;
        this.signerName = signerName;
        this.signedAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public CreditApplication getApplication() {
        return application;
    }

    public String getPersonalNumber() {
        return personalNumber;
    }

    public void setSearchKeyPersonalNumber(byte[] searchKeyPersonalNumber) {
        this.searchKeyPersonalNumber = searchKeyPersonalNumber;
    }

    public String getSignerName() {
        return signerName;
    }

    public Instant getSignedAt() {
        return signedAt;
    }


}
