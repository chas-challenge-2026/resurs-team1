package se.comerit.resurs.persistence;


import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import se.comerit.resurs.persistence.model.ApplicationSignature;

import java.util.List;

@Repository
public interface ApplicationSignatureRepository extends JpaRepository<ApplicationSignature, Long> {
    List<ApplicationSignature> findByApplicationId(Long applicationId);
    boolean existsByApplicationIdAndSearchKeyPersonalNumber(Long applicationId, byte[] searchKeyPersonalNumber);
    long countByApplicationId(Long applicationId);

}
