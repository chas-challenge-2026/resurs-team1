package se.comerit.resurs.persistence;


import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.stereotype.Repository;
import se.comerit.resurs.enums.AuditAction;
import se.comerit.resurs.persistence.model.AuditEvent;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface AuditEventRepository extends JpaRepository<AuditEvent, Long> {
    List<AuditEvent> findByApplicationIdOrderBySequenceNumberAsc(Long applicationId);


    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<AuditEvent> findFirstByApplicationIdOrderBySequenceNumberDesc(
            Long applicationId);

    // Kollar om ärendet har en audit-händelse av en viss typ, t.ex MANUAL_DECISION
    // Spring Data skapar frågan själv utifrån metodnamnet så ingen SQL behövs.
    boolean existsByApplicationIdAndAction(Long applicationId, AuditAction action);

    List<AuditEvent> findByApplicationIdAndActionInOrderBySequenceNumberAsc(
            Long applicationId, Collection<AuditAction> actions);

    //Auditloggen för ett ärende, men bara om ärendet inte är skyddat (annars tomt)
    List<AuditEvent> findByApplicationIdAndApplicationProtectedResourceFalseOrderBySequenceNumberAsc(
            Long applicationId);

}
