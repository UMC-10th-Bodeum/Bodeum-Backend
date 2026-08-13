package com.bodeum.domain.point.repository;

import com.bodeum.domain.point.entity.GuardianPoint;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface GuardianPointRepository extends JpaRepository<GuardianPoint, Long> {

    Optional<GuardianPoint> findByGuardianProfile_Id(Long guardianProfileId);

    @Query("""
            SELECT guardianPoint
            FROM GuardianPoint guardianPoint
            WHERE guardianPoint.guardianProfile.user.id = :userId
            """)
    Optional<GuardianPoint> findByUserId(@Param("userId") Long userId);

    @Query("""
            SELECT guardianPoint.guardianProfile.user.id AS userId,
                   guardianPoint.totalPoint AS totalPoint
            FROM GuardianPoint guardianPoint
            WHERE guardianPoint.guardianProfile.user.id IN :userIds
            """)
    List<UserTotalPoint> findTotalPointsByUserIdIn(
            @Param("userIds") Collection<Long> userIds
    );

    interface UserTotalPoint {

        Long getUserId();

        Integer getTotalPoint();
    }
}
