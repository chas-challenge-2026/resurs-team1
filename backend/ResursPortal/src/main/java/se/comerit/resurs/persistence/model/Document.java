package se.comerit.resurs.persistence.model;

import jakarta.persistence.*;
import org.hibernate.annotations.DynamicInsert;

import java.time.LocalDateTime;

@Entity
@Table(name = "documents")
@DynamicInsert // stops hibernate from overriding DB Default values.
public class Document {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    public Long getId() {
        return id;
    }

    protected void setId(Long id) {
        this.id = id;
    }

    public CreditApplication getApplication() {
        return application;
    }

    public void setApplication(CreditApplication application) {
        this.application = application;
    }

    public String getFilename() {
        return filename;
    }

    public void setFilename(String filename) {
        this.filename = filename;
    }

    public String getDoc_type() {
        return docType;
    }

    public void setDoc_type(String doc_type) {
        this.docType = doc_type;
    }

    public LocalDateTime getUploadedAt() {
        return uploadedAt;
    }

    public void setUploadedAt(LocalDateTime uploadedAt) {
        this.uploadedAt = uploadedAt;
    }

    @ManyToOne
    @JoinColumn(name = "application_id")
    private CreditApplication application;

    @Column
    private String filename;

    @Column(name = "doc_type")
    private String docType;

    @Column(name = "uploaded_at")
    private LocalDateTime uploadedAt;

}
