package com.bodeum.domain.ai.service.port;

import com.bodeum.domain.ai.model.rag.AiUserProfile;
import com.bodeum.domain.ai.model.answer.ExternalAiAnswer;

public interface AiExternalAnswerProvider {

    ExternalAiAnswer search(String question, AiUserProfile profile);
}
