package se.comerit.resurs.persistence.model;

import jakarta.persistence.*;
import se.comerit.resurs.persistence.converter.PiiAttributeConverter;

@Entity
@Table(name = "companies")
public class Company {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Version
    private Integer version;

    @Column(unique = true,name = "org_number" )
    @Convert(converter = PiiAttributeConverter.class)
    private String orgNumber;

    @Column(name = "companyName")
    @Convert(converter = PiiAttributeConverter.class)
    private String companyName;


    @Column(name = "authorized_signatory")
    @Convert(converter = PiiAttributeConverter.class)
    private String authorizedSignatory;

    public Company() {

    }

    public Company(String orgNumber, String companyName, String authorizedSignatory) {
        this.orgNumber = orgNumber;
        this.companyName = companyName;
        this.authorizedSignatory = authorizedSignatory;
    }


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
