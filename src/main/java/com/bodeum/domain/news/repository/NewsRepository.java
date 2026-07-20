package com.bodeum.domain.news.repository;

import com.bodeum.domain.news.entity.News;
import java.util.List;
import java.util.Collection;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface NewsRepository extends JpaRepository<News, Long> {

    @EntityGraph(attributePaths = "newsCategory")
    @Query("""
            select news
            from News news
            where news.active = true
              and news.deletedAt is null
            """)
    List<News> findAllIndexable();

    @EntityGraph(attributePaths = "newsCategory")
    @Query("""
            select news
            from News news
            where news.id in :ids
              and news.active = true
              and news.deletedAt is null
            """)
    List<News> findAllIndexableByIdIn(@Param("ids") Collection<Long> ids);

    @EntityGraph(attributePaths = "newsCategory")
    @Query("""
            select news
            from News news
            where news.id = :id
              and news.active = true
              and news.deletedAt is null
            """)
    Optional<News> findIndexableById(@Param("id") Long id);
}
