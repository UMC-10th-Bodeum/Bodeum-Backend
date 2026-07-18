package com.bodeum.domain.community.repository;

import com.bodeum.domain.community.entity.PostDisabilityTag;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PostDisabilityTagRepository extends JpaRepository<PostDisabilityTag, Long> {

    List<PostDisabilityTag> findAllByPost_IdOrderByIdAsc(Long postId);

    void deleteAllByPost_Id(Long postId);
}
