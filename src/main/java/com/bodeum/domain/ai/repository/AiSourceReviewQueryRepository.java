package com.bodeum.domain.ai.repository;

import com.bodeum.domain.ai.model.rag.AiSourceKey;
import java.util.Collection;

public interface AiSourceReviewQueryRepository {

    boolean existsWarningRequiredBySources(Collection<AiSourceKey> sourceKeys);
}
