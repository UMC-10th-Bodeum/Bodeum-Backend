package com.bodeum.domain.term.enumtype;

import com.bodeum.global.apiPayload.code.GeneralErrorCode;
import com.bodeum.global.apiPayload.exception.ProjectException;
import java.util.Arrays;
import java.util.Set;

public enum TermType {

    SERVICE("service", Set.of("terms", "service-terms"), "이용약관"),
    PRIVACY("privacy", Set.of("privacy-policy"), "개인정보처리방침");

    private final String path;
    private final Set<String> aliases;
    private final String title;

    TermType(String path, Set<String> aliases, String title) {
        this.path = path;
        this.aliases = aliases;
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
                .filter(type -> type.path.equalsIgnoreCase(value) || type.aliases.contains(value.toLowerCase()))
                .findFirst()
                .orElseThrow(() -> new ProjectException(GeneralErrorCode.BAD_REQUEST));
    }
}
