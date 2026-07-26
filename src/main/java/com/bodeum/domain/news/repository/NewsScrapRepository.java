package com.bodeum.domain.news.repository;

import com.bodeum.domain.news.entity.NewsScrap;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NewsScrapRepository extends JpaRepository<NewsScrap, Long> {

    boolean existsByNewsIdAndUserId(Long newsId, Long userId);
}
