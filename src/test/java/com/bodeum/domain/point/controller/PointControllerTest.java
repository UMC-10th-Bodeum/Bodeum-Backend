package com.bodeum.domain.point.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.bodeum.domain.point.dto.response.MyPointResponse;
import com.bodeum.domain.point.dto.response.MyPointResponse.PointActivity;
import com.bodeum.domain.point.enums.PointType;
import com.bodeum.domain.point.service.PointService;
import com.bodeum.global.auth.LoginUser;
import com.bodeum.global.auth.RequireSignupCompleted;
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
class PointControllerTest {

    @Mock
    private PointService pointService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .standaloneSetup(new PointController(pointService))
                .setCustomArgumentResolvers(loginUserArgumentResolver())
                .build();
    }

    @Test
    void getMyPointsReturnsActivityPointSummary() throws Exception {
        MyPointResponse response = new MyPointResponse(
                30,
                List.of(
                        PointActivity.of(PointType.POST_CREATED, 10L, 2L),
                        PointActivity.of(PointType.ANSWER_ACCEPTED, 20L, 1L)
                )
        );

        given(pointService.getMyPoints(10L))
                .willReturn(response);

        mockMvc.perform(get("/api/v1/users/me/points"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isSuccess").value(true))
                .andExpect(jsonPath("$.code").value("COMMON200_1"))
                .andExpect(jsonPath("$.result.totalPoint").value(30))
                .andExpect(jsonPath("$.result.activities[0].pointType")
                        .value("POST_CREATED"))
                .andExpect(jsonPath("$.result.activities[0].label")
                        .value("게시글 작성"))
                .andExpect(jsonPath("$.result.activities[0].pointPerAction")
                        .value(5))
                .andExpect(jsonPath("$.result.activities[0].earnedPoint")
                        .value(10))
                .andExpect(jsonPath("$.result.activities[0].activityCount")
                        .value(2));

        then(pointService).should().getMyPoints(10L);
    }

    @Test
    void pointControllerRequiresSignupCompleted() {
        assertThat(PointController.class.isAnnotationPresent(RequireSignupCompleted.class))
                .isTrue();
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
