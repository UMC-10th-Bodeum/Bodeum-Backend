package com.bodeum.domain.home.repository;

import com.bodeum.domain.news.entity.News;
import com.bodeum.domain.news.entity.NewsType;
import com.bodeum.domain.news.entity.RecruitmentStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface HomeNewsRepository extends JpaRepository<News, Long> {

    @Query("SELECT n FROM News n WHERE n.active = true AND n.deletedAt IS NULL ORDER BY (n.viewCount + n.scrapCount) DESC")
    List<News> findTopRecommended(Pageable pageable);

    @Query("""
            SELECT n FROM News n
            WHERE n.active = true
              AND n.deletedAt IS NULL
              AND n.regionId = :regionId
            ORDER BY (n.viewCount + n.scrapCount) DESC
            """)
    List<News> findTopRecommendedByRegion(@Param("regionId") Long regionId, Pageable pageable);

    @Query("SELECT n FROM News n JOIN n.newsCategory nc WHERE nc.newsType = :newsType AND n.active = true AND n.deletedAt IS NULL ORDER BY n.createdAt DESC")
    List<News> findByNewsType(@Param("newsType") NewsType newsType, Pageable pageable);

    @Query("""
            SELECT n FROM News n
            JOIN n.newsCategory nc
            WHERE nc.newsType = :newsType
              AND n.active = true
              AND n.deletedAt IS NULL
              AND n.regionId = :regionId
            ORDER BY n.createdAt DESC
            """)
    List<News> findByNewsTypeAndRegion(
            @Param("newsType") NewsType newsType,
            @Param("regionId") Long regionId,
            Pageable pageable
    );

    @Query("""
            SELECT n FROM News n
            JOIN NewsScrap ns ON ns.news.id = n.id
            WHERE ns.userId = :userId
              AND n.active = true
              AND n.deletedAt IS NULL
              AND n.recruitmentStatus != :excluded
              AND n.applyEndDate >= CURRENT_DATE
            ORDER BY n.applyEndDate ASC
            """)
    List<News> findBannerForUser(
            @Param("userId") Long userId,
            @Param("excluded") RecruitmentStatus excluded,
            Pageable pageable
    );

    @Query("""
            SELECT n FROM News n
            WHERE n.active = true
              AND n.deletedAt IS NULL
              AND n.recruitmentStatus != :excluded
              AND n.applyEndDate >= CURRENT_DATE
            ORDER BY n.scrapCount DESC
            """)
    List<News> findBannerForAnonymous(
            @Param("excluded") RecruitmentStatus excluded,
            Pageable pageable
    );
}
