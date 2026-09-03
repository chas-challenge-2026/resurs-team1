package se.comerit.resurs.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import se.comerit.resurs.persistence.model.Company;

import java.util.Optional;

@Repository
public interface CompanyRepository extends JpaRepository<Company,Long> {
    Optional<Company> findByOrgNumber(String orgNumber);
}
