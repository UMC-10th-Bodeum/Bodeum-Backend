package com.bodeum.domain.region.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

import com.bodeum.domain.region.repository.RegionRepository;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RegionServiceTest {

    @Mock
    private RegionRepository regionRepository;

    private RegionService regionService;

    @BeforeEach
    void setUp() {
        regionService = new RegionService(regionRepository);
    }

    @Test
    void regionIdTakesPriorityOverRegionLevel1() {
        ResolvedRegionFilter result = regionService.resolveFilter(10L, "경기도");

        assertThat(result.applied()).isTrue();
        assertThat(result.regionIds()).containsExactly(10L);
    }

    @Test
    void regionLevel1ResolvesAllChildRegionIds() {
        given(regionRepository.findIdsByRegionLevel1("경기도"))
                .willReturn(List.of(10L, 11L, 12L));

        ResolvedRegionFilter result = regionService.resolveFilter(null, " 경기도 ");

        assertThat(result.applied()).isTrue();
        assertThat(result.regionIds()).containsExactly(10L, 11L, 12L);
    }

    @Test
    void noSelectionDoesNotApplyRegionFilter() {
        ResolvedRegionFilter result = regionService.resolveFilter(null, null);

        assertThat(result.applied()).isFalse();
        assertThat(result.regionIds()).isEmpty();
    }
}
