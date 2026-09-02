package se.comerit.resurs.model;
import jakarta.persistence.*;
import org.hibernate.annotations.DynamicInsert;

import java.time.LocalDateTime;

@Entity
@Table(name = "documents")
@DynamicInsert
public class Document {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "application_id")
    private CreditApplication application;

    @Column
    private String filename;

    @Column(name = "doc_type")
    private String docType;

    @Column(name = "uploaded_at")
    private LocalDateTime uploadedAt;

    public Document(){}

    public Document(CreditApplication application, String filename, String docType, LocalDateTime uploadedAt) {
        this.application = application;
        this.filename = filename;
        this.docType = docType;
        this.uploadedAt = uploadedAt;
    }

    public Long getId() {return id;}
    public void setId(Long id) {this.id = id;}

    public CreditApplication getApplication() {return application;}
    public void setApplication(CreditApplication application) {this.application = application;}

    public String getFilename() {return filename;}
    public void setFilename(String filename) {this.filename = filename;}

    public String getDocType() {return docType;}
    public void setDocType(String docType) {this.docType = docType;}

    public LocalDateTime getUploadedAt() {return uploadedAt;}
    public void setUploadedAt(LocalDateTime uploadedAt) {this.uploadedAt = uploadedAt;}
}
