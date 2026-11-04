package se.comerit.resurs.persistence.model;

import jakarta.persistence.*;
import se.comerit.resurs.persistence.converter.PiiAttributeConverter;

import java.time.Instant;

@Entity
@Table(name = "application_signatures",
        uniqueConstraints = @UniqueConstraint(columnNames = {"application_id", "signatory_id"}))
public class ApplicationSignature {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "application_id")
    private CreditApplication application;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "signatory_id")
    private Signatory signatory;

    @Column(name = "signer_name", nullable = false)
    @Convert(converter = PiiAttributeConverter.class)
    private String signerName;

    @Column(name = "signed_at", nullable = false)
    private Instant signedAt;

    protected ApplicationSignature(){
    }
    public ApplicationSignature(CreditApplication application, Signatory signatory, String signerName) {
        this.application = application;
        this.signatory = signatory;
        this.signerName = signerName;
        this.signedAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public CreditApplication getApplication() {
        return application;
    }

    public String getSignerName() {
        return signerName;
    }

    public Instant getSignedAt() {
        return signedAt;
    }


}
