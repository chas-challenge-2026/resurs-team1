package se.comerit.resurs.persistence.model;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.DynamicInsert;
import org.hibernate.annotations.UpdateTimestamp;
import se.comerit.resurs.enums.ApplicationStatus;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "applications")
@DynamicInsert // stops hibernate from overriding DB Default values.
public class CreditApplication {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY) // Using Internal postgres sequence, not letting JPA control it
    private Long id;

    @Version
    private Integer version;

    @ManyToOne
    @JoinColumn(name = "company_id")
    private Company company;

    @Column(name = "requestedAmount" )
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

    @Column
    private Integer durationMonths;

    @CreationTimestamp
    @Column(name = "createdAt")
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private Instant updatedAt;

    @Column
    private String contactName;

    @Column
    private String contactNumber;

    @Column
    private String contactEmail;

    @Column(name = "comment")
    private String comment;


    public Integer getDurationMonths() {
        return durationMonths;
    }

    public void setDurationMonths(Integer durationMonths) {
        this.durationMonths = durationMonths;
    }

    public String getContactName() {
        return contactName;
    }

    public void setContactName(String contactName) {
        this.contactName = contactName;
    }

    public String getContactNumber() {
        return contactNumber;
    }

    public void setContactNumber(String contactNumber) {
        this.contactNumber = contactNumber;
    }

    public String getContactEmail() {
        return contactEmail;
    }

    public void setContactEmail(String contactEmail) {
        this.contactEmail = contactEmail;
    }

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



    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }
}
