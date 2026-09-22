package se.comerit.resurs.persistence;


import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import se.comerit.resurs.persistence.model.AuditEvent;

import java.util.List;
import java.util.Optional;

@Repository
public interface AuditEventRepository extends JpaRepository<AuditEvent, Long> {
    List<AuditEvent> findByApplicationIdOrderBySequenceNumberAsc(Long applicationId);


    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<AuditEvent> findFirstByApplicationIdOrderBySequenceNumberDesc(
            Long applicationId);

}
