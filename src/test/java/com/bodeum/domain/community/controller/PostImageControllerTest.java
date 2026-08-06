package com.bodeum.domain.community.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.bodeum.domain.community.dto.response.PostImageUploadResponse;
import com.bodeum.domain.community.service.PostImageService;
import com.bodeum.global.apiPayload.handler.GeneralExceptionAdvice;
import com.bodeum.global.auth.LoginUser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.MethodParameter;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;
import org.springframework.web.multipart.MultipartFile;

@ExtendWith(MockitoExtension.class)
class PostImageControllerTest {

    @Mock
    private PostImageService postImageService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new PostImageController(postImageService))
                .setControllerAdvice(new GeneralExceptionAdvice())
                .setCustomArgumentResolvers(loginUserArgumentResolver())
                .build();
    }

    @Test
    void uploadImageReturnsPublicImageUrl() throws Exception {
        String imageUrl = "https://bodeum-bucket.s3.ap-northeast-2.amazonaws.com/"
                + "community-posts/image-id.png";
        MockMultipartFile image = new MockMultipartFile(
                "image",
                "post-image.png",
                "image/png",
                new byte[]{1, 2, 3}
        );
        given(postImageService.uploadImage(any(MultipartFile.class)))
                .willReturn(PostImageUploadResponse.from(imageUrl));

        mockMvc.perform(multipart("/api/v1/community/posts/images").file(image))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.imageUrl").value(imageUrl));

        then(postImageService).should().uploadImage(any(MultipartFile.class));
    }

    @Test
    void uploadImageRejectsMissingImagePart() throws Exception {
        mockMvc.perform(multipart("/api/v1/community/posts/images"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("COMMON400_1"));

        then(postImageService).shouldHaveNoInteractions();
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
