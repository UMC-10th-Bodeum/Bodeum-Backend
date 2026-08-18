package com.bodeum.domain.ai.model.rag;

import com.bodeum.domain.ai.model.question.AiQuestionIntent;
import com.bodeum.domain.ai.model.question.AiSearchScope;
import com.bodeum.domain.ai.model.context.AiResolvedContext;
import com.bodeum.domain.info.entity.enums.InfoSubCategory;
import java.util.List;
import java.util.stream.Stream;

public record AiQuestionAnalysis(
        AiQuestionIntent intent,
        AiSearchScope searchScope,
        List<String> retrievalQueries,
        Integer requestedResultCount,
        String resolvedQuestion,
        InfoSubCategory infoSubCategory,
        String searchGoal,
        List<AiRequiredConcept> requiredConcepts,
        boolean needsClarification,
        String clarificationQuestion,
        AiResolvedContext resolvedContext,
        boolean siteListRequest,
        boolean resourceListRequest,
        boolean referencesPreviousContext,
        boolean excludePreviousResults
) {

    public AiQuestionAnalysis(AiQuestionIntent intent, List<String> retrievalQueries) {
        this(intent, AiSearchScope.REGION_PRIORITY, retrievalQueries, null, null,
                null, null, List.of(), false, null, null, false, false,
                false, false);
    }

    public AiQuestionAnalysis(
            AiQuestionIntent intent,
            AiSearchScope searchScope,
            List<String> retrievalQueries
    ) {
        this(intent, searchScope, retrievalQueries, null, null,
                null, null, List.of(), false, null, null, false, false,
                false, false);
    }

    public AiQuestionAnalysis(
            AiQuestionIntent intent,
            AiSearchScope searchScope,
            List<String> retrievalQueries,
            Integer requestedResultCount
    ) {
        this(intent, searchScope, retrievalQueries, requestedResultCount, null,
                null, null, List.of(), false, null, null, false, false,
                false, false);
    }

    public AiQuestionAnalysis {
        intent = intent == null ? AiQuestionIntent.NONE : intent;
        searchScope = searchScope == null ? AiSearchScope.REGION_PRIORITY : searchScope;
        retrievalQueries = retrievalQueries == null
                ? List.of()
                : retrievalQueries.stream()
                        .filter(query -> query != null && !query.isBlank())
                        .map(String::trim)
                        .distinct()
                        .limit(3)
                        .toList();
        requestedResultCount = requestedResultCount != null && requestedResultCount > 0
                ? requestedResultCount
                : null;
        resolvedQuestion = resolvedQuestion == null || resolvedQuestion.isBlank()
                ? null
                : resolvedQuestion.trim();
        searchGoal = searchGoal == null || searchGoal.isBlank()
                ? null
                : searchGoal.trim();
        requiredConcepts = requiredConcepts == null
                ? List.of()
                : requiredConcepts.stream()
                        .filter(concept -> concept != null && concept.isValid())
                        .distinct()
                        .limit(3)
                        .toList();
        clarificationQuestion = clarificationQuestion == null
                || clarificationQuestion.isBlank()
                ? null
                : clarificationQuestion.trim();
        needsClarification = needsClarification && clarificationQuestion != null;
        excludePreviousResults = referencesPreviousContext && excludePreviousResults;
    }

    public static AiQuestionAnalysis fallback() {
        return new AiQuestionAnalysis(
                AiQuestionIntent.NONE, AiSearchScope.REGION_PRIORITY, List.of(), null, null,
                null, null, List.of(), false, null, null, false, false,
                false, false);
    }

    public static AiQuestionAnalysis fallback(String question) {
        return forQuestion(question, AiQuestionIntent.NONE, AiSearchScope.REGION_PRIORITY, List.of());
    }

    public static AiQuestionAnalysis forQuestion(
            String question,
            AiQuestionIntent intent,
            List<String> expandedQueries
    ) {
        return forQuestion(question, intent, AiSearchScope.REGION_PRIORITY, expandedQueries);
    }

    public static AiQuestionAnalysis forQuestion(
            String question,
            AiQuestionIntent intent,
            AiSearchScope searchScope,
            List<String> expandedQueries
    ) {
        return forQuestion(
                question, intent, searchScope, expandedQueries, null);
    }

    public static AiQuestionAnalysis forQuestion(
            String question,
            AiQuestionIntent intent,
            AiSearchScope searchScope,
            List<String> expandedQueries,
            Integer requestedResultCount
    ) {
        return forQuestion(
                question,
                intent,
                searchScope,
                expandedQueries,
                requestedResultCount,
                question,
                false
        );
    }

    public static AiQuestionAnalysis forQuestion(
            String question,
            AiQuestionIntent intent,
            AiSearchScope searchScope,
            List<String> expandedQueries,
            Integer requestedResultCount,
            String resolvedQuestion
    ) {
        return forQuestion(
                question,
                intent,
                searchScope,
                expandedQueries,
                requestedResultCount,
                resolvedQuestion,
                false
        );
    }

    public static AiQuestionAnalysis forQuestion(
            String question,
            AiQuestionIntent intent,
            AiSearchScope searchScope,
            List<String> expandedQueries,
            Integer requestedResultCount,
            String resolvedQuestion,
            boolean followUp
    ) {
        return forQuestion(question, intent, searchScope, expandedQueries,
                requestedResultCount, resolvedQuestion, followUp, null);
    }

    public static AiQuestionAnalysis forQuestion(
            String question,
            AiQuestionIntent intent,
            AiSearchScope searchScope,
            List<String> expandedQueries,
            Integer requestedResultCount,
            String resolvedQuestion,
            boolean followUp,
            InfoSubCategory infoSubCategory
    ) {
        AiQuestionIntent resolvedIntent = intent == null
                ? AiQuestionIntent.NONE
                : intent;
        if (resolvedIntent != AiQuestionIntent.NONE) {
            return new AiQuestionAnalysis(
                    resolvedIntent,
                    searchScope,
                    List.of(),
                    requestedResultCount,
                    resolvedQuestion,
                    infoSubCategory,
                    null,
                    List.of(),
                    false,
                    null,
                    null,
                    false,
                    false,
                    followUp,
                    false
            );
        }

        Stream<String> expansions = expandedQueries == null
                ? Stream.empty()
                : expandedQueries.stream();
        List<String> retrievalQueries = Stream.concat(
                        Stream.of(question),
                        expansions
                )
                .filter(query -> query != null && !query.isBlank())
                .map(String::trim)
                .distinct()
                .limit(3)
                .toList();
        return new AiQuestionAnalysis(
                resolvedIntent,
                searchScope,
                retrievalQueries,
                requestedResultCount,
                resolvedQuestion,
                infoSubCategory,
                null,
                List.of(),
                false,
                null,
                null,
                false,
                false,
                followUp,
                false
        );
    }

    public AiQuestionAnalysis withRetrievalPlan(
            String resolvedSearchGoal,
            List<AiRequiredConcept> resolvedRequiredConcepts
    ) {
        return new AiQuestionAnalysis(
                intent,
                searchScope,
                retrievalQueries,
                requestedResultCount,
                resolvedQuestion,
                infoSubCategory,
                resolvedSearchGoal,
                resolvedRequiredConcepts,
                needsClarification,
                clarificationQuestion,
                resolvedContext,
                siteListRequest,
                resourceListRequest,
                referencesPreviousContext,
                excludePreviousResults
        );
    }

    public AiQuestionAnalysis withClarification(
            boolean clarificationRequired,
            String question
    ) {
        return new AiQuestionAnalysis(
                intent,
                searchScope,
                retrievalQueries,
                requestedResultCount,
                resolvedQuestion,
                infoSubCategory,
                searchGoal,
                requiredConcepts,
                clarificationRequired,
                question,
                resolvedContext,
                siteListRequest,
                resourceListRequest,
                referencesPreviousContext,
                excludePreviousResults
        );
    }

    public AiQuestionAnalysis withResolvedContext(AiResolvedContext context) {
        return new AiQuestionAnalysis(
                intent,
                searchScope,
                retrievalQueries,
                requestedResultCount,
                resolvedQuestion,
                infoSubCategory,
                searchGoal,
                requiredConcepts,
                needsClarification,
                clarificationQuestion,
                context,
                siteListRequest,
                resourceListRequest,
                referencesPreviousContext,
                excludePreviousResults
        );
    }

    public AiQuestionAnalysis withSiteListRequest(boolean requested) {
        return new AiQuestionAnalysis(
                intent,
                searchScope,
                retrievalQueries,
                requestedResultCount,
                resolvedQuestion,
                infoSubCategory,
                searchGoal,
                requiredConcepts,
                needsClarification,
                clarificationQuestion,
                resolvedContext,
                requested,
                resourceListRequest,
                referencesPreviousContext,
                excludePreviousResults
        );
    }

    public AiQuestionAnalysis withResourceListRequest(boolean requested) {
        return new AiQuestionAnalysis(
                intent, searchScope, retrievalQueries, requestedResultCount,
                resolvedQuestion, infoSubCategory, searchGoal,
                requiredConcepts, needsClarification, clarificationQuestion,
                resolvedContext, siteListRequest, requested,
                referencesPreviousContext, excludePreviousResults);
    }

    public AiQuestionAnalysis withConversationContext(
            boolean referencesContext,
            boolean excludesPreviousResults
    ) {
        return new AiQuestionAnalysis(
                intent, searchScope, retrievalQueries, requestedResultCount,
                resolvedQuestion, infoSubCategory,
                searchGoal, requiredConcepts, needsClarification, clarificationQuestion,
                resolvedContext, siteListRequest, resourceListRequest,
                referencesContext, excludesPreviousResults);
    }

    public boolean followUp() {
        return referencesPreviousContext;
    }
}
