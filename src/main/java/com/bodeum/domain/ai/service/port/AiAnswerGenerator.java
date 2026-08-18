package com.bodeum.domain.ai.service.port;

import com.bodeum.domain.ai.model.rag.AiReferenceDocument;
import com.bodeum.domain.ai.model.rag.AiUserProfile;
import com.bodeum.domain.ai.model.answer.GeneratedAiAnswer;
import com.bodeum.domain.ai.model.context.AiResolvedContext;
import java.util.List;

/**
 * 질문 문맥과 검색 근거를 사용해 최종 AI 답변을 생성하는 포트이다.
 */
public interface AiAnswerGenerator {
    GeneratedAiAnswer generate(String question, AiUserProfile profile, List<AiReferenceDocument> documents);

    default GeneratedAiAnswer generate(
            String originalQuestion,
            String resolvedQuestion,
            AiResolvedContext resolvedContext,
            String searchRegion,
            AiUserProfile userProfile,
            List<AiReferenceDocument> documents
    ) {
        return generate(resolvedQuestion, userProfile, documents);
    }
}
