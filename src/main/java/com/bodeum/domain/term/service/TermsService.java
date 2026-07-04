package com.bodeum.domain.term.service;

import com.bodeum.domain.term.dto.response.TermsResDTO;
import com.bodeum.domain.term.enumtype.TermType;
import java.time.LocalDate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TermsService {

    @Transactional(readOnly = true)
    public TermsResDTO getTerms(TermType type) {
        if (type == TermType.PRIVACY) {
            return TermsResDTO.of(
                    type,
                    "1.0",
                    LocalDate.of(2026, 7, 4),
                    "보듬은 서비스 제공을 위해 필요한 최소한의 개인정보를 수집하고 안전하게 관리합니다."
            );
        }

        return TermsResDTO.of(
                type,
                "1.0",
                LocalDate.of(2026, 7, 4),
                "보듬은 보호자와 아이에게 필요한 복지·기관 정보를 제공하는 서비스입니다."
        );
    }
}
