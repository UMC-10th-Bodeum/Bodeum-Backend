package com.bodeum.domain.ai.infrastructure.external;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bodeum.domain.ai.entity.AiExternalResource;
import com.bodeum.domain.ai.entity.AiExternalSource;
import com.bodeum.domain.ai.repository.AiExternalResourceRepository;
import java.util.Collection;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.jdbc.core.JdbcTemplate;

class AiExternalResourcePersistenceServiceTest {

    private AiExternalResourceRepository externalResourceRepository;
    private JdbcTemplate jdbcTemplate;
    private AiExternalResourcePersistenceService persistenceService;
    private AiExternalSource externalSource;

    @BeforeEach
    void setUp() {
        externalResourceRepository = mock(AiExternalResourceRepository.class);
        jdbcTemplate = mock(JdbcTemplate.class);
        persistenceService = new AiExternalResourcePersistenceService(
                externalResourceRepository, jdbcTemplate);
        externalSource = mock(AiExternalSource.class);
        when(externalSource.getId()).thenReturn(1L);
    }

    @Test
    void upsertsAllCandidatesAndLoadsSavedResourcesOnce() {
        AiExternalResource existing = AiExternalResource.create(
                externalSource,
                "변경된 제목",
                "https://www.kpat.or.kr/updated",
                "existing-hash",
                null
        );
        AiExternalResource created = AiExternalResource.create(
                externalSource,
                "신규 페이지",
                "https://www.kpat.or.kr/new",
                "new-hash",
                null
        );
        List<AiExternalResourceCandidate> candidates = List.of(
                new AiExternalResourceCandidate(
                        externalSource,
                        "변경된 제목",
                        "https://www.kpat.or.kr/updated",
                        "existing-hash"
                ),
                new AiExternalResourceCandidate(
                        externalSource,
                        "신규 페이지",
                        "https://www.kpat.or.kr/new",
                        "new-hash"
                )
        );

        when(externalResourceRepository.findAllBySourceUrlHashIn(any()))
                .thenReturn(List.of(created, existing));

        List<AiExternalResource> saved = persistenceService.saveAll(candidates);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<Object[]>> batchCaptor = ArgumentCaptor.forClass(List.class);
        verify(jdbcTemplate, times(1)).batchUpdate(anyString(), batchCaptor.capture());
        @SuppressWarnings("unchecked")
        ArgumentCaptor<Collection<String>> hashesCaptor = ArgumentCaptor.forClass(Collection.class);
        verify(externalResourceRepository, times(1))
                .findAllBySourceUrlHashIn(hashesCaptor.capture());

        assertThat(batchCaptor.getValue()).hasSize(2);
        assertThat(batchCaptor.getValue().getFirst())
                .containsExactly(1L, "변경된 제목",
                        "https://www.kpat.or.kr/updated", "existing-hash");
        assertThat(hashesCaptor.getValue())
                .containsExactlyInAnyOrder("existing-hash", "new-hash");
        assertThat(saved).hasSize(2);
        assertThat(saved)
                .extracting(AiExternalResource::getSourceUrlHash)
                .containsExactly("existing-hash", "new-hash");
    }
}
