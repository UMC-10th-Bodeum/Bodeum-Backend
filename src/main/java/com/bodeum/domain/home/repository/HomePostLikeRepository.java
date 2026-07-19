package com.bodeum.domain.home.repository;

import com.bodeum.domain.community.entity.PostLike;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface HomePostLikeRepository extends JpaRepository<PostLike, Long> {

    @Query("SELECT pl.post.id, COUNT(pl) FROM PostLike pl WHERE pl.post.id IN :postIds GROUP BY pl.post.id")
    List<Object[]> countGroupByPostIdIn(@Param("postIds") List<Long> postIds);
}
