package com.bodeum.domain.ai.service.context;

import com.bodeum.domain.ai.enums.AiSearchScope;
import com.bodeum.domain.ai.model.context.AiAdditionalResultsContext;
import com.bodeum.domain.ai.model.context.AiSearchQueryContext;
import com.bodeum.domain.ai.model.rag.AiUserProfile;
import java.util.ArrayList;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

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
        String question = appendRequestedResultCount(
                contextualizeLocalRegion(resolvedQuestion, profile, searchScope),
                requestedResultCount);
        question = appendAdditionalResultsContext(question, additionalResults);
        List<String> queries = contextualizeLocalRegions(
                retrievalQueries, profile, searchScope).stream()
                .map(query -> appendRequestedResultCount(query, requestedResultCount))
                .map(query -> appendAdditionalResultsContext(query, additionalResults))
                .toList();
        return new AiSearchQueryContext(
                question,
                ensureBroaderDisabilityTargetQuery(question, queries, profile, searchScope));
    }

    private String appendRequestedResultCount(String question, Integer requestedResultCount) {
        if (requestedResultCount == null || requestedResultCount <= 0) {
            return question;
        }
        return question + "\n요청 결과 개수: "
                + Math.min(requestedResultCount, maxResultCount) + "개";
    }

    private String appendAdditionalResultsContext(
            String question,
            AiAdditionalResultsContext context
    ) {
        if (!context.isFollowUp()) {
            return question;
        }
        StringBuilder contextualized = new StringBuilder(question)
                .append("\n검색 후보 개수: ").append(maxResultCount).append("개");
        if (!context.excludedTitles().isEmpty()) {
            contextualized.append("\n이전에 안내하여 제외할 기관: ")
                    .append(String.join(", ", context.excludedTitles()));
        }
        return contextualized.toString();
    }

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
                if (searchScope == AiSearchScope.NATIONAL_POLICY
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

    private String contextualizeLocalRegion(
            String query,
            AiUserProfile profile,
            AiSearchScope searchScope
    ) {
        if (searchScope != AiSearchScope.LOCAL_RESOURCE || profile == null
                || profile.region() == null || profile.region().isBlank()) {
            return query;
        }
        return query.replace("우리 지역", profile.region())
                .replace("우리 동네", profile.region())
                .replace("근처", profile.region())
                .replace("주변", profile.region());
    }
}
