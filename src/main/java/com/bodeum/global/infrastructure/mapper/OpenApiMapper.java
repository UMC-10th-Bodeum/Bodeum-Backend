package com.bodeum.global.infrastructure.mapper;

import com.bodeum.domain.info.entity.InfoCategory;
import com.bodeum.domain.info.entity.InfoItem;
import com.bodeum.global.infrastructure.constant.OpenApiSourceSpec;

import java.util.List;

// 외부 오픈 API 데이터를 InfoItem 엔티티로 정규화하는 매퍼 인터페이스

public interface OpenApiMapper {

    // 해당 매퍼가 처리할 수 있는 오픈 API 출처인지 식별.
    boolean supports(OpenApiSourceSpec sourceSpec);

    /**
     * raw 응답 데이터를 파싱하여 InfoItem 엔티티로 변환.
     * @param rawData 외부 API가 리턴한 JSON/XML 데이터 문자열
     * @param category DB에서 매핑해 온 소분류 카테고리 엔티티
     * @param sourceSpec 호출된 API 출처 정보
     */
    InfoItem mapToEntity(String rawData, InfoCategory category, OpenApiSourceSpec sourceSpec);

    /**
     * raw 응답 데이터를 파싱하여 InfoItem 엔티티 리스트로 변환.
     */
    List<InfoItem> mapToEntityList(String rawData, InfoCategory category, OpenApiSourceSpec sourceSpec);
}