package com.bodeum.domain.ai.service;

import com.bodeum.domain.ai.model.AiReferenceDocument;
import com.bodeum.domain.ai.model.AiUserProfile;
import java.util.List;

public interface AiDocumentRetriever {
    List<AiReferenceDocument> retrieve(String question, AiUserProfile profile);
}
