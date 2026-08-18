package com.bodeum.domain.ai.service.response;

import com.bodeum.domain.ai.model.answer.GeneratedAiAnswer;
import com.bodeum.domain.info.entity.enums.InfoSubCategory;
import java.util.Arrays;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

/**
 * 생성된 목록형 AI 답변의 항목 수 문구를
 * 실제 인용 결과 개수와 일치하도록 보정한다.
 */
@Component
public class AiAnswerResultNormalizer {

    private static final Pattern STANDARD_COUNT_MESSAGE_PATTERN = Pattern.compile(
            "^\\s*(?:(?:요청하신\\s+\\d+(?:개|곳|건)\\s+중\\s+)"
                    + "|(?:[^\\n.!?]{1,40}(?:에서|기준으로)\\s+))?"
                    + "(?:현재\\s+보듬에서\\s+)?(?:추가로\\s+)?"
                    + "확인\\s+가능한\\s+[^\\n.!?]+(?:은|는)\\s*"
                    + "\\d+(?:개|곳|건)입니다\\.\\s*$");
    private static final Pattern ADDITIONAL_COUNT_MESSAGE_PATTERN = Pattern.compile(
            "^\\s*이전에 안내한 항목을 제외하면,?\\s*추가로 확인 가능한\\s+"
                    + "관련 (?:항목|학교|기관|사이트)(?:은|는)\\s*"
                    + "\\d+(?:개|곳|건)입니다\\.\\s*$");

    public GeneratedAiAnswer normalizeListedResultCount(
            GeneratedAiAnswer generated,
            Integer requestedResultCount,
            boolean additionalResults,
            InfoSubCategory infoSubCategory
    ) {
        int actualCount = generated.answerItems().size();
        if (actualCount == 0
                || requestedResultCount == null
                || actualCount >= requestedResultCount) {
            return generated;
        }
        String answer = removeExistingCountMessages(generated.answer());
        ResultLabel resultLabel = resultLabel(infoSubCategory);
        String countMessage = additionalResults
                ? "요청하신 " + requestedResultCount + resultLabel.unit()
                        + " 중 이전에 안내한 항목을 제외하고 현재 보듬에서 추가로 확인 가능한 "
                        + resultLabel.name() + "은 " + actualCount + resultLabel.unit() + "입니다."
                : "요청하신 " + requestedResultCount + resultLabel.unit()
                        + " 중 현재 보듬에서 확인 가능한 " + resultLabel.name() + "은 "
                        + actualCount + resultLabel.unit() + "입니다.";
        return withCountMessage(generated, answer, countMessage);
    }

    private String removeExistingCountMessages(String originalAnswer) {
        return Arrays.stream(originalAnswer.split("\\R", -1))
                .filter(line -> !isCountMessage(line))
                .collect(Collectors.joining("\n"))
                .replaceAll("\\n{3,}", "\n\n")
                .trim();
    }

    private boolean isCountMessage(String line) {
        return STANDARD_COUNT_MESSAGE_PATTERN.matcher(line).matches()
                || ADDITIONAL_COUNT_MESSAGE_PATTERN.matcher(line).matches();
    }

    private GeneratedAiAnswer withCountMessage(
            GeneratedAiAnswer generated,
            String answer,
            String countMessage
    ) {
        return new GeneratedAiAnswer(
                answer + "\n\n" + countMessage,
                generated.citedDocumentKeys(),
                generated.answerItems()
        );
    }

    public GeneratedAiAnswer normalizeListedResultCount(
            GeneratedAiAnswer generated,
            Integer requestedResultCount,
            boolean additionalResults
    ) {
        int actualCount = generated.answerItems().size();
        if (actualCount == 0
                || requestedResultCount == null
                || actualCount >= requestedResultCount) {
            return generated;
        }
        String countMessage = additionalResults
                ? "이전에 안내한 항목을 제외하면, 추가로 확인 가능한 관련 항목은 "
                        + actualCount + "개입니다."
                : "현재 확인 가능한 관련 항목은 " + actualCount + "개입니다.";
        return withCountMessage(
                generated, removeExistingCountMessages(generated.answer()), countMessage);
    }

    private ResultLabel resultLabel(InfoSubCategory category) {
        if (category == null) {
            return new ResultLabel("관련 항목", "개");
        }
        return switch (category) {
            case PRIMARY_CARE -> new ResultLabel("건강주치의 기관", "곳");
            case EMERGENCY_CLINIC -> new ResultLabel("응급의료기관", "곳");
            case THERAPY_REHAB -> new ResultLabel("치료·재활기관", "곳");
            case WELFARE_CENTER -> new ResultLabel("장애인복지관", "곳");
            case FAMILY_SUPPORT -> new ResultLabel("장애인가족지원센터", "곳");
            case SPECIAL_SCHOOL -> new ResultLabel("특수학교", "곳");
            case SPECIAL_EDU_SUPPORT -> new ResultLabel("특수교육지원센터", "곳");
            case LIFELONG_EDU -> new ResultLabel("장애인 평생교육기관", "곳");
            case STANDARD_WORKPLACE -> new ResultLabel("장애인표준사업장", "곳");
            case REALTIME_JOB -> new ResultLabel("구인 정보", "건");
            case PRIVATE_WELFARE, NATIONAL_WELFARE, LOCAL_WELFARE ->
                    new ResultLabel("복지 서비스", "개");
            default -> new ResultLabel("관련 항목", "개");
        };
    }

    private record ResultLabel(String name, String unit) {
    }
}
