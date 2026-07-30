package com.bodeum.domain.info.repository;

import com.bodeum.domain.info.entity.InfoItem;
import com.bodeum.domain.info.entity.InfoItemTag;
import com.bodeum.domain.info.entity.InfoTag;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface InfoItemTagRepository extends JpaRepository<InfoItemTag, Long> {
    boolean existsByInfoItemAndInfoTag(InfoItem infoItem, InfoTag infoTag);

    // 단건 상세 조회용 (InfoTag까지 Fetch Join)
    @EntityGraph(attributePaths = "infoTag")
    List<InfoItemTag> findAllByInfoItem(InfoItem infoItem);

    // 목록 조회 Batch 처리용 (InfoItem ID 목록 기준 Fetch Join)
    @EntityGraph(attributePaths = "infoTag")
    List<InfoItemTag> findAllByInfoItemIdIn(Collection<Long> infoItemIds);
}