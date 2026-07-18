package com.bodeum.domain.community.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.bodeum.domain.community.dto.request.CreatePostRequest;
import com.bodeum.domain.community.dto.request.UpdatePostRequest;
import com.bodeum.domain.community.dto.response.PostResponse;
import com.bodeum.domain.community.enums.DisabilityType;
import com.bodeum.domain.community.enums.PostAnonymityType;
import com.bodeum.domain.community.enums.PostBoardType;
import com.bodeum.domain.community.exception.CommunityErrorCode;
import com.bodeum.domain.community.exception.CommunityException;
import com.bodeum.domain.community.service.PostService;
import com.bodeum.global.apiPayload.handler.GeneralExceptionAdvice;
import com.bodeum.global.auth.LoginUser;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@ExtendWith(MockitoExtension.class)
class PostControllerTest {

    @Mock
    private PostService postService;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();
        objectMapper = new ObjectMapper().findAndRegisterModules();
        mockMvc = MockMvcBuilders.standaloneSetup(new PostController(postService))
                .setControllerAdvice(new GeneralExceptionAdvice())
                .setCustomArgumentResolvers(loginUserArgumentResolver())
                .setValidator(validator)
                .build();
    }

    @Test
    void createPostReturnsCreatedResponse() throws Exception {
        given(postService.createPost(any(), any(CreatePostRequest.class))).willReturn(postResponse());

        mockMvc.perform(post("/api/community/posts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "boardType": "FREE_COMMUNICATION",
                                  "anonymityType": "PROFILE_TAG_VISIBLE",
                                  "title": "게시글 제목",
                                  "content": "게시글 내용",
                                  "disabilityTypes": ["AUTISM"],
                                  "hashtags": ["육아"],
                                  "imageUrls": ["https://example.com/image.jpg"]
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.isSuccess").value(true))
                .andExpect(jsonPath("$.code").value("COMMON201_1"))
                .andExpect(jsonPath("$.result.postId").value(1));

        then(postService).should().createPost(any(), any(CreatePostRequest.class));
    }

    @Test
    void createPostRejectsBlankTitle() throws Exception {
        mockMvc.perform(post("/api/community/posts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "boardType": "FREE_COMMUNICATION",
                                  "anonymityType": "PROFILE_TAG_VISIBLE",
                                  "title": " ",
                                  "content": "게시글 내용"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("COMMON400_1"));

        then(postService).should(never()).createPost(any(), any(CreatePostRequest.class));
    }

    @Test
    void createPostRejectsNullDisabilityType() throws Exception {
        mockMvc.perform(post("/api/community/posts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "boardType": "FREE_COMMUNICATION",
                                  "anonymityType": "PROFILE_TAG_VISIBLE",
                                  "title": "게시글 제목",
                                  "content": "게시글 내용",
                                  "disabilityTypes": [null]
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("COMMON400_1"));

        then(postService).should(never()).createPost(any(), any(CreatePostRequest.class));
    }

    @Test
    void updatePostUsesPostPath() throws Exception {
        given(postService.updatePost(any(), any(), any(UpdatePostRequest.class))).willReturn(postResponse());

        mockMvc.perform(patch("/api/community/posts/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "수정된 게시글 제목"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.postId").value(1));
    }

    @Test
    void deletePostUsesPostPath() throws Exception {
        mockMvc.perform(delete("/api/community/posts/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isSuccess").value(true));

        then(postService).should().deletePost(any(), any());
    }

    @Test
    void getPostReturnsDetailResponse() throws Exception {
        given(postService.getPost(10L, 1L)).willReturn(postResponse());

        mockMvc.perform(get("/api/community/posts/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.title").value("게시글 제목"))
                .andExpect(jsonPath("$.result.isMine").value(true))
                .andExpect(jsonPath("$.result.disabilityTypes[0]").value("AUTISM"));
    }

    @Test
    void getPostReturnsCommunityNotFoundError() throws Exception {
        given(postService.getPost(10L, 99L))
                .willThrow(new CommunityException(CommunityErrorCode.POST_NOT_FOUND));

        mockMvc.perform(get("/api/community/posts/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("COMMUNITY404_1"));
    }

    private HandlerMethodArgumentResolver loginUserArgumentResolver() {
        return new HandlerMethodArgumentResolver() {
            @Override
            public boolean supportsParameter(MethodParameter parameter) {
                return parameter.hasParameterAnnotation(LoginUser.class);
            }

            @Override
            public Object resolveArgument(
                    MethodParameter parameter,
                    ModelAndViewContainer mavContainer,
                    NativeWebRequest webRequest,
                    WebDataBinderFactory binderFactory
            ) {
                return 10L;
            }
        };
    }

    private PostResponse postResponse() {
        return new PostResponse(
                1L,
                10L,
                true,
                PostBoardType.FREE_COMMUNICATION,
                PostAnonymityType.PROFILE_TAG_VISIBLE,
                "게시글 제목",
                "게시글 내용",
                List.of(DisabilityType.AUTISM),
                List.of("육아"),
                List.of("https://example.com/image.jpg"),
                Instant.parse("2026-07-18T00:00:00Z"),
                Instant.parse("2026-07-18T00:00:00Z")
        );
    }
}
