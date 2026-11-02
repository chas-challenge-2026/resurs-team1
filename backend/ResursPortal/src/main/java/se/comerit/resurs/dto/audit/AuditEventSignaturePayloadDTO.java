package se.comerit.resurs.dto.audit;


import se.comerit.resurs.persistence.model.AuditEvent;
import tools.jackson.databind.JsonNode;

import java.time.Instant;
import java.util.UUID;

public record AuditEventSignaturePayloadDTO(
        int schemaVersion,
        UUID eventId,
        long applicationId,
        long sequenceNumber,
        Instant occurredAt,
        String action,
        String actor,
        JsonNode data,
        byte[] previousHash
) {

    public AuditEventSignaturePayloadDTO(AuditEvent event){
        this(
                event.getSchemaVersion(),
                event.getEventId(),
                event.getApplication().getId(),
                event.getSequenceNumber(),
                event.getOccurredAt(),
                event.getAction().name(),
                event.getActor(),
                event.getData(),
                event.getPreviousHash()
                );
    }

}
