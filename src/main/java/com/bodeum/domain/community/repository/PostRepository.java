package com.bodeum.domain.community.repository;

import com.bodeum.domain.community.entity.Post;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PostRepository extends JpaRepository<Post, Long> {
}
