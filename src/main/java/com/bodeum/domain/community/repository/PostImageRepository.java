package com.bodeum.domain.community.repository;

import com.bodeum.domain.community.entity.PostImage;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PostImageRepository extends JpaRepository<PostImage, Long> {

    List<PostImage> findAllByPost_IdOrderBySortOrderAsc(Long postId);

    List<PostImage> findAllByPost_IdInOrderByPost_IdAscSortOrderAsc(List<Long> postIds);

    void deleteAllByPost_Id(Long postId);
}
