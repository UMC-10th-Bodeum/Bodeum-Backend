package com.bodeum.domain.term.service;

import com.bodeum.domain.term.dto.response.TermsResponse;
import com.bodeum.domain.term.enumtype.TermType;
import java.time.LocalDate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TermsService {

    private static final String CURRENT_VERSION = "1.0";
    private static final LocalDate EFFECTIVE_DATE = LocalDate.of(2026, 7, 4);
    private static final String PRIVACY_CONTENT =
            "보듬은 서비스 제공을 위해 필요한 최소한의 개인정보를 수집하고 "
                    + "안전하게 관리합니다.";
    private static final String AI_CHAT_CONTENT =
            "AI 챗봇 이용에 동의하시면 입력하신 대화 내용이 "
                    + "답변 생성을 위해 처리됩니다. "
                    + "선택 항목이며, 동의하지 않아도 서비스를 이용하실 수 있습니다.";
    private static final String SERVICE_CONTENT =
            "보듬은 보호자와 아이에게 필요한 복지·기관 정보를 제공하는 서비스입니다.";

    @Transactional(readOnly = true)
    public TermsResponse getTerms(TermType type) {
        if (type == TermType.PRIVACY) {
            return TermsResponse.of(
                    type,
                    CURRENT_VERSION,
                    EFFECTIVE_DATE,
                    PRIVACY_CONTENT,
                    true
            );
        }

        if (type == TermType.AI_CHAT) {
            return TermsResponse.of(
                    type,
                    CURRENT_VERSION,
                    EFFECTIVE_DATE,
                    AI_CHAT_CONTENT,
                    false
            );
        }

        return TermsResponse.of(
                type,
                CURRENT_VERSION,
                EFFECTIVE_DATE,
                SERVICE_CONTENT,
                true
        );
    }
}
