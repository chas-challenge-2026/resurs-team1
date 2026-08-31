package se.comerit.resurs.persistence.model;

import jakarta.persistence.*;

@Entity
@Table(name = "companies")
public class Company {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true )
    private String org_number;

    @Column
    private String company_name;

    @Column
    private String authorized_signatory;


    public Long getId() {
        return id;
    }

    protected void setId(Long id) {
        this.id = id;
    }

    public String getOrg_number() {
        return org_number;
    }

    public void setOrg_number(String org_number) {
        this.org_number = org_number;
    }

    public String getCompany_name() {
        return company_name;
    }

    public void setCompany_name(String company_name) {
        this.company_name = company_name;
    }

    public String getAuthorized_signatory() {
        return authorized_signatory;
    }

    public void setAuthorized_signatory(String authorized_signatory) {
        this.authorized_signatory = authorized_signatory;
    }
}
