package se.comerit.resurs.persistence.model;

import jakarta.persistence.*;

@Entity
@Table(name = "companies")
public class Company {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true,name = "org_number" )
    private String orgNumber;

    @Column(name = "company_name")
    private String companyName;

    @Column(name = "authorized_signatory")
    private String authorizedSignatory;


    public Long getId() {
        return id;
    }

    protected void setId(Long id) {
        this.id = id;
    }

    public String getOrg_number() {
        return orgNumber;
    }

    public void setOrg_number(String org_number) {
        this.orgNumber = org_number;
    }

    public String getCompany_name() {
        return companyName;
    }

    public void setCompany_name(String company_name) {
        this.companyName = company_name;
    }

    public String getAuthorized_signatory() {
        return authorizedSignatory;
    }

    public void setAuthorized_signatory(String authorized_signatory) {
        this.authorizedSignatory = authorized_signatory;
    }
}
