package com.bodeum.domain.mypage.repository;

import com.bodeum.domain.mypage.entity.GuardianPointHistory;
import java.util.Collection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface GuardianPointHistoryRepository extends JpaRepository<GuardianPointHistory, Long> {

    @Modifying
    @Query("delete from GuardianPointHistory h where h.guardianPoint.id in :guardianPointIds")
    int deleteByGuardianPointIdIn(@Param("guardianPointIds") Collection<Long> guardianPointIds);
}
