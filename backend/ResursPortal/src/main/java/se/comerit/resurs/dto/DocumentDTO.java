
package se.comerit.resurs.dto;

import se.comerit.resurs.persistence.model.Document;

import java.time.Instant;

public record DocumentDTO(
        Long id,
        Long applicationId,
        String filename,
        String docType,
        Instant uploadedAt
) {

    public DocumentDTO(Document document) {
        this(
                document.getId(),
                document.getApplication().getId(),
                document.getFilename(),
                document.getDoc_type(),
                document.getUploadedAt()
        );
    }
}
