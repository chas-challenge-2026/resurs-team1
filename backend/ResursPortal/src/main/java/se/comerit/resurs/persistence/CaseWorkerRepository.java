package se.comerit.resurs.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import se.comerit.resurs.persistence.model.CaseWorker;

import java.util.Optional;

public interface CaseWorkerRepository extends JpaRepository<CaseWorker, Long> {
    Optional<CaseWorker> findByEmail(String email);
}
