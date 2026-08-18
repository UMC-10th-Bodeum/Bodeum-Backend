package com.bodeum.domain.ai.service.validation;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;

/**
 * 사용자 질문에 명시된 목록 요청 개수가 1개 이상인지 검증한다.
 */
@Component
public class AiRequestedResultCountValidator {

    private static final Pattern EXPLICIT_RESULT_COUNT_PATTERN = Pattern.compile(
            "(?<!\\d)([+\\-−]?\\s*\\d+)\\s*(?:개|곳|건)");

    public boolean hasNonPositiveCount(String question) {
        if (question == null || question.isBlank()) {
            return false;
        }
        Matcher matcher = EXPLICIT_RESULT_COUNT_PATTERN.matcher(question);
        while (matcher.find()) {
            String number = matcher.group(1)
                    .replace("−", "-")
                    .replaceAll("\\s+", "");
            try {
                if (Integer.parseInt(number) <= 0) {
                    return true;
                }
            } catch (NumberFormatException ignored) {
                // 정수 범위를 벗어난 값은 이후 최대 결과 개수 정책에서 제한한다.
            }
        }
        return false;
    }
}
