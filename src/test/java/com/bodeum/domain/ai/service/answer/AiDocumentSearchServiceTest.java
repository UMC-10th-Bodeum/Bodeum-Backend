package com.bodeum.domain.ai.service.answer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bodeum.domain.ai.enums.AiSearchScope;
import com.bodeum.domain.ai.infrastructure.retrieval.AiReferenceDocumentResolver;
import com.bodeum.domain.ai.model.rag.AiReferenceDocument;
import com.bodeum.domain.ai.model.rag.AiRequiredConcept;
import com.bodeum.domain.ai.service.port.AiDocumentRetriever;
import java.util.List;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;

class AiDocumentSearchServiceTest {

    private final AiDocumentRetriever documentRetriever = mock(AiDocumentRetriever.class);
    private final AiReferenceDocumentResolver referenceDocumentResolver =
            mock(AiReferenceDocumentResolver.class);
    private final AiDocumentSearchService service = new AiDocumentSearchService(
            documentRetriever, referenceDocumentResolver, 10, 3);

    @Test
    void limitsSupplementalConceptSearches() {
        List<AiRequiredConcept> concepts = IntStream.rangeClosed(1, 5)
                .mapToObj(index -> new AiRequiredConcept(
                        "필수 개념 " + index,
                        "보충 검색어 " + index,
                        List.of("일치어-" + index),
                        List.of()
                ))
                .toList();
        when(documentRetriever.retrieve(any(), any(), any())).thenReturn(List.of());
        when(referenceDocumentResolver.resolve(any())).thenReturn(List.of());

        List<AiReferenceDocument> result = service.retrieve(
                "원본 질문",
                List.of(),
                "검색 목표",
                concepts,
                null,
                AiSearchScope.GENERAL
        );

        assertThat(result).isEmpty();
        verify(documentRetriever, times(4))
                .retrieve(any(), any(), eq(AiSearchScope.GENERAL));
    }
}
