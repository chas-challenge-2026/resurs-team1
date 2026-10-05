package se.comerit.resurs.persistence;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import se.comerit.resurs.enums.ApplicationStatus;
import se.comerit.resurs.persistence.model.CreditApplication;
import java.util.List;

/**
 * CreditApplicationRepository -> hämtar och sparar kreditansökningar i databasen
 *
 * Ansvarar för: hämta ansökningar från databasen, t.ex. utifrån status eller företag,
 * och kan hämta dem sida för sida istället för alla på en gång.
 *
 * Inte ansvarig för: att bestämma hur stor en sida får vara eller vad datan används till ->
 * det bestämmer den som anropar den (BackofficeService m.fl.).
 *
 */

@Repository
public interface CreditApplicationRepository extends JpaRepository<CreditApplication,Long> {

    Page<CreditApplication> findByStatusOrderByCreatedAtAsc(ApplicationStatus status, Pageable pageable);

    //Allows for a multiple statuses and limits with pagable
    Page<CreditApplication> findByStatusInOrderByCreatedAtAsc(
            List<ApplicationStatus> statuses,
            Pageable pageable
    );

    List<CreditApplication> findByCompanyId(Long companyID);
    List<CreditApplication> findByCompanyIdOrderByCreatedAtDesc(Long companyID, Pageable pagable);
    List<CreditApplication> findByCompany_OrgNumberOrderByCreatedAtDesc(String orgNumber, Pageable pageable);

}
