package com.bodeum.domain.search.repository;

import com.bodeum.domain.search.entity.SearchLog;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface SearchLogRepository extends JpaRepository<SearchLog, Long> {

    @Query("SELECT s.keyword FROM SearchLog s WHERE s.userId = :userId GROUP BY s.keyword ORDER BY MAX(s.createdAt) DESC")
    List<String> findDistinctKeywordsByUserIdOrderByLatest(@Param("userId") Long userId, Pageable pageable);

    @Modifying
    @Query("DELETE FROM SearchLog s WHERE s.userId = :userId AND s.keyword = :keyword")
    int deleteByUserIdAndKeyword(@Param("userId") Long userId, @Param("keyword") String keyword);

    @Modifying
    @Query("DELETE FROM SearchLog s WHERE s.userId = :userId")
    int deleteByUserId(@Param("userId") Long userId);
}
