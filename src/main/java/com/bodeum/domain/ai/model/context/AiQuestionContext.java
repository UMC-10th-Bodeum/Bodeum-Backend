package com.bodeum.domain.ai.model.context;

import com.bodeum.domain.ai.model.question.AiSearchScope;
import com.bodeum.domain.ai.model.question.AiCuratedAnswerType;
import com.bodeum.domain.ai.model.question.AiResultType;
import com.bodeum.domain.ai.model.rag.AiRequiredConcept;
import com.bodeum.domain.ai.model.rag.AiUserProfile;
import java.util.List;
import java.util.Optional;

public record AiQuestionContext(
        AiUserProfile userProfile,
        AiUserProfile searchProfile,
        Optional<AiCuratedAnswerType> curatedAnswerType,
        Optional<String> safetyGuidance,
        AiSearchScope searchScope,
        List<String> retrievalQueries,
        Integer requestedResultCount,
        String resolvedQuestion,
        String searchGoal,
        List<AiRequiredConcept> requiredConcepts,
        boolean needsClarification,
        String clarificationQuestion,
        AiResolvedContext resolvedContext,
        AiResultType resultType,
        boolean referencesPreviousContext,
        boolean excludePreviousResults
) {
    public AiQuestionContext {
        resultType = resultType == null ? AiResultType.DOCUMENT_ANSWER : resultType;
        excludePreviousResults = referencesPreviousContext && excludePreviousResults;
    }

    public boolean followUp() {
        return referencesPreviousContext;
    }

    public boolean isSiteListRequest() {
        return resultType == AiResultType.SITE_LIST;
    }

    public boolean isResourceListRequest() {
        return resultType == AiResultType.RESOURCE_LIST;
    }
}
