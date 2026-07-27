package com.bodeum.domain.news.repository;

import com.bodeum.domain.news.entity.News;
import com.bodeum.domain.news.entity.NewsSource;
import com.bodeum.domain.news.entity.RecruitmentStatus;
import jakarta.persistence.LockModeType;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface NewsRepository extends JpaRepository<News, Long> {

    @EntityGraph(attributePaths = {"newsCategory", "newsSource"})
    @Query("""
            select news
            from News news
            where news.active = true
              and news.deletedAt is null
              and (:category is null or lower(news.newsCategory.name) = lower(:category))
              and (:status is null or news.recruitmentStatus = :status)
              and (:filterByRegion = false or news.regionId in :regionIds)
            """)
    Page<News> findVisibleNews(
            @Param("category") String category,
            @Param("status") RecruitmentStatus status,
            @Param("filterByRegion") boolean filterByRegion,
            @Param("regionIds") Collection<Long> regionIds,
            Pageable pageable
    );

    @EntityGraph(attributePaths = {"newsCategory", "newsSource"})
    @Query("""
            select news
            from News news
            where news.active = true
              and news.deletedAt is null
              and (
                    lower(news.title) like :keyword
                    or (news.summary is not null and lower(news.summary) like :keyword)
                    or (news.content is not null and lower(cast(news.content as String)) like :keyword)
                    or (news.sourceName is not null and lower(news.sourceName) like :keyword)
                    or (:filterByKeywordRegion = true and news.regionId in :keywordRegionIds)
              )
              and (:category is null or lower(news.newsCategory.name) = lower(:category))
              and (:status is null or news.recruitmentStatus = :status)
              and (:filterByRegion = false or news.regionId in :regionIds)
            """)
    Page<News> searchVisibleNews(
            @Param("keyword") String keyword,
            @Param("filterByKeywordRegion") boolean filterByKeywordRegion,
            @Param("keywordRegionIds") Collection<Long> keywordRegionIds,
            @Param("category") String category,
            @Param("status") RecruitmentStatus status,
            @Param("filterByRegion") boolean filterByRegion,
            @Param("regionIds") Collection<Long> regionIds,
            Pageable pageable
    );

    @EntityGraph(attributePaths = {"newsCategory", "newsSource"})
    @Query("""
            select news
            from News news
            where news.id = :id
              and news.active = true
              and news.deletedAt is null
            """)
    Optional<News> findVisibleById(@Param("id") Long id);

    @Query("""
            select news
            from News news
            where news.id <> :newsId
              and news.regionId in :regionIds
              and news.recruitmentStatus = :status
              and news.active = true
              and news.deletedAt is null
            """)
    List<News> findRelatedRecruitingNews(
            @Param("newsId") Long newsId,
            @Param("regionIds") Collection<Long> regionIds,
            @Param("status") RecruitmentStatus status,
            Pageable pageable
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select news
            from News news
            where news.id = :id
              and news.active = true
              and news.deletedAt is null
            """)
    Optional<News> findVisibleByIdForUpdate(@Param("id") Long id);

    List<News> findAllByNewsSourceAndExternalItemIdIn(
            NewsSource newsSource,
            Collection<String> externalItemIds
    );

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            update News news
            set news.viewCount = news.viewCount + 1
            where news.id = :id
              and news.active = true
              and news.deletedAt is null
            """)
    int incrementViewCount(@Param("id") Long id);

    @EntityGraph(attributePaths = {"newsCategory", "newsSource"})
    @Query("""
            select news
            from News news
            where news.active = true
              and news.deletedAt is null
            """)
    List<News> findAllIndexable();

    @EntityGraph(attributePaths = {"newsCategory", "newsSource"})
    @Query("""
            select news
            from News news
            where news.id in :ids
              and news.active = true
              and news.deletedAt is null
            """)
    List<News> findAllIndexableByIdIn(@Param("ids") Collection<Long> ids);

    @EntityGraph(attributePaths = {"newsCategory", "newsSource"})
    @Query("""
            select news
            from News news
            where news.id = :id
              and news.active = true
              and news.deletedAt is null
            """)
    Optional<News> findIndexableById(@Param("id") Long id);
}
