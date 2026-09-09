package se.comerit.resurs.dto;

import com.fasterxml.jackson.annotation.JsonRawValue;
import se.comerit.resurs.enums.AuditAction;
import se.comerit.resurs.persistence.model.AuditEvent;

import java.time.Instant;
import java.util.UUID;

public record AuditEventDTO(UUID eventId,
                            Long applicationId,
                            long sequenceNumber,
                            Instant occurredAt,
                            AuditAction action,
                            String actor,
                            @JsonRawValue String data,
                            String previousHash,
                            int schemaVersion,
                            String signingKeyId,
                            String signature) {
    public AuditEventDTO(AuditEvent event) {
        this(event.getEventId(), event.getApplication().getId(), event.getSequenceNumber(), event.getOccurredAt(),
                event.getAction(), event.getActor(), event.getData(), event.getPreviousHash(), event.getSchemaVersion(), event.getSigningKeyId(), event.getSignature());
    }

}
