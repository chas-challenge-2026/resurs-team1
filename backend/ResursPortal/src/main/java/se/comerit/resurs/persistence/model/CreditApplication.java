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
    @GeneratedValue(strategy = GenerationType.IDENTITY) // Using Internal postgres sequence, not letting JPA control it
    private Long id;

    @ManyToOne
    @JoinColumn(name = "company_id")
    private Company company;

    @Column(name = "requested_amount" )
    private BigDecimal requestedAmount;

    @Column
    private String purpose;

    @Column
    @Enumerated(EnumType.STRING)
    private ApplicationStatus status;

    @Column
    private String decision;

    @Column(name = "decision_reason")
    private String decisionReason;

    @Column(name = "scoring_result")
    private String scoringResult;

    @Column(name = "audit_log")
    private String auditLog = "[]";

    @Column(name = "created_at")
    private LocalDateTime createdAt;
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

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

    public BigDecimal getRequestedAmount() {
        return requestedAmount;
    }

    public void setRequestedAmount(BigDecimal requestedAmount) {
        this.requestedAmount = requestedAmount;
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

    public String getDecisionReason() {
        return decisionReason;
    }

    public void setDecisionReason(String decisionReason) {
        this.decisionReason = decisionReason;
    }

    public String getScoringResult() {
        return scoringResult;
    }

    public void setScoringResult(String scoringResult) {
        this.scoringResult = scoringResult;
    }

    public String getAuditLog() {
        return auditLog;
    }

    public void setAuditLog(String auditLog) {
        this.auditLog = auditLog;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
