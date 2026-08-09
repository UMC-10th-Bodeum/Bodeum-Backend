package com.bodeum.domain.ai.service.port;

import com.bodeum.domain.ai.model.rag.AiUserProfile;
import com.bodeum.domain.ai.model.answer.ExternalAiAnswer;
import java.util.List;

public interface AiExternalAnswerProvider {

    ExternalAiAnswer search(
            String question,
            List<String> retrievalQueries,
            AiUserProfile profile
    );
}
