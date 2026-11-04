package se.comerit.resurs.security;


import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import se.comerit.resurs.dto.CreditApplicationDTO;
import se.comerit.resurs.dto.CreditApplicationDataObject;
import se.comerit.resurs.persistence.model.CreditApplication;

import java.util.NoSuchElementException;

@Component
public class ApplicationAccessPolicy {


    public void checkCanView(Object principal, CreditApplicationDataObject application) {
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

    public boolean canViewSensitiveData(String orgNR) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null) {
            return false;
        }

        var authorities = authentication.getAuthorities();

        if (authorities.stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_CERTIFIED"))) {
            return true;
        }

        return authorities.stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_COMPANY"))
                && authentication.getPrincipal() instanceof CompanyPrincipal company
                && orgNR.equals(company.orgNumber());
    }

    //same as above but caseworker only
    public boolean caseworkerHasSpecialAccess(){
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) {
            return false;
        }

        var authorities = authentication.getAuthorities();

        if (authorities.stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_CERTIFIED"))) {
            return true;
        } else return false;

    }


    public boolean caseworkerCanReadResource(CreditApplication application){
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) {
            return false;
        }
        if(!application.isProtectedResource()){
            return true;
        }

        var authorities = authentication.getAuthorities();

        if (authorities.stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_CERTIFIED"))) {
            return true;
        } else return false;


    }


}
