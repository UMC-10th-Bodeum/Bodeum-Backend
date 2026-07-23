package com.bodeum.domain.news.repository;

import com.bodeum.domain.news.entity.NewsScrap;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface NewsScrapRepository extends JpaRepository<NewsScrap, Long> {

    @Modifying
    @Query("delete from NewsScrap s where s.userId = :userId")
    int deleteByUserId(@Param("userId") Long userId);
}
