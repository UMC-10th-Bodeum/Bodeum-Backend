package com.bodeum.domain.ai.service.validation;

import java.math.BigInteger;
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
            if (new BigInteger(number).signum() <= 0) {
                return true;
            }
        }
        return false;
    }
}
