package com.bodeum.domain.news.repository;

import com.bodeum.domain.news.entity.NewsScrap;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NewsScrapRepository extends JpaRepository<NewsScrap, Long> {

    boolean existsByNewsIdAndUserId(Long newsId, Long userId);

    Optional<NewsScrap> findByNewsIdAndUserId(Long newsId, Long userId);
}
