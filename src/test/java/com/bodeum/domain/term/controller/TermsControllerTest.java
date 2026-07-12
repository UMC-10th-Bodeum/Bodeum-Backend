package com.bodeum.domain.term.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(properties = "bodeum.auth.jwt-secret=test-jwt-secret-32-bytes-minimum-value")
@AutoConfigureMockMvc
class TermsControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void getTermsReturnsNotionResponseShape() throws Exception {
        mockMvc.perform(get("/api/v1/terms/service"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.type").value("service"))
                .andExpect(jsonPath("$.result.title").value("서비스 이용약관"))
                .andExpect(jsonPath("$.result.content").isNotEmpty())
                .andExpect(jsonPath("$.result.updatedAt").isNotEmpty())
                .andExpect(jsonPath("$.result.version").doesNotExist())
                .andExpect(jsonPath("$.result.effectiveDate").doesNotExist())
                .andExpect(jsonPath("$.result.required").doesNotExist());
    }

    @Test
    void unsupportedTermsTypeReturnsTermsErrorCode() throws Exception {
        mockMvc.perform(get("/api/v1/terms/ai-chat"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("TERMS400_1"));
    }
}
