package com.bodeum.domain.info.repository;

import com.bodeum.domain.info.dto.request.InfoItemSearchCondition;
import com.bodeum.domain.info.entity.InfoItem;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

// jpa로는 동적 쿼리를 짜기 어려움 : custom 인터페이스 -> Impl 구현체 -> 메인 repo 상속

public interface InfoItemRepositoryCustom {
    // 동적 검색 조건(condition)과 페이징 정보(pageable)를 받아 Page<InfoItem>으로 반환.
    Page<InfoItem> searchInfoItems(InfoItemSearchCondition condition, Pageable pageable);
}