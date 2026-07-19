package com.bodeum.domain.ai.repository;

import com.bodeum.domain.ai.entity.AiExternalResource;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AiExternalResourceRepository extends JpaRepository<AiExternalResource, Long> {

    Optional<AiExternalResource> findBySourceUrlHash(String sourceUrlHash);
}
