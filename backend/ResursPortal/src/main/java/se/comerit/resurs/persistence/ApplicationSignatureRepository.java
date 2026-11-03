package se.comerit.resurs.persistence;


import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import se.comerit.resurs.persistence.model.ApplicationSignature;
import se.comerit.resurs.persistence.model.Signatory;

import java.util.List;

@Repository
public interface ApplicationSignatureRepository extends JpaRepository<ApplicationSignature, Long> {
    List<ApplicationSignature> findByApplicationId(Long applicationId);
    boolean existsByApplicationIdAndSignatoryId(Long applicationId, Long signatoryId);
    long countByApplicationId(Long applicationId);

}
