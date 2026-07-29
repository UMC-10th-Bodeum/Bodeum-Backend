package com.bodeum.domain.point.repository;

import com.bodeum.domain.point.entity.GuardianPointHistory;
import com.bodeum.domain.point.enums.PointType;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface GuardianPointHistoryRepository extends JpaRepository<GuardianPointHistory, Long> {

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
