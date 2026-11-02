package se.comerit.resurs.dto.application;

import se.comerit.resurs.dto.CreditApplicationDTO;
import se.comerit.resurs.dto.CreditApplicationDataObject;
import se.comerit.resurs.dto.CreditApplicationProtectedDTO;
import se.comerit.resurs.dto.DocumentDTO;
import se.comerit.resurs.persistence.model.CreditApplication;
import se.comerit.resurs.persistence.model.Document;

import java.util.List;

public record ApplicationWithDocumentsDTO(
        CreditApplicationDataObject app,
        List<DocumentDTO> documents) {

    public ApplicationWithDocumentsDTO(
            CreditApplication app,
            List<Document> documents,
            boolean protectedResource) {
        this(
                protectedResource
                        ? new CreditApplicationProtectedDTO(app)
                        : new CreditApplicationDTO(app),
                documents.stream().map(DocumentDTO::new).toList()
        );
    }
}
