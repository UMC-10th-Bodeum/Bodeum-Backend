package com.bodeum.domain.info.repository;

import com.bodeum.domain.info.entity.InfoScrap;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface InfoScrapRepository extends JpaRepository<InfoScrap, Long> {

    @Modifying
    @Query("delete from InfoScrap s where s.user.id = :userId")
    int deleteByUserId(@Param("userId") Long userId);
}
