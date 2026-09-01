package se.comerit.resurs.persistence.model;

import jakarta.persistence.*;
import org.hibernate.annotations.DynamicInsert;

import java.time.LocalDate;
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
        return doc_type;
    }

    public void setDoc_type(String doc_type) {
        this.doc_type = doc_type;
    }

    public LocalDateTime getUploaded_at() {
        return uploaded_at;
    }

    public void setUploaded_at(LocalDateTime uploaded_at) {
        this.uploaded_at = uploaded_at;
    }

    @OneToMany
    @JoinColumn(name = "application_id")
    private CreditApplication application;

    @Column
    private String filename;

    @Column
    private String doc_type;

    @Column
    private LocalDateTime uploaded_at;

}
