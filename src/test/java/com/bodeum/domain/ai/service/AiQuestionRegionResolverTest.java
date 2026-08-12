package com.bodeum.domain.ai.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.bodeum.domain.ai.model.rag.AiUserProfile;
import com.bodeum.domain.region.entity.Region;
import com.bodeum.domain.region.repository.RegionRepository;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AiQuestionRegionResolverTest {

    @Mock
    private RegionRepository regionRepository;

    @InjectMocks
    private AiQuestionRegionResolver resolver;

    @Test
    void resolvesGwangjuDistrictWithinGwangjuMetropolitanCity() {
        Region gwangjuBukgu = Region.create("광주광역시", "북구");
        Region busanBukgu = Region.create("부산광역시", "북구");
        when(regionRepository.findAllByRegionLevel2OrderByIdAsc("북구"))
                .thenReturn(List.of(busanBukgu, gwangjuBukgu));

        var result = resolver.resolve(
                "광주 북구 특수학교를 알려줘",
                profile("경기도", "수원시")
        );

        assertThat(result.isResolved()).isTrue();
        assertThat(result.region()).isSameAs(gwangjuBukgu);
    }

    @Test
    void keepsBareGwangjuAmbiguousWhenSubregionIsNotSpecified() {
        var result = resolver.resolve(
                "광주 특수학교를 알려줘",
                profile("경기도", "수원시")
        );

        assertThat(result.isAmbiguous()).isTrue();
        assertThat(result.candidates())
                .containsExactly("광주광역시", "경기도 광주시");
    }

    private AiUserProfile profile(String regionLevel1, String regionLevel2) {
        return new AiUserProfile(
                regionLevel1 + " " + regionLevel2,
                regionLevel1,
                regionLevel2,
                null,
                List.of(),
                List.of(),
                ""
        );
    }
}
