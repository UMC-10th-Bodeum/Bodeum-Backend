package com.bodeum.domain.ai.model.context;

import com.bodeum.domain.ai.enums.AiSearchScope;
import com.bodeum.domain.ai.enums.AiStarterQuestionType;
import com.bodeum.domain.ai.model.rag.AiRequiredConcept;
import com.bodeum.domain.ai.model.rag.AiUserProfile;
import java.util.List;
import java.util.Optional;

public record AiQuestionContext(
        AiUserProfile profile,
        Optional<AiStarterQuestionType> questionType,
        Optional<String> safetyGuidance,
        AiSearchScope searchScope,
        List<String> retrievalQueries,
        Integer requestedResultCount,
        String resolvedQuestion,
        boolean followUp,
        String searchGoal,
        List<AiRequiredConcept> requiredConcepts,
        boolean needsClarification,
        String clarificationQuestion,
        AiResolvedContext resolvedContext,
        boolean siteListRequest,
        boolean resourceListRequest
) {
}
