package com.bodeum.domain.community.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import com.bodeum.global.infrastructure.storage.S3ImageStorage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

@ExtendWith(MockitoExtension.class)
class PostImageServiceTest {

    @Mock
    private S3ImageStorage s3ImageStorage;

    @InjectMocks
    private PostImageService postImageService;

    @Test
    void uploadImageStoresFileInCommunityPostDirectory() {
        String imageUrl = "https://bodeum-bucket.s3.ap-northeast-2.amazonaws.com/"
                + "community-posts/image-id.png";
        MockMultipartFile image = new MockMultipartFile(
                "image",
                "post-image.png",
                "image/png",
                new byte[]{1, 2, 3}
        );
        given(s3ImageStorage.upload(image, "community-posts")).willReturn(imageUrl);

        var response = postImageService.uploadImage(image);

        assertThat(response.imageUrl()).isEqualTo(imageUrl);
        then(s3ImageStorage).should().upload(image, "community-posts");
    }
}
