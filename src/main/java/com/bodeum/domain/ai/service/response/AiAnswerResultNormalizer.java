package com.bodeum.domain.ai.service.response;

import com.bodeum.domain.ai.model.answer.GeneratedAiAnswer;
import com.bodeum.domain.ai.model.rag.AiReferenceDocument;
import com.bodeum.domain.ai.model.question.AiSearchScope;
import com.bodeum.domain.info.entity.enums.InfoSubCategory;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * 생성된 목록형 AI 답변의 항목 수 문구를
 * 실제 인용 결과 개수와 일치하도록 보정한다.
 */
@Component
public class AiAnswerResultNormalizer {

    @Value("${bodeum.ai.result.max-count:10}")
    private int maxResultCount = 10;

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
    private static final Pattern NORMALIZED_COUNT_MESSAGE_PATTERN = Pattern.compile(
            "^\\s*(?:요청하신\\s+\\d+(?:개|곳|건)\\s+중|요청하신 개수에 맞춰|요청하신 대로)"
                    + "[^\\n.!?]*(?:확인한\\s+)?[^\\n.!?]+\\s+"
                    + "\\d+(?:개|곳|건)(?:을|를)\\s+(?:추가로\\s+)?안내드립니다\\.\\s*$");
    private static final Pattern COUNT_MESSAGE_SENTENCE_PATTERN = Pattern.compile(
            "(?:(?:요청하신\\s+\\d+(?:개|곳|건)\\s+중\\s*)?"
                    + "(?:현재\\s+보듬에서|현재|[^\\n.!?]{1,30}에서)\\s+"
                    + "(?:추가로\\s+)?확인\\s+가능한\\s+[^\\n.!?]+(?:은|는)\\s*"
                    + "\\d+(?:개|곳|건)입니다\\.)"
                    + "|(?:이전에 안내한 항목을 제외하면,?\\s*추가로 확인 가능한\\s+"
                    + "[^\\n.!?]+(?:은|는)\\s*\\d+(?:개|곳|건)입니다\\.)"
                    + "|(?:[^\\n.!?]{1,50}(?:로|으로)\\s+안내할 수 있는 곳은\\s*"
                    + "\\d+(?:개|곳|건)입니다\\.)"
                    + "|(?:(?:요청하신\\s+\\d+(?:개|곳|건)\\s+중|요청하신 개수에 맞춰|요청하신 대로)"
                    + "[^\\n.!?]+\\d+(?:개|곳|건)(?:을|를)\\s+"
                    + "(?:추가로\\s+)?안내드립니다\\.)");

    public GeneratedAiAnswer normalizeListedResultCount(
            GeneratedAiAnswer generated,
            Integer requestedResultCount,
            boolean additionalResults,
            InfoSubCategory infoSubCategory
    ) {
        return normalizeListedResultCount(
                generated, requestedResultCount, additionalResults, infoSubCategory, null);
    }

    public GeneratedAiAnswer normalizeListedResultCount(
            GeneratedAiAnswer generated,
            Integer requestedResultCount,
            boolean additionalResults,
            InfoSubCategory infoSubCategory,
            String region
    ) {
        return normalizeListedResultCount(
                generated, requestedResultCount, additionalResults,
                infoSubCategory, region, true);
    }

    public GeneratedAiAnswer normalizeListedResultCount(
            GeneratedAiAnswer generated,
            Integer requestedResultCount,
            boolean additionalResults,
            InfoSubCategory infoSubCategory,
            String region,
            boolean listRequest
    ) {
        return normalizeListedResultCount(
                generated, requestedResultCount, additionalResults,
                infoSubCategory, region, listRequest, List.of(), null);
    }

    public GeneratedAiAnswer normalizeListedResultCount(
            GeneratedAiAnswer generated,
            Integer requestedResultCount,
            boolean additionalResults,
            InfoSubCategory infoSubCategory,
            String region,
            boolean listRequest,
            List<AiReferenceDocument> documents,
            AiSearchScope searchScope
    ) {
        if (!listRequest) {
            return generated;
        }
        int actualCount = generated.answerItems().size();
        String answer = removeExistingCountMessages(generated.answer());
        if (requestedResultCount == null) {
            return withAnswer(generated, answer);
        }
        if (actualCount == 0) {
            return withAnswer(generated, answer);
        }
        ResultLabel resultLabel = resultLabel(infoSubCategory);
        String target = (region == null || region.isBlank() ? "" : region.trim() + " ")
                + resultLabel.name();
        String countMessage = countMessage(
                requestedResultCount, actualCount, additionalResults, target, resultLabel.unit());
        String mixedRegionMessage = mixedRegionMessage(
                generated, documents, requestedResultCount, additionalResults,
                resultLabel, region, searchScope);
        if (mixedRegionMessage != null) {
            countMessage = mixedRegionMessage;
        }
        return withCountMessage(generated, answer, countMessage);
    }

    private String mixedRegionMessage(
            GeneratedAiAnswer generated,
            List<AiReferenceDocument> documents,
            Integer requestedResultCount,
            boolean additionalResults,
            ResultLabel label,
            String priorityRegion,
            AiSearchScope searchScope
    ) {
        if (requestedResultCount == null
                || requestedResultCount > maxResultCount
                || additionalResults
                || searchScope != AiSearchScope.REGION_PRIORITY
                || priorityRegion == null || priorityRegion.isBlank()
                || documents == null || documents.isEmpty()) {
            return null;
        }
        Set<String> answerKeys = generated.answerItems().stream()
                .map(item -> item.documentKey())
                .filter(key -> key != null && !key.isBlank())
                .collect(Collectors.toSet());
        List<AiReferenceDocument> usedDocuments = documents.stream()
                .filter(document -> answerKeys.contains(document.documentKey()))
                .toList();
        if (usedDocuments.size() != generated.answerItems().size()) {
            return null;
        }
        long priorityCount = usedDocuments.stream()
                .filter(document -> belongsToRegion(document, priorityRegion))
                .count();
        long supplementalCount = usedDocuments.size() - priorityCount;
        if (supplementalCount == 0) {
            return null;
        }
        if (priorityCount == 0) {
            if (usedDocuments.size() < requestedResultCount) {
                return shortRegion(priorityRegion) + "에서 확인 가능한 " + label.name()
                        + objectParticle(label.name()) + " 찾지 못해, 요청하신 "
                        + requestedResultCount + label.unit()
                        + " 중 현재 보듬에서 확인한 다른 지역의 " + label.name() + " "
                        + supplementalCount + label.unit()
                        + objectParticle(label.unit()) + " 안내드립니다.";
            }
            return shortRegion(priorityRegion) + "에서 확인 가능한 " + label.name()
                    + objectParticle(label.name()) + " 찾지 못해, 요청하신 "
                    + requestedResultCount + label.unit()
                    + topicParticle(label.unit()) + " 다른 지역의 " + label.name()
                    + directionalParticle(label.name()) + " 안내드립니다.";
        }
        if (usedDocuments.size() < requestedResultCount) {
            return "요청하신 " + requestedResultCount + label.unit()
                    + " 중 현재 보듬에서 확인 가능한 " + shortRegion(priorityRegion)
                    + " " + label.name() + topicParticle(label.name()) + " "
                    + priorityCount + label.unit()
                    + "입니다. 다른 지역의 " + label.name() + " "
                    + supplementalCount + label.unit()
                    + directionalParticle(label.unit())
                    + " 보충했지만, 현재 총 " + usedDocuments.size()
                    + label.unit() + "만 확인했습니다.";
        }
        return "요청하신 " + requestedResultCount + label.unit()
                + " 중 현재 보듬에서 확인 가능한 " + shortRegion(priorityRegion)
                + " " + label.name() + topicParticle(label.name()) + " "
                + priorityCount + label.unit()
                + "입니다. 부족한 " + supplementalCount + label.unit()
                + topicParticle(label.unit()) + " 다른 지역의 " + label.name()
                + directionalParticle(label.name()) + " 보충했습니다.";
    }

    private boolean belongsToRegion(AiReferenceDocument document, String region) {
        if (document == null || document.content() == null) {
            return false;
        }
        return document.content().replaceAll("\\s+", "")
                .contains(region.replaceAll("\\s+", ""));
    }

    private String shortRegion(String region) {
        String[] parts = region.trim().split("\\s+");
        return parts[parts.length - 1];
    }

    public String normalizeExternalListAnswer(
            String answer,
            int actualCount,
            Integer requestedResultCount,
            boolean additionalResults,
            InfoSubCategory infoSubCategory,
            String region,
            boolean listRequest,
            boolean siteListRequest
    ) {
        if (!listRequest || answer == null || answer.isBlank() || actualCount <= 0) {
            return answer;
        }
        String normalized = removeExistingCountMessages(answer);
        if (requestedResultCount == null) {
            return normalized;
        }
        ResultLabel resultLabel = siteListRequest
                ? new ResultLabel("공식 사이트", "곳")
                : resultLabel(infoSubCategory);
        String target = (region == null || region.isBlank() ? "" : region.trim() + " ")
                + resultLabel.name();
        return normalized + "\n\n" + countMessage(
                requestedResultCount, actualCount, additionalResults,
                target, resultLabel.unit());
    }

    private String countMessage(
            int requestedCount,
            int actualCount,
            boolean additionalResults,
            String target,
            String unit
    ) {
        String exclusion = additionalResults
                ? " 이전에 안내한 항목을 제외하고" : "";
        if (requestedCount > maxResultCount) {
            return "한 번에 최대 " + maxResultCount + unit + "까지 안내할 수 있어,"
                    + exclusion + " 현재 보듬에서 확인한 " + target + " "
                    + actualCount + unit
                    + objectParticle(unit)
                    + (additionalResults ? " 추가로 안내드립니다." : " 안내드립니다.");
        }
        if (actualCount < requestedCount) {
            return "요청하신 " + requestedCount + unit + " 중" + exclusion
                    + " 현재 보듬에서 확인한 " + target + " "
                    + actualCount + unit + objectParticle(unit) + " 안내드립니다.";
        }
        return "요청하신 개수에 맞춰" + exclusion + " " + target + " "
                + requestedCount + unit
                + objectParticle(unit)
                + (additionalResults ? " 추가로 안내드립니다." : " 안내드립니다.");
    }

    private String objectParticle(String unit) {
        return hasFinalConsonant(unit) ? "을" : "를";
    }

    private String topicParticle(String word) {
        return hasFinalConsonant(word) ? "은" : "는";
    }

    private String directionalParticle(String word) {
        char last = lastKoreanSyllable(word);
        if (last == 0) {
            return "로";
        }
        int finalConsonant = (last - 0xAC00) % 28;
        return finalConsonant == 0 || finalConsonant == 8 ? "로" : "으로";
    }

    private boolean hasFinalConsonant(String word) {
        char last = lastKoreanSyllable(word);
        return last != 0 && (last - 0xAC00) % 28 != 0;
    }

    private char lastKoreanSyllable(String word) {
        if (word == null) {
            return 0;
        }
        for (int index = word.length() - 1; index >= 0; index--) {
            char current = word.charAt(index);
            if (current >= 0xAC00 && current <= 0xD7A3) {
                return current;
            }
        }
        return 0;
    }

    private String removeExistingCountMessages(String originalAnswer) {
        return Arrays.stream(originalAnswer.split("\\R", -1))
                .map(this::removeCountMessageSentences)
                .filter(line -> !line.isBlank())
                .collect(Collectors.joining("\n"))
                .replaceAll("\\n{3,}", "\n\n")
                .trim();
    }

    private String removeCountMessageSentences(String line) {
        if (isCountMessage(line)) {
            return "";
        }
        return COUNT_MESSAGE_SENTENCE_PATTERN.matcher(line)
                .replaceAll("")
                .replaceAll("\\s{2,}", " ")
                .trim();
    }

    private boolean isCountMessage(String line) {
        return STANDARD_COUNT_MESSAGE_PATTERN.matcher(line).matches()
                || ADDITIONAL_COUNT_MESSAGE_PATTERN.matcher(line).matches()
                || NORMALIZED_COUNT_MESSAGE_PATTERN.matcher(line).matches();
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

    private GeneratedAiAnswer withAnswer(GeneratedAiAnswer generated, String answer) {
        return new GeneratedAiAnswer(
                answer,
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
