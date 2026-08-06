package com.bodeum.domain.region.controller;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.bodeum.domain.region.dto.response.RegionResponse;
import com.bodeum.domain.region.service.RegionService;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@ExtendWith(MockitoExtension.class)
class RegionControllerTest {

    @Mock
    private RegionService regionService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new RegionController(regionService)).build();
    }

    @Test
    void getRegionsReturnsRegionList() throws Exception {
        given(regionService.getRegions()).willReturn(List.of(
                new RegionResponse(1L, "서울특별시", "종로구", "서울특별시 종로구"),
                new RegionResponse(2L, "경기도", "수원시", "경기도 수원시")
        ));

        mockMvc.perform(get("/api/v1/regions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isSuccess").value(true))
                .andExpect(jsonPath("$.code").value("COMMON200_1"))
                .andExpect(jsonPath("$.result").isArray())
                .andExpect(jsonPath("$.result.length()").value(2))
                .andExpect(jsonPath("$.result[0].regionId").value(1))
                .andExpect(jsonPath("$.result[0].fullName").value("서울특별시 종로구"))
                .andExpect(jsonPath("$.result[1].regionLevel1").value("경기도"))
                .andExpect(jsonPath("$.result[1].regionLevel2").value("수원시"));
    }
}
