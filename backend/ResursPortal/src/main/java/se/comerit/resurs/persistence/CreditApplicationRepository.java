package se.comerit.resurs.persistence;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import se.comerit.resurs.enums.ApplicationStatus;
import se.comerit.resurs.persistence.model.CreditApplication;

import java.util.List;

@Repository
public interface CreditApplicationRepository extends JpaRepository<CreditApplication,Long> {

    List<CreditApplication> findByStatusOrderByCreated_atAsc(ApplicationStatus status);

    //Allows for a multiple statuses and limits with pagable
    List<CreditApplication> findByStatusInOrderByCreated_atAsc(
            List<ApplicationStatus> statuses,
            Pageable pageable
    );


}
