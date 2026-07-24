package com.bodeum.domain.ai.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.bodeum.domain.ai.dto.response.AiMessageHistoryResponse;
import com.bodeum.domain.ai.dto.response.AiTodayMessageResponse;
import com.bodeum.domain.ai.service.AiMessageQueryService;
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
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

@ExtendWith(MockitoExtension.class)
class AiMessageControllerTest {

    @Mock
    private AiMessageQueryService aiMessageQueryService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new AiMessageController(aiMessageQueryService))
                .setControllerAdvice(new GeneralExceptionAdvice())
                .setCustomArgumentResolvers(loginUserArgumentResolver())
                .build();
    }

    @Test
    void getHistoryMessagesRejectsCursorIdOnly() throws Exception {
        mockMvc.perform(get("/api/v1/ai/messages/history")
                        .param("cursorId", "10"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("AI400_1"));

        verify(aiMessageQueryService, never()).getHistoryMessages(any(), any(), any());
    }

    @Test
    void getHistoryMessagesRejectsCursorCreatedAtOnly() throws Exception {
        mockMvc.perform(get("/api/v1/ai/messages/history")
                        .param("cursorCreatedAt", "2026-07-05T14:20:00Z"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("AI400_1"));

        verify(aiMessageQueryService, never()).getHistoryMessages(any(), any(), any());
    }

    @Test
    void getHistoryMessagesAllowsBothCursorValues() throws Exception {
        when(aiMessageQueryService.getHistoryMessages(10L, 26L, Instant.parse("2026-07-05T14:20:00Z")))
                .thenReturn(AiMessageHistoryResponse.of(List.of(), null, false));

        mockMvc.perform(get("/api/v1/ai/messages/history")
                        .param("cursorId", "26")
                        .param("cursorCreatedAt", "2026-07-05T14:20:00Z"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("COMMON200_1"));
    }

    @Test
    void getTodayMessagesStillWorks() throws Exception {
        when(aiMessageQueryService.getTodayMessages(10L))
                .thenReturn(AiTodayMessageResponse.of(List.of()));

        mockMvc.perform(get("/api/v1/ai/messages/today"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("COMMON200_1"));
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
