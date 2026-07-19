package com.bodeum.domain.ai.service;

import com.bodeum.domain.ai.model.AiUserProfile;
import com.bodeum.domain.ai.model.ExternalAiAnswer;

public interface AiExternalAnswerProvider {

    ExternalAiAnswer search(String question, AiUserProfile profile);
}
