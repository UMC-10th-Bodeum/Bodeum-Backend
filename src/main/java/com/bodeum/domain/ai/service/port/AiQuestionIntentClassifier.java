package com.bodeum.domain.ai.service.port;

import com.bodeum.domain.ai.model.rag.AiQuestionAnalysis;
import com.bodeum.domain.ai.model.context.AiResolvedContext;

/**
 * 현재 질문과 대화 문맥을 분석해 의도, 검색 범위 및 구조화 문맥을 반환하는 포트이다.
 */
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

    default AiQuestionAnalysis analyze(
            String question,
            String previousUserQuestion,
            String previousAiAnswer,
            AiResolvedContext previousResolvedContext,
            String profileRegion
    ) {
        return analyze(
                question, previousUserQuestion, previousAiAnswer, previousResolvedContext);
    }

    default AiQuestionAnalysis analyze(
            String question,
            String recentConversation,
            String previousUserQuestion,
            String previousAiAnswer,
            AiResolvedContext previousResolvedContext,
            String profileRegion
    ) {
        return analyze(
                question, previousUserQuestion, previousAiAnswer,
                previousResolvedContext, profileRegion);
    }
}
