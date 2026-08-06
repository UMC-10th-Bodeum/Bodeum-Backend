package com.bodeum.domain.community.controller;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.bodeum.domain.auth.enums.SocialProvider;
import com.bodeum.global.auth.AuthUserPrincipal;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest(properties = "bodeum.auth.jwt-secret=test-jwt-secret-32-bytes-minimum-value")
@AutoConfigureMockMvc
@Transactional
class PostListControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void getPostsAllowsAnonymousUser() throws Exception {
        mockMvc.perform(get("/api/v1/community/posts"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.content").isArray());
    }

    @Test
    void getPostsRejectsNegativePage() throws Exception {
        mockMvc.perform(get("/api/v1/community/posts")
                        .param("page", "-1")
                        .with(authentication(userAuthentication())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("COMMON400_1"));
    }

    @Test
    void getPostsRejectsUnsupportedSort() throws Exception {
        mockMvc.perform(get("/api/v1/community/posts")
                        .param("sort", "latest")
                        .with(authentication(userAuthentication())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("COMMUNITY400_7"));
    }

    private UsernamePasswordAuthenticationToken userAuthentication() {
        AuthUserPrincipal principal = new AuthUserPrincipal(
                10L,
                SocialProvider.KAKAO,
                "보듬맘",
                "user@example.com"
        );
        return new UsernamePasswordAuthenticationToken(
                principal,
                null,
                List.of(new SimpleGrantedAuthority("ROLE_USER"))
        );
    }
}
