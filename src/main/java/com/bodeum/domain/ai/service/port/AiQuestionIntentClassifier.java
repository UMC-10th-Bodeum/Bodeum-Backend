package com.bodeum.domain.ai.service.port;

import com.bodeum.domain.ai.model.rag.AiQuestionAnalysis;
import com.bodeum.domain.ai.model.context.AiResolvedContext;

public interface AiQuestionIntentClassifier {

    AiQuestionAnalysis analyze(String question);

    default AiQuestionAnalysis analyze(
            String question,
            String previousUserQuestion,
            String previousAiAnswer
    ) {
        return analyze(question);
    }

    default AiQuestionAnalysis analyze(
            String question,
            String previousUserQuestion,
            String previousAiAnswer,
            AiResolvedContext previousResolvedContext
    ) {
        return analyze(question, previousUserQuestion, previousAiAnswer);
    }
}
