
package se.comerit.resurs.dto;

import se.comerit.resurs.persistence.model.Document;

import java.time.LocalDateTime;

public record DocumentDTO(
        Long id,
        Long applicationId,
        String filename,
        String docType,
        LocalDateTime uploadedAt
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
