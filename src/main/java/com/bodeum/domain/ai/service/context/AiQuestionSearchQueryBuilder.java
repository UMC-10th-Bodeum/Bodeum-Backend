package com.bodeum.domain.ai.service.context;

import com.bodeum.domain.ai.model.question.AiSearchScope;
import com.bodeum.domain.ai.model.context.AiAdditionalResultsContext;
import com.bodeum.domain.ai.model.context.AiSearchQueryContext;
import com.bodeum.domain.ai.model.rag.AiUserProfile;
import java.util.ArrayList;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * 사용자 지역, 요청 결과 개수, 후속 질문 문맥을 반영하여 최종 검색 쿼리를 생성한다.
 */
@Component
public class AiQuestionSearchQueryBuilder {

    @Value("${bodeum.ai.result.max-count:10}")
    private int maxResultCount = 10;

    public AiSearchQueryContext build(
            String resolvedQuestion,
            List<String> retrievalQueries,
            AiUserProfile profile,
            AiSearchScope searchScope,
            Integer requestedResultCount,
            AiAdditionalResultsContext additionalResults
    ) {
        // 사용자 지역과 요청 결과 개수를 최종 질문에 반영
        String question = appendRequestedResultCount(
                contextualizeLocalRegion(resolvedQuestion, profile, searchScope),
                requestedResultCount);

        // 추가 결과 요청이면 이전에 안내한 결과를 제외하도록 검색 문맥 추가
        question = appendAdditionalResultsContext(question, additionalResults);

        // 검색 후보 쿼리에도 동일한 지역/개수/추가 결과 문맥을 적용
        List<String> queries = contextualizeLocalRegions(
                retrievalQueries, profile, searchScope).stream()
                .map(query -> appendRequestedResultCount(query, requestedResultCount))
                .map(query -> appendAdditionalResultsContext(query, additionalResults))
                .toList();
        return new AiSearchQueryContext(
                question,
                ensureBroaderDisabilityTargetQuery(question, queries, profile, searchScope));
    }

    /**
     * 사용자가 요청한 결과 개수를 검색 질문에 추가하되,
     * 설정된 최대 결과 개수를 초과하지 않도록 제한한다.
     */
    private String appendRequestedResultCount(String question, Integer requestedResultCount) {
        if (requestedResultCount == null || requestedResultCount <= 0) {
            return question;
        }
        return question + "\n요청 결과 개수: "
                + Math.min(requestedResultCount, maxResultCount) + "개";
    }

    /**
     * 추가 결과 요청인 경우, 이전에 안내한 결과를 제외할 수 있도록
     * 외부 검색에서 사용할 제외 대상 제목을 질문에 추가한다.
     * 내부 벡터 검색의 후보 개수는 검색 서비스에 숫자 인자로 전달한다.
     */
    private String appendAdditionalResultsContext(
            String question,
            AiAdditionalResultsContext context
    ) {
        if (!context.isFollowUp()) {
            return question;
        }
        StringBuilder contextualized = new StringBuilder(question);
        if (!context.excludedTitles().isEmpty()) {
            contextualized.append("\n이전에 안내하여 제외할 기관: ")
                    .append(String.join(", ", context.excludedTitles()));
        }
        return contextualized.toString();
    }

    /**
     * 검색 후보 목록에 사용자 지역 정보를 일괄 반영한다.
     */
    private List<String> contextualizeLocalRegions(
            List<String> queries,
            AiUserProfile profile,
            AiSearchScope searchScope
    ) {
        if (queries == null) {
            return List.of();
        }
        return queries.stream()
                .map(query -> contextualizeLocalRegion(query, profile, searchScope)).toList();
    }

    /**
     * "장애 아동" 검색어를 "장애인"으로 확장하여
     * 대상 표현 차이로 인해 관련 검색 결과가 누락되지 않도록 한다.
     */
    private List<String> ensureBroaderDisabilityTargetQuery(
            String question,
            List<String> queries,
            AiUserProfile profile,
            AiSearchScope searchScope
    ) {
        List<String> expanded = new ArrayList<>();
        if (question != null) {
            String broaderTargetQuery = question.replaceAll("장애\\s*아동", "장애인");
            if (!broaderTargetQuery.equals(question)) {
                expanded.add(broaderTargetQuery);

                // 국가 활동지원 정책 검색 시 사용자 지역을 포함한 확장 쿼리도 추가
                if (searchScope == AiSearchScope.NATIONWIDE
                        && question.replaceAll("\\s+", "").contains("활동지원")
                        && profile != null && profile.region() != null
                        && !profile.region().isBlank()) {
                    expanded.add(profile.region() + " " + broaderTargetQuery);
                }
            }
        }
        if (queries != null) {
            expanded.addAll(queries);
        }
        return expanded.stream().filter(query -> query != null && !query.isBlank())
                .map(String::trim).distinct().limit(3).toList();
    }

    /**
     * 지역 기반 검색일 때 "우리 지역", "근처" 등의 표현을
     * 사용자 프로필의 실제 지역명으로 치환한다.
     */
    private String contextualizeLocalRegion(
            String query,
            AiUserProfile profile,
            AiSearchScope searchScope
    ) {
        if (searchScope != AiSearchScope.LOCAL_ONLY || profile == null
                || profile.region() == null || profile.region().isBlank()) {
            return query;
        }
        return query.replace("우리 지역", profile.region())
                .replace("우리 동네", profile.region())
                .replace("근처", profile.region())
                .replace("주변", profile.region());
    }
}
