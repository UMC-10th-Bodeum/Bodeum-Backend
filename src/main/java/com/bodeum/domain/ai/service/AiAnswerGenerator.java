package com.bodeum.domain.ai.service;

import com.bodeum.domain.ai.model.AiReferenceDocument;
import com.bodeum.domain.ai.model.AiUserProfile;
import com.bodeum.domain.ai.model.GeneratedAiAnswer;
import java.util.List;

public interface AiAnswerGenerator {
    GeneratedAiAnswer generate(String question, AiUserProfile profile, List<AiReferenceDocument> documents);
}
