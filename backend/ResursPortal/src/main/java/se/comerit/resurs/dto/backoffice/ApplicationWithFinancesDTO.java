package se.comerit.resurs.dto.backoffice;

import se.comerit.resurs.dto.application.ApplicationWithDocumentsDTO;
import se.comerit.resurs.dto.companyvalidation.CompanyFinancialApiDTO;

public record ApplicationWithFinancesDTO (
        ApplicationWithDocumentsDTO appDetails, CompanyFinancialApiDTO companyFinances
){
}
