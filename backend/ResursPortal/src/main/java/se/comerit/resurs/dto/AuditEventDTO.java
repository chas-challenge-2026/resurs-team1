package se.comerit.resurs.dto;

import se.comerit.resurs.enums.AuditAction;
import se.comerit.resurs.persistence.model.AuditEvent;
import tools.jackson.databind.JsonNode;

import java.time.Instant;
import java.util.UUID;

public record AuditEventDTO(UUID eventId,
                            Long applicationId,
                            long sequenceNumber,
                            Instant occurredAt,
                            AuditAction action,
                            String actor,
                            JsonNode data,
                            byte[] previousHash,
                            byte[] currentHash,
                            int schemaVersion,
                            String signingKeyId,
                            byte[] signature) {
    public AuditEventDTO(AuditEvent event) {
        this(event.getEventId(), event.getApplication().getId(), event.getSequenceNumber(), event.getOccurredAt(),
                event.getAction(), event.getActor(), event.getData(), event.getPreviousHash(),event.getCurrentHash(), event.getSchemaVersion(), event.getSigningKeyId(), event.getSignature());
    }

}
