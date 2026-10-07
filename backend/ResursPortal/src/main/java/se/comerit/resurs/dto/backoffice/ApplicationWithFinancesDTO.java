package se.comerit.resurs.dto.backoffice;

import se.comerit.resurs.dto.companyvalidation.CompanyFinancialApiDTO;

public record ApplicationWithFinancesDTO (
        CreditApplicationDetails appDetails, CompanyFinancialApiDTO companyFinances
){
}
