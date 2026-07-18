package com.bodeum.domain.community.repository;

import com.bodeum.domain.community.entity.PostLike;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PostLikeRepository extends JpaRepository<PostLike, Long> {

    void deleteAllByPost_Id(Long postId);
}
