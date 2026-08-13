package com.bodeum.domain.ai.controller;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.bodeum.domain.ai.dto.response.AiChatStarterResponse;
import com.bodeum.domain.ai.service.chat.AiChatStarterService;
import com.bodeum.global.apiPayload.handler.GeneralExceptionAdvice;
import com.bodeum.global.auth.LoginUser;
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
class AiChatStarterControllerTest {

    @Mock
    private AiChatStarterService aiChatStarterService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .standaloneSetup(new AiChatStarterController(aiChatStarterService))
                .setControllerAdvice(new GeneralExceptionAdvice())
                .setCustomArgumentResolvers(loginUserArgumentResolver())
                .build();
    }

    @Test
    void createsChatStarter() throws Exception {
        when(aiChatStarterService.getChatStarter(10L))
                .thenReturn(AiChatStarterResponse.of(
                        "안녕하세요!\n\n민정맘님에게 필요한 정보를 안내해드려요.",
                        List.of(
                                "질문 1",
                                "질문 2",
                                "질문 3",
                                "질문 4",
                                "질문 5"
                        )
                ));

        mockMvc.perform(post("/api/v1/ai/chat-room/starter"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("COMMON200_1"))
                .andExpect(jsonPath("$.result.greeting")
                        .value("안녕하세요!\n\n민정맘님에게 필요한 정보를 안내해드려요."))
                .andExpect(jsonPath("$.result.suggestedQuestions.length()").value(5))
                .andExpect(jsonPath("$.result.suggestedQuestions[0]")
                        .value("질문 1"));
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
