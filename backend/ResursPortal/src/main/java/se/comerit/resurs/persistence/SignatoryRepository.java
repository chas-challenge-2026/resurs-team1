package se.comerit.resurs.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import se.comerit.resurs.persistence.model.Signatory;

import java.util.Optional;

@Repository
public interface SignatoryRepository extends JpaRepository<Signatory, Long> {
    Optional<Signatory> findBySearchKeyPersonalNumber(byte[] searchKeyPersonalNumber);
}
