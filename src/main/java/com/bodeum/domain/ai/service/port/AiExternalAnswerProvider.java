package com.bodeum.domain.ai.service.port;

import com.bodeum.domain.ai.model.rag.AiUserProfile;
import com.bodeum.domain.ai.model.answer.ExternalAiAnswer;
import java.util.Set;
import java.util.List;

public interface AiExternalAnswerProvider {

    ExternalAiAnswer search(String question, AiUserProfile profile);

    default ExternalAiAnswer searchWithinDomains(
            String question,
            AiUserProfile profile,
            Set<String> allowedDomains
    ) {
        return search(question, profile);
    }

    default ExternalAiAnswer searchWithinSources(
            String question,
            AiUserProfile profile,
            Set<String> allowedDomains,
            List<String> preferredUrls
    ) {
        return searchWithinDomains(question, profile, allowedDomains);
    }
}
