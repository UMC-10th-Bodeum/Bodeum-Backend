package com.bodeum.domain.ai.infrastructure.external;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bodeum.domain.ai.entity.AiExternalResource;
import com.bodeum.domain.ai.entity.AiExternalSource;
import com.bodeum.domain.ai.enums.AiExternalSourceType;
import com.bodeum.domain.ai.enums.AiSourceAuthorityLevel;
import com.bodeum.domain.ai.repository.AiExternalResourceRepository;
import java.util.Collection;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class AiExternalResourcePersistenceServiceTest {

    private AiExternalResourceRepository externalResourceRepository;
    private AiExternalResourcePersistenceService persistenceService;
    private AiExternalSource externalSource;

    @BeforeEach
    void setUp() {
        externalResourceRepository = mock(AiExternalResourceRepository.class);
        persistenceService = new AiExternalResourcePersistenceService(externalResourceRepository);
        externalSource = AiExternalSource.create(
                "한국장애인부모회",
                AiExternalSourceType.WEBSITE,
                "https://www.kpat.or.kr/",
                null,
                "장애인 가족 지원 정보를 제공하는 사이트",
                AiSourceAuthorityLevel.NONPROFIT_ORGANIZATION
        );
    }

    @Test
    void loadsExistingResourcesOnceAndSavesAllCandidatesTogether() {
        AiExternalResource existing = AiExternalResource.create(
                externalSource,
                "기존 제목",
                "https://www.kpat.or.kr/old",
                "existing-hash",
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
                .thenReturn(List.of(existing));
        when(externalResourceRepository.saveAll(any()))
                .thenAnswer(invocation -> invocation.getArgument(0));

        List<AiExternalResource> saved = persistenceService.saveAll(candidates);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Collection<String>> hashesCaptor = ArgumentCaptor.forClass(Collection.class);
        verify(externalResourceRepository, times(1))
                .findAllBySourceUrlHashIn(hashesCaptor.capture());
        verify(externalResourceRepository, times(1)).saveAll(any());

        assertThat(hashesCaptor.getValue())
                .containsExactlyInAnyOrder("existing-hash", "new-hash");
        assertThat(saved).hasSize(2);
        assertThat(existing.getTitle()).isEqualTo("변경된 제목");
        assertThat(existing.getSourceUrl()).isEqualTo("https://www.kpat.or.kr/updated");
        assertThat(saved)
                .extracting(AiExternalResource::getSourceUrlHash)
                .containsExactlyInAnyOrder("existing-hash", "new-hash");
    }
}
