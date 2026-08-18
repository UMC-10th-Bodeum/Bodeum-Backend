package com.bodeum.domain.ai.service.port;

import com.bodeum.domain.ai.model.question.AiSearchScope;
import com.bodeum.domain.ai.model.rag.AiUserProfile;
import com.bodeum.domain.ai.model.answer.ExternalAiAnswer;
import java.util.List;

/**
 * 내부 근거가 부족할 때 외부 검색으로 답변과 출처를 조회하는 포트이다.
 */
public interface AiExternalAnswerProvider {

    ExternalAiAnswer search(
            String question,
            List<String> retrievalQueries,
            AiUserProfile profile,
            AiSearchScope searchScope
    );
}
