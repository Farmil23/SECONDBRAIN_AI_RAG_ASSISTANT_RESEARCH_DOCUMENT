package com.secondbrain.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface DocumentRepository extends JpaRepository<DocumentEntity, Long> {
    Optional<DocumentEntity> findByFilename(String filename);

    // Fetch only documents owned by the specific user
    java.util.List<DocumentEntity> findAllByUserId(Long userId);
}
