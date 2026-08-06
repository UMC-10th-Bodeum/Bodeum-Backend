package com.bodeum.domain.community.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(properties = "bodeum.auth.jwt-secret=test-jwt-secret-32-bytes-minimum-value")
@AutoConfigureMockMvc
class PostImageSecurityIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void anonymousUserCannotUploadPostImage() throws Exception {
        MockMultipartFile image = new MockMultipartFile(
                "image",
                "post-image.png",
                "image/png",
                new byte[]{1, 2, 3}
        );

        mockMvc.perform(multipart("/api/v1/community/posts/images").file(image))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTH401_1"));
    }
}
