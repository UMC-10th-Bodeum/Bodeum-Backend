package com.bodeum.domain.ai.infrastructure.external;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bodeum.domain.ai.entity.AiExternalDocument;
import com.bodeum.domain.ai.entity.AiExternalSource;
import com.bodeum.domain.ai.exception.AiErrorCode;
import com.bodeum.domain.ai.repository.AiExternalDocumentRepository;
import com.bodeum.global.apiPayload.exception.ProjectException;
import java.util.Collection;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.jdbc.core.JdbcTemplate;

class AiExternalDocumentPersistenceServiceTest {

    private AiExternalDocumentRepository externalDocumentRepository;
    private JdbcTemplate jdbcTemplate;
    private AiExternalDocumentPersistenceService persistenceService;
    private AiExternalSource externalSource;

    @BeforeEach
    void setUp() {
        externalDocumentRepository = mock(AiExternalDocumentRepository.class);
        jdbcTemplate = mock(JdbcTemplate.class);
        persistenceService = new AiExternalDocumentPersistenceService(
                externalDocumentRepository, jdbcTemplate);
        externalSource = mock(AiExternalSource.class);
        when(externalSource.getId()).thenReturn(1L);
    }

    @Test
    void upsertsAllCandidatesAndLoadsSavedResourcesOnce() {
        AiExternalDocument existing = AiExternalDocument.create(
                externalSource,
                "변경된 제목",
                "https://www.kpat.or.kr/updated",
                "existing-hash",
                null
        );
        AiExternalDocument created = AiExternalDocument.create(
                externalSource,
                "신규 페이지",
                "https://www.kpat.or.kr/new",
                "new-hash",
                null
        );
        List<AiExternalDocumentCandidate> candidates = List.of(
                new AiExternalDocumentCandidate(
                        externalSource,
                        "변경된 제목",
                        "https://www.kpat.or.kr/updated",
                        "existing-hash"
                ),
                new AiExternalDocumentCandidate(
                        externalSource,
                        "신규 페이지",
                        "https://www.kpat.or.kr/new",
                        "new-hash"
                )
        );

        when(externalDocumentRepository.findAllBySourceUrlHashIn(any()))
                .thenReturn(List.of(created, existing));

        List<AiExternalDocument> saved = persistenceService.saveAll(candidates);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<Object[]>> batchCaptor = ArgumentCaptor.forClass(List.class);
        verify(jdbcTemplate, times(1)).batchUpdate(anyString(), batchCaptor.capture());
        @SuppressWarnings("unchecked")
        ArgumentCaptor<Collection<String>> hashesCaptor = ArgumentCaptor.forClass(Collection.class);
        verify(externalDocumentRepository, times(1))
                .findAllBySourceUrlHashIn(hashesCaptor.capture());

        assertThat(batchCaptor.getValue()).hasSize(2);
        assertThat(batchCaptor.getValue().getFirst())
                .containsExactly(1L, "변경된 제목",
                        "https://www.kpat.or.kr/updated", "existing-hash");
        assertThat(hashesCaptor.getValue())
                .containsExactlyInAnyOrder("existing-hash", "new-hash");
        assertThat(saved).hasSize(2);
        assertThat(saved)
                .extracting(AiExternalDocument::getSourceUrlHash)
                .containsExactly("existing-hash", "new-hash");
    }

    @Test
    void throwsProjectExceptionWhenUpsertedDocumentCannotBeLoaded() {
        AiExternalDocumentCandidate candidate = new AiExternalDocumentCandidate(
                externalSource,
                "신규 페이지",
                "https://www.kpat.or.kr/new",
                "new-hash"
        );
        when(externalDocumentRepository.findAllBySourceUrlHashIn(any()))
                .thenReturn(List.of());

        assertThatThrownBy(() -> persistenceService.saveAll(List.of(candidate)))
                .isInstanceOf(ProjectException.class)
                .extracting(exception -> ((ProjectException) exception).getErrorCode())
                .isEqualTo(AiErrorCode.AI_RESPONSE_FAILED);
    }
}
