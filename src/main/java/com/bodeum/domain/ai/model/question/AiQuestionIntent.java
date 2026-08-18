package com.bodeum.domain.ai.model.question;

/**
 * 사용자 질문에서 분류한 주요 AI 응답 의도를 나타낸다.
 * 선별 답변과 안전 안내가 필요한 질문을 일반 문서 검색과 구분하는 데 사용한다.
 */
public enum AiQuestionIntent {
    /** 공식 복지 사이트 추천 요청 */
    WELFARE_SITES,
    /** 사용자 지역의 치료·재활기관 추천 요청 */
    LOCAL_REHAB_CENTERS,
    /** 장애아동 의료비 지원 정보 요청 */
    CHILD_MEDICAL_SUPPORT,
    /** 장애 진단 이후의 초기 대응 절차 요청 */
    DIAGNOSIS_FIRST_STEPS,
    /** 발달재활서비스 바우처 신청 정보 요청 */
    VOUCHER_APPLICATION,
    /** 의료적 진단이나 판단을 요구하는 질문 */
    MEDICAL_DIAGNOSIS,
    /** 법률적 판단이나 자문을 요구하는 질문 */
    LEGAL_ADVICE,
    /** 특정 기관에 대한 평가나 단정을 요구하는 질문 */
    INSTITUTION_EVALUATION,
    /** 별도 선별 의도에 해당하지 않는 일반 질문 */
    NONE
}
