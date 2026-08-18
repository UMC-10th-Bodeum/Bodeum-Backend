package com.bodeum.domain.ai.model.question;

/**
 * AI 근거 검색에 적용할 지역 범위를 나타낸다.
 */
public enum AiSearchScope {
    /** 우선 지역의 결과를 먼저 조회하고 부족한 결과는 전국에서 보충 */
    REGION_PRIORITY,
    /** 특정 지역 우선순위 없이 전국 단위로 검색 */
    NATIONWIDE,
    /** 질문이나 사용자 문맥에서 확정된 지역으로 검색 범위를 제한 */
    LOCAL_ONLY
}
