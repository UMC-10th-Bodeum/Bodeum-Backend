package com.bodeum.global.infrastructure.dto;

import java.util.List;
import java.util.Map;

// 모든 외부 공공데이터 API의 파싱 결과를 통일된 규격으로 맞추기 위함. <총 데이터 개수(페이징 조건), 실제 데이터 목록>

public record ExtractedDataResult(
        int totalCount,
        List<Map<String, Object>> records
) {}
