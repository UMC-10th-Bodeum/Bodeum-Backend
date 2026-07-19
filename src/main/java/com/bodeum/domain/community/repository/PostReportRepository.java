package com.bodeum.domain.community.repository;

import com.bodeum.domain.community.entity.PostReport;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PostReportRepository extends JpaRepository<PostReport, Long> {

    void deleteAllByPost_Id(Long postId);
}
