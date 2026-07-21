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

import com.bodeum.domain.community.dto.request.CreateCommentRequest;
import com.bodeum.domain.community.dto.request.UpdateCommentRequest;
import com.bodeum.domain.community.dto.response.CommentLikeResponse;
import com.bodeum.domain.community.dto.response.CommentListResponse;
import com.bodeum.domain.community.dto.response.CommentResponse;
import com.bodeum.domain.community.enums.CommentStatus;
import com.bodeum.domain.community.service.CommentService;
import com.bodeum.global.apiPayload.handler.GeneralExceptionAdvice;
import com.bodeum.global.auth.LoginUser;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

@ExtendWith(MockitoExtension.class)
class CommentControllerTest {

    @Mock
    private CommentService commentService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();
        mockMvc = MockMvcBuilders.standaloneSetup(new CommentController(commentService))
                .setControllerAdvice(new GeneralExceptionAdvice())
                .setCustomArgumentResolvers(loginUserArgumentResolver())
                .setValidator(validator)
                .build();
    }

    @Test
    void createCommentReturnsCreatedResponse() throws Exception {
        given(commentService.createComment(any(), any(), any(CreateCommentRequest.class)))
                .willReturn(commentResponse(1L, null, List.of()));

        mockMvc.perform(post("/api/community/posts/1/comments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "content": "댓글 내용"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value("COMMON201_1"))
                .andExpect(jsonPath("$.result.commentId").value(1));
    }

    @Test
    void createReplyAllowsAnyCommentAsParent() throws Exception {
        given(commentService.createReply(any(), any(), any(CreateCommentRequest.class)))
                .willReturn(commentResponse(3L, 2L, List.of()));

        mockMvc.perform(post("/api/community/comments/2/replies")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "content": "중첩 답글"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.result.commentId").value(3))
                .andExpect(jsonPath("$.result.parentCommentId").value(2));
    }

    @Test
    void getCommentsReturnsNestedTree() throws Exception {
        CommentResponse nestedReply = commentResponse(3L, 2L, List.of());
        CommentResponse reply = commentResponse(2L, 1L, List.of(nestedReply));
        CommentResponse root = commentResponse(1L, null, List.of(reply));
        given(commentService.getComments(10L, 1L))
                .willReturn(new CommentListResponse(3, List.of(root)));

        mockMvc.perform(get("/api/community/posts/1/comments"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.totalCount").value(3))
                .andExpect(jsonPath("$.result.comments[0].commentId").value(1))
                .andExpect(jsonPath("$.result.comments[0].replies[0].commentId").value(2))
                .andExpect(jsonPath("$.result.comments[0].replies[0].replies[0].commentId").value(3));
    }

    @Test
    void updateAndDeleteCommentUseCommentPath() throws Exception {
        given(commentService.updateComment(any(), any(), any(UpdateCommentRequest.class)))
                .willReturn(commentResponse(1L, null, List.of()));

        mockMvc.perform(patch("/api/community/comments/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "content": "수정된 댓글"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.commentId").value(1));

        mockMvc.perform(delete("/api/community/comments/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isSuccess").value(true));

        then(commentService).should().deleteComment(10L, 1L);
    }

    @Test
    void likeAndUnlikeCommentReturnCurrentState() throws Exception {
        given(commentService.likeComment(10L, 1L)).willReturn(new CommentLikeResponse(true, 2));
        given(commentService.unlikeComment(10L, 1L)).willReturn(new CommentLikeResponse(false, 1));

        mockMvc.perform(put("/api/community/comments/1/likes"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.isLiked").value(true))
                .andExpect(jsonPath("$.result.likeCount").value(2));

        mockMvc.perform(delete("/api/community/comments/1/likes"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.isLiked").value(false))
                .andExpect(jsonPath("$.result.likeCount").value(1));
    }

    @Test
    void createCommentRejectsBlankContent() throws Exception {
        mockMvc.perform(post("/api/community/posts/1/comments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "content": " "
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("COMMON400_1"));

        then(commentService).should(never()).createComment(any(), any(), any(CreateCommentRequest.class));
    }

    @Test
    void createCommentRejectsContentLongerThanLimit() throws Exception {
        String content = "a".repeat(1001);

        mockMvc.perform(post("/api/community/posts/1/comments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "content": "%s"
                                }
                                """.formatted(content)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("COMMON400_1"));

        then(commentService).should(never()).createComment(any(), any(), any(CreateCommentRequest.class));
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

    private CommentResponse commentResponse(
            Long commentId,
            Long parentCommentId,
            List<CommentResponse> replies
    ) {
        return new CommentResponse(
                commentId,
                parentCommentId,
                10L,
                true,
                "댓글 내용",
                false,
                1,
                true,
                CommentStatus.ACTIVE,
                Instant.parse("2026-07-20T00:00:00Z"),
                Instant.parse("2026-07-20T00:00:00Z"),
                replies
        );
    }
}
