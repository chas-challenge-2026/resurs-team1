package se.comerit.resurs.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import se.comerit.resurs.model.Document;

import java.util.List;

public interface DocumentRepository extends JpaRepository<Document, Long> {
    List<Document> findByApplicationIdOrderByUploadedAtDesc(Long applicationId);
}
