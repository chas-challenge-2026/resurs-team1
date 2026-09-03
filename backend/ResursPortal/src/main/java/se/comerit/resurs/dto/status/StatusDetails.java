package se.comerit.resurs.dto.status;

import se.comerit.resurs.dto.DocumentDTO;
import se.comerit.resurs.enums.ApplicationStatus;

import java.util.List;

public record StatusDetails (
        ApplicationStatusDetails app,
        List<Step> steps,
        ApplicationStatus currentStatus,
        List<DocumentDTO> documents,
        String auditLogRaw
) {
}
