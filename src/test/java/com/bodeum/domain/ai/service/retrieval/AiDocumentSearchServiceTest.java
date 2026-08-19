package com.bodeum.domain.ai.service.retrieval;

import com.bodeum.domain.ai.service.validation.AiAnswerEvidenceService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bodeum.domain.ai.model.question.AiSearchScope;
import com.bodeum.domain.ai.enums.AiResponseSourceType;
import com.bodeum.domain.ai.infrastructure.retrieval.AiReferenceDocumentResolver;
import com.bodeum.domain.ai.model.context.AiAdditionalResultsContext;
import com.bodeum.domain.ai.model.rag.AiReferenceDocument;
import com.bodeum.domain.ai.model.rag.AiRequiredConcept;
import com.bodeum.domain.ai.model.rag.AiSourceKey;
import com.bodeum.domain.ai.service.port.AiDocumentRetriever;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.stream.IntStream;
import org.mockito.ArgumentCaptor;
import org.junit.jupiter.api.Test;

class AiDocumentSearchServiceTest {

    private final AiDocumentRetriever documentRetriever = mock(AiDocumentRetriever.class);
    private final AiReferenceDocumentResolver referenceDocumentResolver =
            mock(AiReferenceDocumentResolver.class);
    private final AiAnswerEvidenceService evidenceService =
            mock(AiAnswerEvidenceService.class);
    private final AiDocumentSearchService service = new AiDocumentSearchService(
            documentRetriever, referenceDocumentResolver, 5, 10, 3, evidenceService);

    @Test
    void limitsMainSearchQueriesUsingConfiguredCount() {
        service.configureMaxQueryCount(2);
        when(documentRetriever.retrieve(any(), any(), any())).thenReturn(List.of());
        when(referenceDocumentResolver.resolve(any())).thenReturn(List.of());

        service.retrieve(
                "원본 질문", List.of("확장 질문 1", "확장 질문 2"),
                null, List.of(), null, AiSearchScope.REGION_PRIORITY);

        verify(documentRetriever, times(2))
                .retrieve(any(), any(), eq(AiSearchScope.REGION_PRIORITY));
    }

    @Test
    void rejectsOutOfRangeMainSearchQueryCount() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> service.configureMaxQueryCount(0))
                .withMessageContaining("1 이상 3 이하");
        assertThatIllegalArgumentException()
                .isThrownBy(() -> service.configureMaxQueryCount(4))
                .withMessageContaining("1 이상 3 이하");
    }

    @Test
    void usesDefaultFiveDocumentsWhenResultCountIsNotRequested() {
        List<AiReferenceDocument> documents = IntStream.rangeClosed(1, 10)
                .mapToObj(index -> document("DOC-" + index, index))
                .toList();
        when(documentRetriever.retrieve(any(), any(), any())).thenReturn(documents);
        when(referenceDocumentResolver.resolve(any()))
                .thenAnswer(invocation -> invocation.getArgument(0));

        List<AiReferenceDocument> result = service.retrieve(
                "특수학교를 알려줘", List.of(), null, List.of(), null,
                AiSearchScope.REGION_PRIORITY, null, AiAdditionalResultsContext.empty());

        assertThat(result).hasSize(5);
    }

    @Test
    void capsConfiguredDefaultCountAtMaximumResultCount() {
        AiDocumentSearchService cappedService = new AiDocumentSearchService(
                documentRetriever, referenceDocumentResolver, 20, 10, 3, evidenceService);
        List<AiReferenceDocument> documents = IntStream.rangeClosed(1, 15)
                .mapToObj(index -> document("DOC-" + index, index))
                .toList();
        when(documentRetriever.retrieve(any(), any(), any())).thenReturn(documents);
        when(referenceDocumentResolver.resolve(any()))
                .thenAnswer(invocation -> invocation.getArgument(0));

        List<AiReferenceDocument> result = cappedService.retrieve(
                "특수학교를 알려줘", List.of(), null, List.of(), null,
                AiSearchScope.REGION_PRIORITY, null, AiAdditionalResultsContext.empty());

        assertThat(result).hasSize(10);
    }

    @Test
    void retrievesBroaderCandidatesBeforeExcludingPreviousResults() {
        AiReferenceDocument previous = document("OLD", 1L);
        List<AiReferenceDocument> newDocuments = IntStream.rangeClosed(2, 21)
                .mapToObj(index -> document("NEW-" + index, index))
                .toList();
        List<AiReferenceDocument> candidates = java.util.stream.Stream.concat(
                java.util.stream.Stream.of(previous), newDocuments.stream()).toList();
        when(documentRetriever.retrieve(any(), any(), any(), any(Integer.class)))
                .thenReturn(candidates);
        when(referenceDocumentResolver.resolve(any()))
                .thenAnswer(invocation -> invocation.getArgument(0));
        AiAdditionalResultsContext additionalContext = new AiAdditionalResultsContext(
                "재활센터 알려줘",
                Set.of(new AiSourceKey(AiResponseSourceType.INFO, 1L)),
                List.of("기존 센터"),
                Set.of());

        List<AiReferenceDocument> result = service.retrieve(
                "재활센터 더 알려줘\n이전에 안내하여 제외할 기관: 기존 센터",
                List.of(), null, List.of(), null, AiSearchScope.LOCAL_ONLY,
                10, additionalContext);

        ArgumentCaptor<Integer> candidateCount = ArgumentCaptor.forClass(Integer.class);
        verify(documentRetriever).retrieve(
                eq("재활센터 더 알려줘"), any(),
                eq(AiSearchScope.LOCAL_ONLY), candidateCount.capture());
        assertThat(candidateCount.getValue()).isEqualTo(30);
        assertThat(result).hasSize(10).doesNotContain(previous);
    }

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
                AiSearchScope.REGION_PRIORITY
        );

        assertThat(result).isEmpty();
        verify(documentRetriever, times(4))
                .retrieve(any(), any(), eq(AiSearchScope.REGION_PRIORITY));
    }

    private AiReferenceDocument document(String key, long sourceId) {
        return new AiReferenceDocument(
                key, key, AiResponseSourceType.INFO, sourceId,
                key, "https://example.com/" + sourceId, Instant.now());
    }
}
