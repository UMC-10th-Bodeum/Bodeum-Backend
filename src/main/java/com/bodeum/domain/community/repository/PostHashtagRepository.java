package com.bodeum.domain.community.repository;

import com.bodeum.domain.community.entity.PostHashtag;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PostHashtagRepository extends JpaRepository<PostHashtag, Long> {

    List<PostHashtag> findAllByPost_IdOrderByIdAsc(Long postId);

    List<PostHashtag> findAllByPost_IdIn(List<Long> postIds);

    void deleteAllByPost_Id(Long postId);
}
