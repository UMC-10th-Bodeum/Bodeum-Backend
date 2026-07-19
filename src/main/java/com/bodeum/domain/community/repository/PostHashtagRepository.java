package com.bodeum.domain.community.repository;

import com.bodeum.domain.community.entity.PostHashtag;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PostHashtagRepository extends JpaRepository<PostHashtag, Long> {

    List<PostHashtag> findAllByPost_IdOrderByIdAsc(Long postId);

    void deleteAllByPost_Id(Long postId);
}
