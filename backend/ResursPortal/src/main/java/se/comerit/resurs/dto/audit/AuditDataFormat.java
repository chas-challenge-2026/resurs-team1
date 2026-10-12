package se.comerit.resurs.dto.audit;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import se.comerit.resurs.enums.ApplicationStatus;

/**
 * AuditDataFormat -> formatet på det som sparas i fältet "data" på en audit-händelse
 *
 * Innehåller en record per händelsetyp ApplicationCreated, ScoringRun, DocumentUploaded, ManualDecision och CommentAdded.
 * Jackson gör om dem till JSON så text med citattecken eller radbrytningar sparas ändå. CommentEntry används bara
 * när kommentarer läses tillbaka.
 *
 * Representerar bara data -> ingen logik, de fylls i och sparas av AuditService
 *
 */

public final class AuditDataFormat {

    private AuditDataFormat(){}

    public record ApplicationCreated(String actorType, String purpose) {}

    public record ScoringRun(String actorType, String decision, ApplicationStatus newStatus, int flags) {}

    public record DocumentUploaded(String actorType, String filename, String docType) {}

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record ManualDecision(String actorType, String workerName,
                                 ApplicationStatus previousStatus,
                                 ApplicationStatus newStatus, String comment) {}

    public record CommentAdded(String actorType, String workerName, String comment) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record CommentEntry(String workerName, String comment) {}
}
