package se.comerit.resurs.persistence.model;

import jakarta.persistence.*;
import org.hibernate.annotations.DynamicInsert;
import se.comerit.resurs.enums.ApplicationStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "applications")
@DynamicInsert // stops hibernate from overriding DB Default values.
public class CreditApplication {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "company_id")
    private Company company;

    @Column
    private BigDecimal requested_amount;

    @Column
    private String purpose;

    @Column
    private ApplicationStatus status;

    @Column
    private String decision;

    @Column
    private String decision_reason;

    @Column
    private String scoring_result;

    @Column
    private String audit_log = "[]";

    @Column
    private LocalDateTime created_at;
    @Column
    private LocalDateTime updated_at;

    //Need version field here for Optimistic Locking


    public Long getId() {
        return id;
    }

    protected void setId(Long id) {
        this.id = id;
    }

    public Company getCompany() {
        return company;
    }

    public void setCompany(Company company) {
        this.company = company;
    }

    public BigDecimal getRequested_amount() {
        return requested_amount;
    }

    public void setRequested_amount(BigDecimal requested_amount) {
        this.requested_amount = requested_amount;
    }

    public String getPurpose() {
        return purpose;
    }

    public void setPurpose(String purpose) {
        this.purpose = purpose;
    }

    public ApplicationStatus getStatus() {
        return status;
    }

    public void setStatus(ApplicationStatus status) {
        this.status = status;
    }

    public String getDecision() {
        return decision;
    }

    public void setDecision(String decision) {
        this.decision = decision;
    }

    public String getDecision_reason() {
        return decision_reason;
    }

    public void setDecision_reason(String decision_reason) {
        this.decision_reason = decision_reason;
    }

    public String getScoring_result() {
        return scoring_result;
    }

    public void setScoring_result(String scoring_result) {
        this.scoring_result = scoring_result;
    }

    public String getAudit_log() {
        return audit_log;
    }

    public void setAudit_log(String audit_log) {
        this.audit_log = audit_log;
    }

    public LocalDateTime getCreated_at() {
        return created_at;
    }

    public void setCreated_at(LocalDateTime created_at) {
        this.created_at = created_at;
    }

    public LocalDateTime getUpdated_at() {
        return updated_at;
    }

    public void setUpdated_at(LocalDateTime updated_at) {
        this.updated_at = updated_at;
    }
}
