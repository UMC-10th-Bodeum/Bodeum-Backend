package com.bodeum.domain.ai.service.port;

import com.bodeum.domain.ai.enums.AiSearchScope;
import com.bodeum.domain.ai.model.rag.AiReferenceDocument;
import com.bodeum.domain.ai.model.rag.AiUserProfile;
import java.util.List;

public interface AiDocumentRetriever {
    List<AiReferenceDocument> retrieve(
            String question,
            AiUserProfile profile,
            AiSearchScope searchScope
    );

    default List<AiReferenceDocument> retrieve(String question, AiUserProfile profile) {
        return retrieve(question, profile, AiSearchScope.GENERAL);
    }
}
