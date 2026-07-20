package com.bodeum.domain.ai.infrastructure.retrieval;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bodeum.domain.ai.model.rag.AiUserProfile;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStoreRetriever;

@ExtendWith(MockitoExtension.class)
class SpringAiDocumentRetrieverTest {

    @Mock
    private VectorStoreRetriever vectorStoreRetriever;

    @Test
    void includesUserProfileContextInVectorSearchQuery() {
        SpringAiDocumentRetriever retriever =
                new SpringAiDocumentRetriever(vectorStoreRetriever, 5, 0.7);
        AiUserProfile profile = new AiUserProfile(
                "서울 강남구",
                6,
                List.of("AUTISM_SPECTRUM"),
                List.of("HOSPITAL_HEALTH"),
                "언어치료, 사회성 발달"
        );
        when(vectorStoreRetriever.similaritySearch(org.mockito.ArgumentMatchers.any(SearchRequest.class)))
                .thenReturn(List.of());

        retriever.retrieve("복지 센터 알려줘", profile);

        ArgumentCaptor<SearchRequest> requestCaptor = ArgumentCaptor.forClass(SearchRequest.class);
        verify(vectorStoreRetriever).similaritySearch(requestCaptor.capture());
        assertThat(requestCaptor.getValue().getQuery())
                .contains("복지 센터 알려줘")
                .contains("활동 지역: 서울 강남구")
                .contains("장애 유형: AUTISM_SPECTRUM")
                .contains("관심사: HOSPITAL_HEALTH")
                .contains("자녀 관련 관심 키워드: 언어치료, 사회성 발달");
    }
}
