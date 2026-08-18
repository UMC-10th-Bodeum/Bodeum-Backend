package com.bodeum.domain.ai.service.response;

import com.bodeum.domain.ai.model.rag.AiReferenceDocument;
import com.bodeum.domain.info.entity.enums.InfoSubCategory;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

/**
 * 구조화된 기관 검색 결과를 LLM 없이 사용자에게 보여줄 목록 답변으로 구성한다.
 */
@Component
public class AiResourceListAnswerBuilder {

    private static final Set<String> INTERNAL_METADATA_LABELS = Set.of(
            "정보명", "대분류", "세부 분류");

    public String build(
            List<AiReferenceDocument> documents,
            Integer requestedResultCount,
            boolean additionalResults,
            InfoSubCategory category
    ) {
        List<AiReferenceDocument> safeDocuments = documents == null ? List.of() : documents;
        ResultLabel label = resultLabel(category);
        StringBuilder answer = new StringBuilder(countMessage(
                requestedResultCount, safeDocuments.size(), additionalResults, label));
        safeDocuments.forEach(document -> appendDocument(answer, document));
        answer.append("\n\n방문 전 운영 여부, 이용 대상 및 신청 방법은 해당 기관에 직접 확인해 주세요.");
        return answer.toString();
    }

    private String countMessage(
            Integer requestedResultCount,
            int actualCount,
            boolean additionalResults,
            ResultLabel label
    ) {
        String availability = additionalResults
                ? "이전에 안내한 항목을 제외하고 현재 보듬에서 추가로 확인 가능한 "
                : "현재 보듬에서 확인 가능한 ";
        if (requestedResultCount != null && actualCount < requestedResultCount) {
            return "요청하신 " + requestedResultCount + label.unit() + " 중 "
                    + availability + label.subject() + " "
                    + actualCount + label.unit() + "입니다.";
        }
        return availability + label.subject() + " " + actualCount + label.unit() + "입니다.";
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
            return new ResultLabel("관련 기관은", "곳");
        }
        return switch (category) {
            case PRIMARY_CARE -> new ResultLabel("건강주치의 기관은", "곳");
            case EMERGENCY_CLINIC -> new ResultLabel("응급의료기관은", "곳");
            case THERAPY_REHAB -> new ResultLabel("치료·재활기관은", "곳");
            case WELFARE_CENTER -> new ResultLabel("장애인복지관은", "곳");
            case FAMILY_SUPPORT -> new ResultLabel("장애인가족지원센터는", "곳");
            case SPECIAL_SCHOOL -> new ResultLabel("특수학교는", "곳");
            case SPECIAL_EDU_SUPPORT -> new ResultLabel("특수교육지원센터는", "곳");
            case LIFELONG_EDU -> new ResultLabel("장애인 평생교육기관은", "곳");
            case STANDARD_WORKPLACE -> new ResultLabel("장애인표준사업장은", "곳");
            case REALTIME_JOB -> new ResultLabel("구인 정보는", "건");
            case PRIVATE_WELFARE, NATIONAL_WELFARE, LOCAL_WELFARE ->
                    new ResultLabel("복지 서비스는", "개");
            default -> new ResultLabel("관련 기관은", "곳");
        };
    }

    private record ResultLabel(String subject, String unit) {
    }
}
