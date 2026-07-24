package com.bodeum.global.infrastructure.dto;

import java.util.Map;

// 제3의 API 규격이 들어왔을 때를 대비.

public record EtcApiResponse(
        Map<String, Object> unknownRawMap // 통째로 보관.
) {}
