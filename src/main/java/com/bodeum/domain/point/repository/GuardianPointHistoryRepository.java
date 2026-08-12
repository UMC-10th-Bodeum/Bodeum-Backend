package com.bodeum.domain.point.repository;

import com.bodeum.domain.point.entity.GuardianPointHistory;
import com.bodeum.domain.point.enums.PointEventType;
import com.bodeum.domain.point.enums.PointType;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface GuardianPointHistoryRepository extends JpaRepository<GuardianPointHistory, Long> {

    boolean existsByGuardianPoint_IdAndEventTypeAndReferenceIdAndActorUserId(
            Long guardianPointId,
            PointEventType eventType,
            Long referenceId,
            Long actorUserId
    );

    Optional<GuardianPointHistory> findByGuardianPoint_IdAndEventTypeAndReferenceIdAndActorUserId(
            Long guardianPointId,
            PointEventType eventType,
            Long referenceId,
            Long actorUserId
    );

    long countByGuardianPoint_IdAndEventTypeAndCreatedAtGreaterThanEqualAndCreatedAtLessThan(
            Long guardianPointId,
            PointEventType eventType,
            Instant startInclusive,
            Instant endExclusive
    );

    @Query("""
            SELECT history.pointType AS pointType,
                   SUM(history.pointValue) AS earnedPoint,
                   COUNT(history) AS activityCount
            FROM GuardianPointHistory history
            WHERE history.guardianPoint.id = :guardianPointId
            GROUP BY history.pointType
            """)
    List<PointActivitySummary> summarizeByGuardianPointId(
            @Param("guardianPointId") Long guardianPointId
    );

    @Modifying
    @Query("""
            DELETE FROM GuardianPointHistory history
            WHERE history.guardianPoint.id = :guardianPointId
            """)
    int deleteByGuardianPointId(@Param("guardianPointId") Long guardianPointId);

    interface PointActivitySummary {

        PointType getPointType();

        Long getEarnedPoint();

        Long getActivityCount();
    }
}
