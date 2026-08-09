package com.bodeum.domain.ai.service.port;

import com.bodeum.domain.ai.model.rag.AiQuestionAnalysis;

public interface AiQuestionIntentClassifier {

    AiQuestionAnalysis analyze(String question);
}
