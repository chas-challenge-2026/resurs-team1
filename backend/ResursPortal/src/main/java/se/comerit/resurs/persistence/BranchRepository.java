package se.comerit.resurs.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import se.comerit.resurs.persistence.model.Branch;

import java.util.Optional;

@Repository
public interface BranchRepository extends JpaRepository<Branch,Long> {

    Optional<Branch> findByBranchName(String branchName);

}
