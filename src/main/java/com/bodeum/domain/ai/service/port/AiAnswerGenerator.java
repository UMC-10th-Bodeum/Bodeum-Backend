package com.bodeum.domain.ai.service.port;

import com.bodeum.domain.ai.model.rag.AiReferenceDocument;
import com.bodeum.domain.ai.model.rag.AiUserProfile;
import com.bodeum.domain.ai.model.answer.GeneratedAiAnswer;
import java.util.List;

public interface AiAnswerGenerator {
    GeneratedAiAnswer generate(String question, AiUserProfile profile, List<AiReferenceDocument> documents);
}
