package com.bodeum.domain.ai.enums;

import java.util.Optional;

public enum AiQuestionIntent {
    WELFARE_SITES,
    LOCAL_REHAB_CENTERS,
    CHILD_MEDICAL_SUPPORT,
    DIAGNOSIS_FIRST_STEPS,
    VOUCHER_APPLICATION,
    MEDICAL_DIAGNOSIS,
    LEGAL_ADVICE,
    INSTITUTION_EVALUATION,
    NONE;

    public Optional<AiStarterQuestionType> starterQuestionType() {
        return switch (this) {
            case WELFARE_SITES -> Optional.of(AiStarterQuestionType.WELFARE_SITES);
            case LOCAL_REHAB_CENTERS -> Optional.of(AiStarterQuestionType.LOCAL_REHAB_CENTERS);
            case CHILD_MEDICAL_SUPPORT -> Optional.of(AiStarterQuestionType.CHILD_MEDICAL_SUPPORT);
            case DIAGNOSIS_FIRST_STEPS -> Optional.of(AiStarterQuestionType.DIAGNOSIS_FIRST_STEPS);
            case VOUCHER_APPLICATION -> Optional.of(AiStarterQuestionType.VOUCHER_APPLICATION);
            default -> Optional.empty();
        };
    }

    public Optional<String> safetyGuidance() {
        return switch (this) {
            case MEDICAL_DIAGNOSIS -> Optional.of("""
                    입력해 주신 증상만으로 특정 질환이나 장애 여부를 진단해드릴 수 없습니다. 정확한 진단은 증상과 연령에 맞는 관련 전문의의 종합적인 평가가 필요합니다.

                    가까운 전문 의료기관에 상담하거나 보건복지상담센터(129)를 통해 적절한 진료기관과 공식 안내를 확인해 주세요.
                    """);
            case LEGAL_ADVICE -> Optional.of("""
                    소송의 승소 여부를 판단하거나 구체적인 법률 자문을 제공해드릴 수 없습니다. 결과는 사실관계와 증거, 적용 법률 등에 따라 달라질 수 있습니다.

                    변호사와 상담하거나 대한법률구조공단(132) 등 공식 법률상담 기관을 통해 상황에 맞는 안내를 받아보세요.
                    """);
            case INSTITUTION_EVALUATION -> Optional.of("""
                    특정 기관이 다른 기관보다 더 좋다고 주관적으로 평가해드릴 수 없습니다.

                    제공 서비스, 전문인력, 이용 비용, 접근성, 대기 기간 등 객관적인 정보를 각 기관의 공식 안내에서 확인해 비교해 주세요. 복지·바우처 제공기관은 사회서비스 전자바우처 또는 관할 지자체의 공식 기관 정보를 함께 확인할 수 있습니다.
                    """);
            default -> Optional.empty();
        };
    }
}
