package com.bodeum.domain.ai.service.port;

import com.bodeum.domain.ai.model.question.AiSearchScope;
import com.bodeum.domain.ai.model.rag.AiReferenceDocument;
import com.bodeum.domain.ai.model.rag.AiUserProfile;
import java.util.List;

/**
 * 검색 범위와 사용자 문맥에 맞는 RAG 근거 문서를 조회하는 포트이다.
 */
public interface AiDocumentRetriever {
    List<AiReferenceDocument> retrieve(
            String question,
            AiUserProfile profile,
            AiSearchScope searchScope
    );

    default List<AiReferenceDocument> retrieve(
            String question,
            AiUserProfile profile,
            AiSearchScope searchScope,
            int candidateCount
    ) {
        return retrieve(question, profile, searchScope);
    }

    default List<AiReferenceDocument> retrieve(String question, AiUserProfile profile) {
        return retrieve(question, profile, AiSearchScope.REGION_PRIORITY);
    }
}
