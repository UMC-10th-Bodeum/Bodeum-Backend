package com.bodeum.domain.ai.repository;

import com.bodeum.domain.ai.entity.AiResponseSource;
import com.bodeum.domain.ai.repository.projection.AiResponseSourceProjection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface AiResponseSourceRepository extends JpaRepository<AiResponseSource, Long> {

    @Query("""
        SELECT
            s.aiMessage.id AS aiMessageId,
            s.sourceType AS sourceType,
            s.sourceId AS sourceId,
            s.sourceTitle AS sourceTitle,
            s.sourceUrl AS sourceUrl,
            s.sourceUpdatedAt AS sourceUpdatedAt
        FROM AiResponseSource s
        WHERE s.aiMessage.id IN :messageIds
        """)
    List<AiResponseSourceProjection> findAllByMessageIds(
            @Param("messageIds") List<Long> messageIds
    );
}
