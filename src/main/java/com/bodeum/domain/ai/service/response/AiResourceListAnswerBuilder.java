package com.bodeum.domain.ai.service.response;

import com.bodeum.domain.ai.model.rag.AiReferenceDocument;
import com.bodeum.domain.ai.model.question.AiSearchScope;
import com.bodeum.domain.info.entity.enums.InfoSubCategory;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * 구조화된 기관 검색 결과를 LLM 없이 사용자에게 보여줄 목록 답변으로 구성한다.
 */
@Component
public class AiResourceListAnswerBuilder {

    @Value("${bodeum.ai.result.max-count:10}")
    private int maxResultCount = 10;

    private static final Set<String> INTERNAL_METADATA_LABELS = Set.of(
            "정보명", "대분류", "세부 분류");

    public String build(
            List<AiReferenceDocument> documents,
            Integer requestedResultCount,
            boolean additionalResults,
            InfoSubCategory category
    ) {
        return build(
                documents, requestedResultCount, additionalResults,
                category, null, null);
    }

    public String build(
            List<AiReferenceDocument> documents,
            Integer requestedResultCount,
            boolean additionalResults,
            InfoSubCategory category,
            AiSearchScope searchScope,
            String priorityRegion
    ) {
        List<AiReferenceDocument> safeDocuments = documents == null ? List.of() : documents;
        ResultLabel label = resultLabel(category);
        StringBuilder answer = new StringBuilder(countMessage(
                requestedResultCount, safeDocuments, additionalResults, label,
                searchScope, priorityRegion));
        safeDocuments.forEach(document -> appendDocument(answer, document));
        answer.append("\n\n방문 전 운영 여부, 이용 대상 및 신청 방법은 해당 기관에 직접 확인해 주세요.");
        return answer.toString();
    }

    private String countMessage(
            Integer requestedResultCount,
            List<AiReferenceDocument> documents,
            boolean additionalResults,
            ResultLabel label,
            AiSearchScope searchScope,
            String priorityRegion
    ) {
        int actualCount = documents.size();
        if (requestedResultCount == null) {
            String region = commonRegion(documents);
            String target = (region == null ? "" : region + " ")
                    + label.name() + " 목록";
            return additionalResults
                    ? "이전에 안내한 항목을 제외하고 " + target
                            + "을 추가로 안내드립니다."
                    : target + "을 안내드립니다.";
        }
        String region = commonRegion(documents);
        String target = (region == null ? "" : region + " ") + label.name();
        if (requestedResultCount > maxResultCount) {
            String exclusion = additionalResults
                    ? " 이전에 안내한 항목을 제외하고" : "";
            return "한 번에 최대 " + maxResultCount + label.unit()
                    + "까지 안내할 수 있어," + exclusion
                    + " 현재 보듬에서 확인 가능한 " + target + " "
                    + actualCount + label.unit()
                    + (additionalResults ? "을 추가로 안내드립니다." : "을 안내드립니다.");
        }
        String mixedRegionMessage = mixedRegionMessage(
                documents, requestedResultCount, additionalResults,
                label, searchScope, priorityRegion);
        if (mixedRegionMessage != null) {
            return mixedRegionMessage;
        }
        if (requestedResultCount != null && actualCount < requestedResultCount) {
            String exclusion = additionalResults
                    ? " 이전에 안내한 항목을 제외하고" : "";
            return "요청하신 " + requestedResultCount + label.unit() + " 중"
                    + exclusion + " 현재 보듬에서 확인 가능한 " + target + " "
                    + actualCount + label.unit() + "을 안내드립니다.";
        }
        String exclusion = additionalResults
                ? " 이전에 안내한 항목을 제외하고" : "";
        return "요청하신 개수에 맞춰" + exclusion
                + " 현재 보듬에서 확인 가능한 " + target + " "
                + actualCount + label.unit()
                + (additionalResults ? "을 추가로 안내드립니다." : "을 안내드립니다.");
    }

    private String mixedRegionMessage(
            List<AiReferenceDocument> documents,
            Integer requestedResultCount,
            boolean additionalResults,
            ResultLabel label,
            AiSearchScope searchScope,
            String priorityRegion
    ) {
        if (requestedResultCount == null
                || requestedResultCount > maxResultCount
                || additionalResults
                || searchScope != AiSearchScope.REGION_PRIORITY
                || priorityRegion == null || priorityRegion.isBlank()) {
            return null;
        }
        long priorityCount = documents.stream()
                .filter(document -> belongsToRegion(document, priorityRegion))
                .count();
        long supplementalCount = documents.size() - priorityCount;
        if (supplementalCount == 0) {
            return null;
        }
        if (priorityCount == 0) {
            if (documents.size() < requestedResultCount) {
                return shortRegion(priorityRegion) + "에서 확인 가능한 " + label.name()
                        + "를 찾지 못해, 요청하신 " + requestedResultCount + label.unit()
                        + " 중 현재 보듬에서 확인 가능한 다른 지역의 " + label.name() + " "
                        + supplementalCount + label.unit() + "을 안내드립니다.";
            }
            return shortRegion(priorityRegion) + "에서 확인 가능한 " + label.name()
                    + "를 찾지 못해, 요청하신 " + requestedResultCount + label.unit()
                    + "은 다른 지역의 " + label.name() + "로 안내드립니다.";
        }
        if (documents.size() < requestedResultCount) {
            return "요청하신 " + requestedResultCount + label.unit()
                    + " 중 현재 보듬에서 확인 가능한 " + shortRegion(priorityRegion)
                    + " " + label.name() + "는 " + priorityCount + label.unit()
                    + "입니다. 다른 지역의 " + label.name() + " "
                    + supplementalCount + label.unit()
                    + "으로 보충했지만, 현재 총 " + documents.size()
                    + label.unit() + "만 확인했습니다.";
        }
        return "요청하신 " + requestedResultCount + label.unit()
                + " 중 현재 보듬에서 확인 가능한 " + shortRegion(priorityRegion)
                + " " + label.name() + "는 " + priorityCount + label.unit()
                + "입니다. 부족한 " + supplementalCount + label.unit()
                + "은 다른 지역의 " + label.name() + "로 보충했습니다.";
    }

    private boolean belongsToRegion(AiReferenceDocument document, String region) {
        if (document == null || document.content() == null) {
            return false;
        }
        String normalizedRegion = region.replaceAll("\\s+", "");
        String normalizedContent = document.content().replaceAll("\\s+", "");
        return normalizedContent.contains(normalizedRegion);
    }

    private String shortRegion(String region) {
        String[] parts = region.trim().split("\\s+");
        return parts[parts.length - 1];
    }

    private String commonRegion(List<AiReferenceDocument> documents) {
        Set<String> regions = documents.stream()
                .flatMap(document -> document.content() == null
                        ? java.util.stream.Stream.empty()
                        : document.content().lines())
                .map(String::trim)
                .filter(line -> line.startsWith("지역:"))
                .map(line -> line.substring("지역:".length()).trim())
                .filter(region -> !region.isBlank())
                .collect(Collectors.toSet());
        return regions.size() == 1 ? regions.iterator().next() : null;
    }

    private void appendDocument(StringBuilder answer, AiReferenceDocument document) {
        String title = document.title() == null || document.title().isBlank()
                ? "기관 정보" : document.title().trim();
        answer.append("\n\n**").append(title).append("**");
        String details = visibleDetails(document.content());
        if (!details.isBlank()) {
            answer.append('\n').append(details);
        }
    }

    private String visibleDetails(String content) {
        if (content == null || content.isBlank()) {
            return "";
        }
        return content.lines()
                .map(String::trim)
                .filter(line -> !line.isBlank())
                .filter(line -> INTERNAL_METADATA_LABELS.stream()
                        .noneMatch(label -> line.startsWith(label + ":")))
                .distinct()
                .collect(Collectors.joining("\n"));
    }

    private ResultLabel resultLabel(InfoSubCategory category) {
        if (category == null) {
            return new ResultLabel("관련 기관", "곳");
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
            default -> new ResultLabel("관련 기관", "곳");
        };
    }

    private record ResultLabel(String name, String unit) {
    }
}
