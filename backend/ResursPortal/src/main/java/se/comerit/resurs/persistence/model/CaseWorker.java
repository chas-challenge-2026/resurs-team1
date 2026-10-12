package se.comerit.resurs.persistence.model;

import jakarta.persistence.*;
import se.comerit.resurs.persistence.converter.PiiAttributeConverter;

@Entity
@Table(name = "case_workers")
public class CaseWorker {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Version
    private Integer version;

    @Column(name = "name")
    @Convert(converter = PiiAttributeConverter.class)
    private String name;

    @Column(name = "email", unique = true)
    @Convert(converter = PiiAttributeConverter.class)
    private String email;

    @Column(name = "password_hash")
    private String passwordHash;

    public CaseWorker() {

    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }
}
