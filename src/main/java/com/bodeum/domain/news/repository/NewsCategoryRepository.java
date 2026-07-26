package com.bodeum.domain.news.repository;

import com.bodeum.domain.news.entity.NewsCategory;
import com.bodeum.domain.news.entity.NewsType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NewsCategoryRepository extends JpaRepository<NewsCategory, Long> {

    Optional<NewsCategory> findByNewsTypeAndName(NewsType newsType, String name);
}
