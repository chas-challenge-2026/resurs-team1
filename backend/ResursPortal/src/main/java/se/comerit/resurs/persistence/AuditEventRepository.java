package se.comerit.resurs.persistence;


import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import se.comerit.resurs.persistence.model.AuditEvent;

import java.util.List;

@Repository
public interface AuditEventRepository extends JpaRepository<AuditEvent, Long> {
    List<AuditEvent> findByApplicationIdOrderBySequenceNumberAsc(Long applicationId);

    @Query("select coalesce(max(e.sequenceNumber), 0) from AuditEvent e " +
            "where e.application.id = :applicationId")
    long findMaxSequenceNumber(@Param("applicationId") Long applicationId);
}
