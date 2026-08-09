package com.bodeum.domain.community.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.bodeum.domain.community.dto.request.CreatePostRequest;
import com.bodeum.domain.community.dto.request.UpdatePostRequest;
import com.bodeum.domain.community.dto.response.PostLikeResponse;
import com.bodeum.domain.community.dto.response.PostListItemResponse;
import com.bodeum.domain.community.dto.response.PostResponse;
import com.bodeum.domain.community.dto.response.PostScrapResponse;
import com.bodeum.domain.community.enums.DisabilityType;
import com.bodeum.domain.community.enums.PostAnonymityType;
import com.bodeum.domain.community.enums.PostBoardType;
import com.bodeum.domain.community.exception.CommunityErrorCode;
import com.bodeum.domain.community.exception.CommunityException;
import com.bodeum.domain.community.service.PostListService;
import com.bodeum.domain.community.service.PostQueryFacade;
import com.bodeum.domain.community.service.PostService;
import com.bodeum.global.apiPayload.handler.GeneralExceptionAdvice;
import com.bodeum.global.auth.LoginUser;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.List;
import java.util.stream.IntStream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.MethodParameter;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
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
    @Mock
    private PostQueryFacade postQueryFacade;
    @Mock
    private PostListService postListService;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();
        objectMapper = new ObjectMapper().findAndRegisterModules();
        mockMvc = MockMvcBuilders.standaloneSetup(
                        new PostController(postService, postQueryFacade, postListService)
                )
                .setControllerAdvice(new GeneralExceptionAdvice())
                .setCustomArgumentResolvers(loginUserArgumentResolver())
                .setValidator(validator)
                .build();
    }

    @Test
    void getPostsReturnsTenItemPageWithDefaultSort() throws Exception {
        given(postListService.getPosts(10L, 0, "view", null, null))
                .willReturn(new PageImpl<>(
                        List.of(postListItemResponse()),
                        PageRequest.of(0, 10),
                        1
                ));

        mockMvc.perform(get("/api/v1/community/posts"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.content[0].postId").value(1))
                .andExpect(jsonPath("$.result.content[0].author.nickname").value("보듬맘"))
                .andExpect(jsonPath("$.result.content[0].thumbnailUrl")
                        .value("https://example.com/image.jpg"))
                .andExpect(jsonPath("$.result.content[0].isLiked").value(true))
                .andExpect(jsonPath("$.result.size").value(10))
                .andExpect(jsonPath("$.result.totalElements").value(1));

        then(postListService).should().getPosts(10L, 0, "view", null, null);
    }

    @Test
    void getPostsPassesSearchAndSortParameters() throws Exception {
        given(postListService.getPosts(
                10L,
                2,
                "comment",
                "언어치료",
                PostBoardType.TREATMENT_GROWTH_RECORD
        ))
                .willReturn(new PageImpl<>(List.of(), PageRequest.of(2, 10), 0));

        mockMvc.perform(get("/api/v1/community/posts")
                        .param("page", "2")
                        .param("sort", "comment")
                        .param("keyword", "언어치료")
                        .param("categoryCode", "TREATMENT_GROWTH_RECORD"))
                .andExpect(status().isOk());

        then(postListService).should().getPosts(
                10L,
                2,
                "comment",
                "언어치료",
                PostBoardType.TREATMENT_GROWTH_RECORD
        );
    }

    @Test
    void createPostReturnsCreatedResponse() throws Exception {
        given(postService.createPost(any(), any(CreatePostRequest.class))).willReturn(postResponse());

        mockMvc.perform(post("/api/v1/community/posts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "boardType": "INFORMATION_QUESTION",
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
                .andExpect(jsonPath("$.result.postId").value(1))
                .andExpect(jsonPath("$.result.isQuestion").value(true));

        then(postService).should().createPost(any(), any(CreatePostRequest.class));
    }

    @Test
    void createPostRejectsBlankTitle() throws Exception {
        mockMvc.perform(post("/api/v1/community/posts")
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
        mockMvc.perform(post("/api/v1/community/posts")
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
    void createPostRejectsMoreThanTenImages() throws Exception {
        List<String> imageUrls = IntStream.rangeClosed(1, 11)
                .mapToObj(index -> "https://example.com/" + index + ".jpg")
                .toList();
        CreatePostRequest request = new CreatePostRequest(
                PostBoardType.FREE_COMMUNICATION,
                PostAnonymityType.PROFILE_TAG_VISIBLE,
                "게시글 제목",
                "게시글 내용",
                List.of(DisabilityType.AUTISM),
                List.of("육아"),
                imageUrls
        );

        mockMvc.perform(post("/api/v1/community/posts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("COMMON400_1"));

        then(postService).should(never()).createPost(any(), any(CreatePostRequest.class));
    }

    @Test
    void updatePostUsesPostPath() throws Exception {
        given(postService.updatePost(any(), any(), any(UpdatePostRequest.class))).willReturn(postResponse());

        mockMvc.perform(patch("/api/v1/community/posts/1")
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
    void updatePostAcceptsTenImages() throws Exception {
        List<String> imageUrls = IntStream.rangeClosed(1, 10)
                .mapToObj(index -> "https://example.com/" + index + ".jpg")
                .toList();
        UpdatePostRequest request = new UpdatePostRequest(
                null,
                null,
                null,
                null,
                null,
                null,
                imageUrls
        );
        given(postService.updatePost(any(), any(), any(UpdatePostRequest.class))).willReturn(postResponse());

        mockMvc.perform(patch("/api/v1/community/posts/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.postId").value(1));

        then(postService).should().updatePost(any(), any(), any(UpdatePostRequest.class));
    }

    @Test
    void updatePostRejectsMoreThanTenImages() throws Exception {
        List<String> imageUrls = IntStream.rangeClosed(1, 11)
                .mapToObj(index -> "https://example.com/" + index + ".jpg")
                .toList();
        UpdatePostRequest request = new UpdatePostRequest(
                null,
                null,
                null,
                null,
                null,
                null,
                imageUrls
        );

        mockMvc.perform(patch("/api/v1/community/posts/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("COMMON400_1"));

        then(postService).should(never()).updatePost(any(), any(), any(UpdatePostRequest.class));
    }

    @Test
    void deletePostUsesPostPath() throws Exception {
        mockMvc.perform(delete("/api/v1/community/posts/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isSuccess").value(true));

        then(postService).should().deletePost(any(), any());
    }

    @Test
    void getPostReturnsDetailResponse() throws Exception {
        given(postQueryFacade.getPost(10L, 1L)).willReturn(postResponse());

        mockMvc.perform(get("/api/v1/community/posts/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.title").value("게시글 제목"))
                .andExpect(jsonPath("$.result.authorLevel").value(3))
                .andExpect(jsonPath("$.result.childAge").value(7))
                .andExpect(jsonPath("$.result.isMine").value(true))
                .andExpect(jsonPath("$.result.viewCount").value(3))
                .andExpect(jsonPath("$.result.likeCount").value(4))
                .andExpect(jsonPath("$.result.isLiked").value(true))
                .andExpect(jsonPath("$.result.commentCount").value(5))
                .andExpect(jsonPath("$.result.scrapCount").value(6))
                .andExpect(jsonPath("$.result.isScrapped").value(false))
                .andExpect(jsonPath("$.result.disabilityTypes[0]").value("AUTISM"));
    }

    @Test
    void getPostReturnsCommunityNotFoundError() throws Exception {
        given(postQueryFacade.getPost(10L, 99L))
                .willThrow(new CommunityException(CommunityErrorCode.POST_NOT_FOUND));

        mockMvc.perform(get("/api/v1/community/posts/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("COMMUNITY404_1"));
    }

    @Test
    void likePostReturnsCurrentLikeStateAndCount() throws Exception {
        given(postService.likePost(10L, 1L)).willReturn(new PostLikeResponse(true, 5));

        mockMvc.perform(put("/api/v1/community/posts/1/likes"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.isLiked").value(true))
                .andExpect(jsonPath("$.result.likeCount").value(5));
    }

    @Test
    void unlikePostReturnsCurrentLikeStateAndCount() throws Exception {
        given(postService.unlikePost(10L, 1L)).willReturn(new PostLikeResponse(false, 3));

        mockMvc.perform(delete("/api/v1/community/posts/1/likes"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.isLiked").value(false))
                .andExpect(jsonPath("$.result.likeCount").value(3));
    }

    @Test
    void scrapPostReturnsCurrentScrapStateAndCount() throws Exception {
        given(postService.scrapPost(10L, 1L)).willReturn(new PostScrapResponse(true, 7));

        mockMvc.perform(put("/api/v1/community/posts/1/scraps"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.isScrapped").value(true))
                .andExpect(jsonPath("$.result.scrapCount").value(7));
    }

    @Test
    void unscrapPostReturnsCurrentScrapStateAndCount() throws Exception {
        given(postService.unscrapPost(10L, 1L)).willReturn(new PostScrapResponse(false, 6));

        mockMvc.perform(delete("/api/v1/community/posts/1/scraps"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.isScrapped").value(false))
                .andExpect(jsonPath("$.result.scrapCount").value(6));
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
                null,
                3,
                7,
                true,
                PostBoardType.FREE_COMMUNICATION,
                PostAnonymityType.PROFILE_TAG_VISIBLE,
                "게시글 제목",
                "게시글 내용",
                true,
                3,
                4,
                true,
                5,
                6,
                false,
                List.of(DisabilityType.AUTISM),
                List.of("육아"),
                List.of("https://example.com/image.jpg"),
                Instant.parse("2026-07-18T00:00:00Z"),
                Instant.parse("2026-07-18T00:00:00Z")
        );
    }

    private PostListItemResponse postListItemResponse() {
        return new PostListItemResponse(
                1L,
                PostBoardType.FREE_COMMUNICATION,
                PostAnonymityType.PROFILE_TAG_VISIBLE,
                "게시글 제목",
                "게시글 내용",
                false,
                new PostListItemResponse.AuthorResponse(
                        10L,
                        "보듬맘",
                        "https://example.com/profile.jpg",
                        1,
                        "새싹",
                        true
                ),
                "https://example.com/image.jpg",
                3,
                4,
                5,
                6,
                true,
                Instant.parse("2026-07-18T00:00:00Z")
        );
    }
}
