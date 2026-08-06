package com.bodeum.domain.news.repository;

import com.bodeum.domain.news.entity.NewsSource;
import com.bodeum.domain.news.entity.NewsSourceType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NewsSourceRepository extends JpaRepository<NewsSource, Long> {

    Optional<NewsSource> findBySourceTypeAndName(NewsSourceType sourceType, String name);
}
