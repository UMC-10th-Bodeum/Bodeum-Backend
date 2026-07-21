package com.bodeum.domain.ai.infrastructure.indexing;

import com.bodeum.domain.ai.enums.AiResponseSourceType;
import java.util.Set;

public interface AiIndexedDocumentReader {

    Set<String> findIds(AiResponseSourceType sourceType, Long sourceId);
}
