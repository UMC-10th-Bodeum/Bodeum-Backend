package com.bodeum.domain.ai.repository;

import com.bodeum.domain.ai.entity.AiExternalResource;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AiExternalResourceRepository extends JpaRepository<AiExternalResource, Long> {

    List<AiExternalResource> findAllBySourceUrlHashIn(Collection<String> sourceUrlHashes);
}
