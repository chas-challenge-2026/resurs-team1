package se.comerit.resurs.dto.application;

import se.comerit.resurs.dto.CreditApplicationDTO;
import se.comerit.resurs.dto.DocumentDTO;
import se.comerit.resurs.persistence.model.CreditApplication;
import se.comerit.resurs.persistence.model.Document;

import java.util.List;

public record ApplicationWithDocumentsDTO(CreditApplicationDTO app , List<DocumentDTO> documents) {

    public ApplicationWithDocumentsDTO(CreditApplication app, List<Document> documents){
        this(new CreditApplicationDTO(app),documents.stream().map(DocumentDTO::new).toList());
    }


}
