package se.comerit.resurs.security;


import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;
import se.comerit.resurs.dto.CreditApplicationDTO;

@Component
public class ApplicationAccessPolicy {


    public void checkCanView(Object principal, CreditApplicationDTO application) {
        boolean allowed = switch (principal) {
            case CaseWorkerPrincipal caseWorker -> true;
            case CompanyPrincipal company -> company.orgNumber().equals(application.orgNumber());
            case null, default -> false;
        };

        if (!allowed) {
            throw new AccessDeniedException("Not allowed to view application " + application.id());
        }
    }

    public void checkCanSubmitFor(CompanyPrincipal principal, String orgNumber) {
        if (!principal.orgNumber().equals(orgNumber)) {
            throw new AccessDeniedException("Not allowed to submit applications for " + orgNumber);
        }
    }
}
