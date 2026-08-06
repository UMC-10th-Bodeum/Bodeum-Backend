package com.bodeum.domain.ai.repository;

import com.bodeum.domain.ai.entity.AiExternalSource;
import com.bodeum.domain.ai.enums.AiExternalSourceType;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AiExternalSourceRepository extends JpaRepository<AiExternalSource, Long> {

    List<AiExternalSource> findAllBySourceTypeAndActiveTrue(
            AiExternalSourceType sourceType
    );
}
