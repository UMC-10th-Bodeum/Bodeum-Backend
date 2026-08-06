package com.bodeum.domain.term.enums;

import com.bodeum.domain.term.exception.TermsErrorCode;
import com.bodeum.global.apiPayload.exception.ProjectException;
import java.util.Arrays;

public enum TermType {

    SERVICE("service", "서비스 이용약관"),
    PRIVACY("privacy", "개인정보처리방침"),
    AI_CHAT("ai-chat", "AI 챗봇 이용 동의");

    private final String path;
    private final String title;

    TermType(String path, String title) {
        this.path = path;
        this.title = title;
    }

    public String getPath() {
        return path;
    }

    public String getTitle() {
        return title;
    }

    public static TermType from(String value) {
        return Arrays.stream(values())
                .filter(type -> type.path.equalsIgnoreCase(value))
                .findFirst()
                .orElseThrow(() -> new ProjectException(TermsErrorCode.UNSUPPORTED_TYPE));
    }
}
