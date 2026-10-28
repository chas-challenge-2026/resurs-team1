package se.comerit.resurs.persistence.model;

import jakarta.persistence.*;
import se.comerit.resurs.persistence.converter.PiiAttributeConverter;
import se.comerit.resurs.persistence.listener.CaseworkerSearchKeyListener;

import java.util.HashSet;
import java.util.Set;

@Entity
@EntityListeners(CaseworkerSearchKeyListener.class)
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


    @Column(name = "search_key_email", unique = true)
    private byte[] searchKeyEmail;

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
            name = "case_worker_roles",
            joinColumns = @JoinColumn(name = "worker_id"),
            inverseJoinColumns = @JoinColumn(name = "role_id")
    )
    private Set<Role> roles = new HashSet<>();
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

    public void setSearchKeyEmail(byte[] searchKeyEmail) {
        this.searchKeyEmail = searchKeyEmail;
    }

    public Set<Role> getRoles() {
        return roles;
    }
}
