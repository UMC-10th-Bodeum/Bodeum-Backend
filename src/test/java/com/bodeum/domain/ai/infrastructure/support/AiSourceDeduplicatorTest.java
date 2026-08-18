package com.bodeum.domain.ai.infrastructure.support;

import static org.assertj.core.api.Assertions.assertThat;

import com.bodeum.domain.ai.enums.AiResponseSourceType;
import com.bodeum.domain.ai.model.rag.AiReferenceDocument;
import java.util.List;
import org.junit.jupiter.api.Test;

class AiSourceDeduplicatorTest {

    @Test
    void removesSameNormalizedUrlButKeepsDifferentPaths() {
        List<AiReferenceDocument> result = AiSourceDeduplicator.deduplicate(List.of(
                source("A", 1L, "https://www.heart4u.or.kr/"),
                source("B", 2L, "https://heart4u.or.kr"),
                source("C", 3L, "https://heart4u.or.kr/program/motor"),
                source("D", 4L, "https://heart4u.or.kr/program/sensory")
        ));

        assertThat(result)
                .extracting(AiReferenceDocument::documentKey)
                .containsExactly("A", "C", "D");
    }

    @Test
    void removesSameSourceIdentifierEvenWhenUrlDiffers() {
        List<AiReferenceDocument> result = AiSourceDeduplicator.deduplicate(List.of(
                source("A", 1L, "https://example.com/first"),
                source("B", 1L, "https://example.com/second")
        ));

        assertThat(result).extracting(AiReferenceDocument::documentKey)
                .containsExactly("A");
    }

    private AiReferenceDocument source(String key, Long id, String url) {
        return new AiReferenceDocument(
                key, key, AiResponseSourceType.SITE, id, key, url, null);
    }
}
