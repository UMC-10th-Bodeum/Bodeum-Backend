package com.bodeum.domain.user.controller;

import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.bodeum.domain.mypage.entity.enums.ScrapType;
import com.bodeum.domain.mypage.service.MyPageService;
import com.bodeum.domain.onboarding.service.OnboardingService;
import com.bodeum.domain.user.service.UserService;
import com.bodeum.global.apiPayload.handler.GeneralExceptionAdvice;
import com.bodeum.global.auth.LoginUser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.MethodParameter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

@ExtendWith(MockitoExtension.class)
class UserControllerTest {

    @Mock
    private UserService userService;

    @Mock
    private OnboardingService onboardingService;

    @Mock
    private MyPageService myPageService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();

        mockMvc = MockMvcBuilders
                .standaloneSetup(
                        new UserController(
                                userService,
                                onboardingService,
                                myPageService
                        )
                )
                .setControllerAdvice(new GeneralExceptionAdvice())
                .setCustomArgumentResolvers(loginUserArgumentResolver())
                .setValidator(validator)
                .build();
    }

    @Test
    void deleteInfoScrapReturnsNoContent() throws Exception {
        mockMvc.perform(delete("/api/v1/users/me/scraps/1")
                        .param("scrapType", "INFO"))
                .andExpect(status().isNoContent());

        then(myPageService).should()
                .deleteScrap(10L, 1L, ScrapType.INFO);
    }

    @Test
    void deleteNewsScrapReturnsNoContent() throws Exception {
        mockMvc.perform(delete("/api/v1/users/me/scraps/2")
                        .param("scrapType", "NEWS"))
                .andExpect(status().isNoContent());

        then(myPageService).should()
                .deleteScrap(10L, 2L, ScrapType.NEWS);
    }

    @Test
    void deleteScrapRejectsMissingScrapType() throws Exception {
        mockMvc.perform(delete("/api/v1/users/me/scraps/1"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("COMMON400_1"));

        then(myPageService).should(never())
                .deleteScrap(10L, 1L, ScrapType.INFO);
    }

    @Test
    void deleteScrapRejectsUnknownScrapType() throws Exception {
        mockMvc.perform(delete("/api/v1/users/me/scraps/1")
                        .param("scrapType", "UNKNOWN"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("COMMON400_1"));

        then(myPageService).shouldHaveNoInteractions();
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
