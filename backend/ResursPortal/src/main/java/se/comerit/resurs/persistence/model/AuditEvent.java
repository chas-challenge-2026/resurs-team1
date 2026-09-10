package se.comerit.resurs.persistence.model;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import se.comerit.resurs.enums.AuditAction;


import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "audit_events", uniqueConstraints = @UniqueConstraint(columnNames = {"application_id", "sequence_number"}))
public class AuditEvent {

    @Id
    @Column(name = "event_id", nullable = false, updatable = false)
    private UUID eventId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "application_id")
    private CreditApplication application;

    @Column(name = "sequence_number", nullable = false)
    private long sequenceNumber;

    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt;

    @Column(name = "action", nullable = false, length = 50)
    @Enumerated(EnumType.STRING)
    private AuditAction action;

    @Column(name = "actor", nullable = false, length = 100)
    private String actor;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "data", columnDefinition = "jsonb")
    private String data;

    @Column(name = "previous_hash", length = 64)
    private String previousHash;

    @Column(name = "current_hash", length = 64)
    private String currentHash;

    private static final int SCHEMA_VERSION = 1;

    @Column(name = "schema_version", nullable = false)
    private int schemaVersion;

    @Column(name = "signing_key_id", length = 64)
    private String signingKeyId;

    @Column(name = "signature", length = 512)
    private String signature;

    protected AuditEvent() {
    }



    public AuditEvent(CreditApplication application, long sequenceNumber, AuditAction action,
                      String actor, String data,
                      String previousHash, String currentHash, String signingKeyId, String signature) {
        this.eventId = UUID.randomUUID();
        this.application = application;
        this.sequenceNumber = sequenceNumber;
        this.action = action;
        this.actor = actor;
        this.data = data;
        this.schemaVersion = SCHEMA_VERSION ;
        this.previousHash = previousHash;
        this.currentHash = currentHash;
        this.signingKeyId = signingKeyId;
        this.signature = signature;
        this.occurredAt = Instant.now();
    }

    public UUID getEventId() { return eventId; }
    public CreditApplication getApplication() { return application; }
    public long getSequenceNumber() { return sequenceNumber; }
    public Instant getOccurredAt() { return occurredAt; }
    public AuditAction getAction() { return action; }
    public String getActor() { return actor; }
    public String getData() { return data; }
    public String getPreviousHash() { return previousHash; }
    public String getCurrentHash() {return currentHash;}
    public int getSchemaVersion() { return schemaVersion; }
    public String getSigningKeyId() { return signingKeyId; }
    public String getSignature() { return signature; }
}


