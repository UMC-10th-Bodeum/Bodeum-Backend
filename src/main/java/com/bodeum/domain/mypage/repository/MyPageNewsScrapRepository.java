package com.bodeum.domain.mypage.repository;

import com.bodeum.domain.news.entity.NewsScrap;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MyPageNewsScrapRepository
        extends JpaRepository<NewsScrap, Long> {

    @Query("""
            select count(scrap)
            from NewsScrap scrap
            where scrap.userId = :userId
              and scrap.news.active = true
              and scrap.news.deletedAt is null
            """)
    long countVisibleByUserId(@Param("userId") Long userId);
}
