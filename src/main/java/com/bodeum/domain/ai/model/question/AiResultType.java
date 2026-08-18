package com.bodeum.domain.ai.model.question;

/**
 * 질문 처리 파이프라인에서 최종적으로 사용할 결과 형태이다.
 */
public enum AiResultType {
    DOCUMENT_ANSWER, // 일반 RAG·설명 답변
    SITE_LIST, // 사이트 목록 검증 적용
    RESOURCE_LIST // info_item 기반 구조화 기관 검색
}
