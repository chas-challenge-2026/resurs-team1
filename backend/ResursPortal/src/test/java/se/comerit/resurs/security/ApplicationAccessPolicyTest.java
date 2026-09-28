package se.comerit.resurs.security;

import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;
import se.comerit.resurs.dto.CreditApplicationDTO;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class ApplicationAccessPolicyTest {

    private static final String OWN_ORG_NUMBER = "556000-1234";
    private static final String OTHER_ORG_NUMBER = "556000-5678";

    private final ApplicationAccessPolicy policy = new ApplicationAccessPolicy();

    private CreditApplicationDTO applicationFor(String orgNumber) {
        return new CreditApplicationDTO(1L, null, null, null, null, null, null, null,
                null, null, null, orgNumber, null, null);
    }

    @Test
    void checkCanView_shouldAllowCaseWorker() {
        CaseWorkerPrincipal caseWorker = new CaseWorkerPrincipal(1L, "Karin", "karin@resurs.se");

        assertDoesNotThrow(() -> policy.checkCanView(caseWorker, applicationFor(OWN_ORG_NUMBER)));
    }

    @Test
    void checkCanView_shouldAllowCompany_whenApplicationBelongsToCompany() {
        CompanyPrincipal company = new CompanyPrincipal(OWN_ORG_NUMBER, "Fasen Elteknik AB", "750312-1234");

        assertDoesNotThrow(() -> policy.checkCanView(company, applicationFor(OWN_ORG_NUMBER)));
    }

    @Test
    void checkCanView_shouldDenyCompany_whenApplicationBelongsToOtherCompany() {
        CompanyPrincipal company = new CompanyPrincipal(OWN_ORG_NUMBER, "Fasen Elteknik AB", "750312-1234");

        assertThrows(AccessDeniedException.class,
                () -> policy.checkCanView(company, applicationFor(OTHER_ORG_NUMBER)));
    }

    @Test
    void checkCanView_shouldDeny_whenPrincipalIsMissing() {
        assertThrows(AccessDeniedException.class,
                () -> policy.checkCanView(null, applicationFor(OWN_ORG_NUMBER)));
    }
}
