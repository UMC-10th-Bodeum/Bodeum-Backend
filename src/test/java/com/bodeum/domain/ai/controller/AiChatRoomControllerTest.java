package com.bodeum.domain.ai.controller;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.bodeum.domain.ai.dto.response.AiChatRoomResponse;
import com.bodeum.domain.ai.exception.AiErrorCode;
import com.bodeum.domain.ai.service.chat.AiChatRoomService;
import com.bodeum.domain.ai.service.chat.AiMessageService;
import com.bodeum.global.apiPayload.exception.ProjectException;
import com.bodeum.global.apiPayload.handler.GeneralExceptionAdvice;
import com.bodeum.global.auth.LoginUser;
import java.time.Instant;
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
class AiChatRoomControllerTest {

    @Mock
    private AiChatRoomService aiChatRoomService;

    @Mock
    private AiMessageService aiMessageService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .standaloneSetup(new AiChatRoomController(
                        aiChatRoomService,
                        aiMessageService
                ))
                .setControllerAdvice(new GeneralExceptionAdvice())
                .setCustomArgumentResolvers(loginUserArgumentResolver())
                .build();
    }

    @Test
    void getsExistingChatRoom() throws Exception {
        when(aiChatRoomService.getChatRoom(10L)).thenReturn(response());

        mockMvc.perform(get("/api/v1/ai/chat-room"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("COMMON200_1"))
                .andExpect(jsonPath("$.result.aiChatRoomId").value(1));
    }

    @Test
    void createsChatRoom() throws Exception {
        when(aiChatRoomService.createChatRoom(10L)).thenReturn(response());

        mockMvc.perform(post("/api/v1/ai/chat-room"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value("COMMON201_1"))
                .andExpect(jsonPath("$.result.aiChatRoomId").value(1));
    }

    @Test
    void returnsNotFoundWhenChatRoomDoesNotExist() throws Exception {
        when(aiChatRoomService.getChatRoom(10L))
                .thenThrow(new ProjectException(AiErrorCode.AI_CHAT_ROOM_NOT_FOUND));

        mockMvc.perform(get("/api/v1/ai/chat-room"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("AI404_1"));
    }

    @Test
    void returnsConflictWhenChatRoomAlreadyExists() throws Exception {
        when(aiChatRoomService.createChatRoom(10L))
                .thenThrow(new ProjectException(AiErrorCode.AI_CHAT_ROOM_ALREADY_EXISTS));

        mockMvc.perform(post("/api/v1/ai/chat-room"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("AI409_1"));
    }

    private AiChatRoomResponse response() {
        return AiChatRoomResponse.of(
                1L,
                Instant.parse("2026-08-07T00:00:00Z"),
                true,
                false,
                false
        );
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
