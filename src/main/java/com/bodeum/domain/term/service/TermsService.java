package com.bodeum.domain.term.service;

import com.bodeum.domain.term.dto.response.TermsResponse;
import com.bodeum.domain.term.enumtype.TermType;
import java.time.Instant;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TermsService {

    private static final Instant UPDATED_AT = Instant.parse("2026-07-03T01:30:00Z");
    private static final String PRIVACY_CONTENT =
            "보듬은 서비스 제공을 위해 필요한 최소한의 개인정보를 수집하고 "
                    + "안전하게 관리합니다.";
    private static final String SERVICE_CONTENT =
            "보듬은 보호자와 아이에게 필요한 복지·기관 정보를 제공하는 서비스입니다.";
    private static final String AI_CHAT_CONTENT =
            "보듬은 AI 챗봇 상담을 위해 이용자가 입력한 대화 내용을 처리하며, "
                    + "AI 챗봇 이용에 동의한 경우에 한해 관련 기능을 제공합니다.";

    @Transactional(readOnly = true)
    public TermsResponse getTerms(TermType type) {
        String content = switch (type) {
            case PRIVACY -> PRIVACY_CONTENT;
            case AI_CHAT -> AI_CHAT_CONTENT;
            case SERVICE -> SERVICE_CONTENT;
        };

        return TermsResponse.of(type, content, UPDATED_AT);
    }
}
