package se.comerit.resurs.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import se.comerit.resurs.persistence.model.Document;

import java.util.List;

@Repository
public interface DocumentRepository extends JpaRepository<Document,Long> {

    List<Document> findByApplicationId(Long applicationId);

}
