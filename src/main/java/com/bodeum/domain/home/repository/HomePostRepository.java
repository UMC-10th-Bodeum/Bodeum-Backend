package com.bodeum.domain.home.repository;

import com.bodeum.domain.community.entity.Post;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface HomePostRepository extends JpaRepository<Post, Long> {

    @Query("SELECT p FROM Post p ORDER BY (SELECT COUNT(pl) FROM PostLike pl WHERE pl.post = p) DESC, p.createdAt DESC")
    List<Post> findTopByPopularity(Pageable pageable);

    List<Post> findAllByOrderByCreatedAtDesc(Pageable pageable);
}
