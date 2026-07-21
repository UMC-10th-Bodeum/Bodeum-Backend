package com.bodeum.domain.home.repository;

import com.bodeum.domain.news.entity.News;
import com.bodeum.domain.news.entity.NewsType;
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
              AND (n.regionId = :regionId OR n.regionId IS NULL)
            ORDER BY (n.viewCount + n.scrapCount) DESC
            """)
    List<News> findTopRecommendedByRegion(@Param("regionId") Long regionId, Pageable pageable);

    @Query("SELECT n FROM News n WHERE n.newsCategory.newsType = :newsType AND n.active = true AND n.deletedAt IS NULL ORDER BY (n.viewCount + n.scrapCount) DESC")
    List<News> findByNewsType(@Param("newsType") NewsType newsType, Pageable pageable);

    @Query("""
            SELECT n FROM News n
            WHERE n.newsCategory.newsType = :newsType
              AND n.active = true
              AND n.deletedAt IS NULL
              AND (n.regionId = :regionId OR n.regionId IS NULL)
            ORDER BY (n.viewCount + n.scrapCount) DESC
            """)
    List<News> findByNewsTypeAndRegion(
            @Param("newsType") NewsType newsType,
            @Param("regionId") Long regionId,
            Pageable pageable
    );
}
