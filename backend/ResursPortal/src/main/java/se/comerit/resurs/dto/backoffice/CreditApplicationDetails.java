package se.comerit.resurs.dto.backoffice;

import se.comerit.resurs.dto.CreditApplicationDTO;
import se.comerit.resurs.dto.DocumentDTO;

import java.util.List;

public record CreditApplicationDetails(CreditApplicationDTO application, List<DocumentDTO> documents) {



}
