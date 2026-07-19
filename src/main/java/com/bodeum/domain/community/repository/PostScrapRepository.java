package com.bodeum.domain.community.repository;

import com.bodeum.domain.community.entity.PostScrap;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PostScrapRepository extends JpaRepository<PostScrap, Long> {

    void deleteAllByPost_Id(Long postId);
}
