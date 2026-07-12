package com.bodeum.domain.term.service;

import com.bodeum.domain.term.dto.response.TermsResponse;
import com.bodeum.domain.term.enumtype.TermType;
import java.time.LocalDateTime;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TermsService {

    private static final LocalDateTime UPDATED_AT = LocalDateTime.of(2026, 7, 3, 1, 30);
    private static final String PRIVACY_CONTENT =
            "보듬은 서비스 제공을 위해 필요한 최소한의 개인정보를 수집하고 "
                    + "안전하게 관리합니다.";
    private static final String SERVICE_CONTENT =
            "보듬은 보호자와 아이에게 필요한 복지·기관 정보를 제공하는 서비스입니다.";

    @Transactional(readOnly = true)
    public TermsResponse getTerms(TermType type) {
        if (type == TermType.PRIVACY) {
            return TermsResponse.of(
                    type,
                    PRIVACY_CONTENT,
                    UPDATED_AT
            );
        }

        return TermsResponse.of(
                type,
                SERVICE_CONTENT,
                UPDATED_AT
        );
    }
}
