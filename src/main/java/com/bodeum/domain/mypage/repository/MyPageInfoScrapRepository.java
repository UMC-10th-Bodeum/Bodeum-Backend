package com.bodeum.domain.mypage.repository;

import com.bodeum.domain.info.entity.InfoScrap;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MyPageInfoScrapRepository
        extends JpaRepository<InfoScrap, Long> {

    @Query("""
            select count(scrap)
            from InfoScrap scrap
            where scrap.user.id = :userId
            """)
    long countByUserId(@Param("userId") Long userId);
}
