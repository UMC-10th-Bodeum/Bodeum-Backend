package com.bodeum.domain.ai.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.bodeum.domain.ai.dto.response.CreateAiFeedbackResponse;
import com.bodeum.domain.ai.enums.AiFeedbackReasonType;
import com.bodeum.domain.ai.enums.AiFeedbackType;
import com.bodeum.domain.ai.service.AiFeedbackService;
import com.bodeum.global.apiPayload.handler.GeneralExceptionAdvice;
import com.bodeum.global.auth.LoginUser;
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
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

@ExtendWith(MockitoExtension.class)
class AiFeedbackControllerTest {

    @Mock
    private AiFeedbackService aiFeedbackService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new AiFeedbackController(aiFeedbackService))
                .setControllerAdvice(new GeneralExceptionAdvice())
                .setCustomArgumentResolvers(loginUserArgumentResolver())
                .build();
    }

    @Test
    void createsHelpfulFeedbackWithoutReasons() throws Exception {
        when(aiFeedbackService.createFeedback(eq(10L), eq(12L), any()))
                .thenReturn(CreateAiFeedbackResponse.of(
                        1L,
                        AiFeedbackType.HELPFUL,
                        List.of()
                ));

        mockMvc.perform(post("/api/v1/ai/messages/12/feedback")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"feedbackType":"HELPFUL"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.aiFeedbackId").value(1))
                .andExpect(jsonPath("$.result.feedbackType").value("HELPFUL"))
                .andExpect(jsonPath("$.result.reasons").doesNotExist());
    }

    @Test
    void createsIncorrectFeedbackWithReasons() throws Exception {
        when(aiFeedbackService.createFeedback(eq(10L), eq(12L), any()))
                .thenReturn(CreateAiFeedbackResponse.of(
                        1L,
                        AiFeedbackType.INCORRECT,
                        List.of(AiFeedbackReasonType.TIME, AiFeedbackReasonType.ELIGIBILITY)
                ));

        mockMvc.perform(post("/api/v1/ai/messages/12/feedback")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "feedbackType":"INCORRECT",
                                  "reasons":["TIME","ELIGIBILITY"]
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.reasons[0]").value("TIME"))
                .andExpect(jsonPath("$.result.reasons[1]").value("ELIGIBILITY"));
    }

    @Test
    void rejectsIncorrectFeedbackWithoutReasons() throws Exception {
        mockMvc.perform(post("/api/v1/ai/messages/12/feedback")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"feedbackType":"INCORRECT"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("COMMON400_1"));

        verify(aiFeedbackService, never()).createFeedback(any(), any(), any());
    }

    @Test
    void rejectsIncorrectFeedbackWithNullReason() throws Exception {
        mockMvc.perform(post("/api/v1/ai/messages/12/feedback")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"feedbackType":"INCORRECT","reasons":[null]}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("COMMON400_1"));

        verify(aiFeedbackService, never()).createFeedback(any(), any(), any());
    }

    @Test
    void rejectsHelpfulFeedbackWithReasons() throws Exception {
        mockMvc.perform(post("/api/v1/ai/messages/12/feedback")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"feedbackType":"HELPFUL","reasons":["TIME"]}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("COMMON400_1"));

        verify(aiFeedbackService, never()).createFeedback(any(), any(), any());
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
}
