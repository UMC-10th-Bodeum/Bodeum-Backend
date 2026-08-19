package com.bodeum.domain.ai.infrastructure.retrieval;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bodeum.domain.ai.enums.AiResponseSourceType;
import com.bodeum.domain.ai.model.question.AiSearchScope;
import com.bodeum.domain.ai.model.rag.AiUserProfile;
import com.bodeum.domain.info.entity.enums.InfoSubCategory;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStoreRetriever;

@ExtendWith(MockitoExtension.class)
class SpringAiDocumentRetrieverTest {

    @Mock
    private VectorStoreRetriever vectorStoreRetriever;

    @Test
    void usesTypedCandidateCountForAdditionalResultSearch() {
        SpringAiDocumentRetriever retriever =
                new SpringAiDocumentRetriever(vectorStoreRetriever, 5, 10, 0.4);
        AiUserProfile profile = new AiUserProfile(
                "경기도 수원시", "경기도", "수원시",
                null, List.of(), List.of(), null);
        when(vectorStoreRetriever.similaritySearch(
                org.mockito.ArgumentMatchers.any(SearchRequest.class)))
                .thenReturn(List.of());

        retriever.retrieve(
                "재활센터 더 알려줘", profile, AiSearchScope.LOCAL_ONLY, 30);

        ArgumentCaptor<SearchRequest> requestCaptor =
                ArgumentCaptor.forClass(SearchRequest.class);
        verify(vectorStoreRetriever, times(3)).similaritySearch(requestCaptor.capture());
        assertThat(requestCaptor.getAllValues())
                .extracting(SearchRequest::getTopK)
                .containsOnly(30);
    }

    @Test
    void includesRegionAsPriorityContextWithoutAddingRegionFilterToGeneralSearch() {
        SpringAiDocumentRetriever retriever =
                new SpringAiDocumentRetriever(vectorStoreRetriever, 5, 10, 0.7);
        AiUserProfile profile = new AiUserProfile(
                "서울 강남구",
                "서울",
                "강남구",
                6,
                List.of("AUTISM_SPECTRUM"),
                List.of("HOSPITAL_HEALTH"),
                "언어치료, 사회성 발달"
        );
        when(vectorStoreRetriever.similaritySearch(org.mockito.ArgumentMatchers.any(SearchRequest.class)))
                .thenReturn(List.of());

        retriever.retrieve("복지 센터 알려줘", profile);

        ArgumentCaptor<SearchRequest> requestCaptor = ArgumentCaptor.forClass(SearchRequest.class);
        verify(vectorStoreRetriever, times(2)).similaritySearch(requestCaptor.capture());
        assertThat(requestCaptor.getAllValues()).extracting(SearchRequest::getQuery)
                .contains("복지 센터 알려줘");
        assertThat(requestCaptor.getAllValues().getFirst().getQuery())
                .contains("복지 센터 알려줘")
                .contains("집중 케어 영역: AUTISM_SPECTRUM")
                .contains("관심사: HOSPITAL_HEALTH")
                .contains("자녀 관련 관심 키워드: 언어치료, 사회성 발달")
                .contains("활동 지역: 서울 강남구");
        assertThat(requestCaptor.getAllValues())
                .extracting(SearchRequest::getFilterExpression)
                .containsOnlyNulls();
    }

    @Test
    void prioritizesExactProfileRegionDocumentsInNationwideGeneralSearch() {
        SpringAiDocumentRetriever retriever =
                new SpringAiDocumentRetriever(vectorStoreRetriever, 5, 10, 0.4);
        AiUserProfile profile = new AiUserProfile(
                "경기도 수원시",
                "경기도",
                "수원시",
                6,
                List.of(),
                List.of(),
                ""
        );
        List<Document> personalizedDocuments = List.of(
                document("SUWON-1", 0.7, "경기도", "수원시"),
                document("SEOUL-1", 0.9, "서울특별시", "강남구")
        );
        List<Document> questionDocuments = List.of(
                document("BUSAN-1", 0.95, "부산광역시", "남구"),
                document("SUWON-2", 0.6, "경기도", "수원시")
        );
        when(vectorStoreRetriever.similaritySearch(
                org.mockito.ArgumentMatchers.any(SearchRequest.class)))
                .thenReturn(personalizedDocuments, questionDocuments);

        var result = retriever.retrieve(
                "특수학교를 알려줘",
                profile,
                AiSearchScope.REGION_PRIORITY
        );

        assertThat(result)
                .extracting(document -> document.documentKey())
                .containsExactly("SUWON-1", "SUWON-2", "BUSAN-1", "SEOUL-1");
    }

    @Test
    void expandsLocalInstitutionSearchFromSigunguToSidoAndAll() {
        SpringAiDocumentRetriever retriever =
                new SpringAiDocumentRetriever(vectorStoreRetriever, 5, 10, 0.7);
        AiUserProfile profile = new AiUserProfile(
                "경기도 수원시",
                "경기도",
                "수원시",
                6,
                List.of(),
                List.of(),
                ""
        );
        when(vectorStoreRetriever.similaritySearch(
                org.mockito.ArgumentMatchers.any(SearchRequest.class)))
                .thenReturn(List.of());

        retriever.retrieve("우리 지역 복지센터 알려줘", profile,
                AiSearchScope.LOCAL_ONLY);

        ArgumentCaptor<SearchRequest> requestCaptor = ArgumentCaptor.forClass(SearchRequest.class);
        verify(vectorStoreRetriever, times(3)).similaritySearch(requestCaptor.capture());
        assertThat(requestCaptor.getAllValues().get(0).getFilterExpression().toString())
                .contains("경기도", "수원시");
        assertThat(requestCaptor.getAllValues().get(1).getFilterExpression().toString())
                .contains("경기도")
                .doesNotContain("수원시");
        assertThat(requestCaptor.getAllValues().get(2).getFilterExpression()).isNull();
    }

    @Test
    void doesNotAddProfileRegionToNationalPolicySearchQuery() {
        SpringAiDocumentRetriever retriever =
                new SpringAiDocumentRetriever(vectorStoreRetriever, 5, 10, 0.7);
        AiUserProfile profile = new AiUserProfile(
                "경기도 수원시",
                "경기도",
                "수원시",
                6,
                List.of(),
                List.of(),
                ""
        );
        when(vectorStoreRetriever.similaritySearch(
                org.mockito.ArgumentMatchers.any(SearchRequest.class)))
                .thenReturn(List.of());

        retriever.retrieve("장애인 활동지원서비스 신청 방법", profile,
                AiSearchScope.NATIONWIDE);

        ArgumentCaptor<SearchRequest> requestCaptor = ArgumentCaptor.forClass(SearchRequest.class);
        verify(vectorStoreRetriever).similaritySearch(requestCaptor.capture());
        assertThat(requestCaptor.getAllValues())
                .extracting(SearchRequest::getQuery)
                .allMatch(query -> !query.contains("경기도 수원시"));
    }

    @Test
    void prioritizesQuestionDocumentsBeforePersonalizedDocuments() {
        SpringAiDocumentRetriever retriever =
                new SpringAiDocumentRetriever(vectorStoreRetriever, 5, 10, 0.4);
        AiUserProfile profile = new AiUserProfile(
                "경기도 수원시",
                "경기도",
                "수원시",
                6,
                List.of("AUTISM_SPECTRUM"),
                List.of("EDUCATION"),
                "특수교육"
        );
        List<Document> personalizedDocuments = List.of(
                document("PERSONALIZED-1", 0.9),
                document("PERSONALIZED-2", 0.8),
                document("PERSONALIZED-3", 0.7),
                document("PERSONALIZED-4", 0.65),
                document("PERSONALIZED-5", 0.61)
        );
        List<Document> questionDocuments = List.of(
                document("QUESTION-1", 0.6),
                document("QUESTION-2", 0.55),
                document("QUESTION-3", 0.5),
                document("QUESTION-4", 0.45),
                document("QUESTION-5", 0.4)
        );
        when(vectorStoreRetriever.similaritySearch(
                org.mockito.ArgumentMatchers.any(SearchRequest.class)))
                .thenReturn(personalizedDocuments, questionDocuments);

        var result = retriever.retrieve(
                "수원시 특수학교를 알려줘",
                profile,
                AiSearchScope.LOCAL_ONLY
        );

        assertThat(result)
                .extracting(document -> document.documentKey())
                .containsExactly(
                        "QUESTION-1",
                        "QUESTION-2",
                        "QUESTION-3",
                        "QUESTION-4",
                        "QUESTION-5"
                );
    }

    @Test
    void expandsTopKWhenQuestionRequestsTenInstitutions() {
        SpringAiDocumentRetriever retriever =
                new SpringAiDocumentRetriever(vectorStoreRetriever, 5, 10, 0.4);
        AiUserProfile profile = new AiUserProfile(
                "경기도 수원시",
                "경기도",
                "수원시",
                6,
                List.of(),
                List.of(),
                ""
        );
        List<Document> documents = java.util.stream.IntStream.rangeClosed(1, 10)
                .mapToObj(index -> document("CENTER-" + index, 0.9 - index * 0.01))
                .toList();
        when(vectorStoreRetriever.similaritySearch(
                org.mockito.ArgumentMatchers.any(SearchRequest.class)))
                .thenReturn(documents, documents);

        var result = retriever.retrieve(
                "근처 장애인재활센터 10개 알려줘",
                profile,
                AiSearchScope.LOCAL_ONLY
        );

        ArgumentCaptor<SearchRequest> requestCaptor =
                ArgumentCaptor.forClass(SearchRequest.class);
        verify(vectorStoreRetriever).similaritySearch(requestCaptor.capture());
        assertThat(requestCaptor.getAllValues())
                .extracting(SearchRequest::getTopK)
                .containsOnly(10);
        assertThat(result).hasSize(10);
    }

    @Test
    void keepsDefaultTopKAsCandidateMinimumWhenQuestionRequestsThreeInstitutions() {
        SpringAiDocumentRetriever retriever =
                new SpringAiDocumentRetriever(vectorStoreRetriever, 5, 10, 0.4);
        AiUserProfile profile = new AiUserProfile(
                "", "", "", 6, List.of(), List.of(), "");
        when(vectorStoreRetriever.similaritySearch(
                org.mockito.ArgumentMatchers.any(SearchRequest.class)))
                .thenReturn(List.of());

        retriever.retrieve(
                "재활센터 3개 알려줘",
                profile,
                AiSearchScope.REGION_PRIORITY
        );

        ArgumentCaptor<SearchRequest> requestCaptor =
                ArgumentCaptor.forClass(SearchRequest.class);
        verify(vectorStoreRetriever).similaritySearch(requestCaptor.capture());
        assertThat(requestCaptor.getAllValues())
                .extracting(SearchRequest::getTopK)
                .containsOnly(5);
    }

    @Test
    void capsTopKAtTenWhenQuestionRequestsMoreThanTenInstitutions() {
        SpringAiDocumentRetriever retriever =
                new SpringAiDocumentRetriever(vectorStoreRetriever, 5, 10, 0.4);
        AiUserProfile profile = new AiUserProfile(
                "경기도 수원시",
                "경기도",
                "수원시",
                6,
                List.of(),
                List.of(),
                ""
        );
        when(vectorStoreRetriever.similaritySearch(
                org.mockito.ArgumentMatchers.any(SearchRequest.class)))
                .thenReturn(List.of());

        retriever.retrieve(
                "근처 장애인재활센터 100개 알려줘",
                profile,
                AiSearchScope.LOCAL_ONLY
        );

        ArgumentCaptor<SearchRequest> requestCaptor =
                ArgumentCaptor.forClass(SearchRequest.class);
        verify(vectorStoreRetriever, times(3)).similaritySearch(requestCaptor.capture());
        assertThat(requestCaptor.getAllValues())
                .extracting(SearchRequest::getTopK)
                .containsOnly(10);
    }

    @Test
    void capsTopKWhenRequestedCountExceedsIntegerRange() {
        SpringAiDocumentRetriever retriever =
                new SpringAiDocumentRetriever(vectorStoreRetriever, 5, 10, 0.4);
        AiUserProfile profile = new AiUserProfile(
                "", "", "", 6, List.of(), List.of(), "");
        when(vectorStoreRetriever.similaritySearch(
                org.mockito.ArgumentMatchers.any(SearchRequest.class)))
                .thenReturn(List.of());

        retriever.retrieve(
                "재활센터 999999999999999999999개 알려줘",
                profile,
                AiSearchScope.REGION_PRIORITY
        );

        ArgumentCaptor<SearchRequest> requestCaptor =
                ArgumentCaptor.forClass(SearchRequest.class);
        verify(vectorStoreRetriever).similaritySearch(requestCaptor.capture());
        assertThat(requestCaptor.getAllValues())
                .extracting(SearchRequest::getTopK)
                .containsOnly(10);
    }

    @Test
    void combinesRegionAndSubCategoryFiltersForLocalVectorSearch() {
        SpringAiDocumentRetriever retriever =
                new SpringAiDocumentRetriever(vectorStoreRetriever, 5, 10, 0.4);
        AiUserProfile profile = new AiUserProfile(
                "경기도 수원시", "경기도", "수원시", 6,
                List.of(), List.of(), ""
        ).withInfoSubCategory(InfoSubCategory.SPECIAL_SCHOOL);
        when(vectorStoreRetriever.similaritySearch(
                org.mockito.ArgumentMatchers.any(SearchRequest.class)))
                .thenReturn(List.of());

        retriever.retrieve(
                "경기도 수원시 특수학교를 알려줘",
                profile,
                AiSearchScope.LOCAL_ONLY
        );

        ArgumentCaptor<SearchRequest> requestCaptor =
                ArgumentCaptor.forClass(SearchRequest.class);
        verify(vectorStoreRetriever, times(3)).similaritySearch(requestCaptor.capture());
        assertThat(requestCaptor.getAllValues().get(0).getFilterExpression().toString())
                .contains("경기도", "수원시", "SPECIAL_SCHOOL");
        assertThat(requestCaptor.getAllValues().get(1).getFilterExpression().toString())
                .contains("경기도", "SPECIAL_SCHOOL")
                .doesNotContain("수원시");
        assertThat(requestCaptor.getAllValues().get(2).getFilterExpression().toString())
                .contains("SPECIAL_SCHOOL");
    }

    private Document document(String id, double score) {
        return document(id, score, null, null);
    }

    private Document document(
            String id,
            double score,
            String sido,
            String sigungu
    ) {
        Document document = org.mockito.Mockito.mock(Document.class);
        when(document.getId()).thenReturn(id);
        lenient().when(document.getText()).thenReturn(id + " content");
        when(document.getScore()).thenReturn(score);
        Map<String, Object> metadata = new java.util.HashMap<>();
        metadata.put("sourceType", AiResponseSourceType.INFO.name());
        metadata.put("sourceId", Integer.toString(Math.abs(id.hashCode())));
        metadata.put("title", id);
        metadata.put("sourceUrl", "https://example.com/" + id);
        metadata.put("updatedAt", Instant.parse("2026-07-01T00:00:00Z").toString());
        if (sido != null) {
            metadata.put("sido", sido);
        }
        if (sigungu != null) {
            metadata.put("sigungu", sigungu);
        }
        lenient().when(document.getMetadata()).thenReturn(metadata);
        return document;
    }

}
