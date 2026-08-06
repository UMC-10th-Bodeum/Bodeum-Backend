package com.bodeum.domain.ai.repository;

import com.bodeum.domain.ai.entity.AiExternalDocument;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AiExternalDocumentRepository extends JpaRepository<AiExternalDocument, Long> {

    List<AiExternalDocument> findAllBySourceUrlHashIn(Collection<String> sourceUrlHashes);
}
