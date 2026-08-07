package com.bodeum.domain.community.service;

import com.bodeum.domain.community.dto.response.PostImageUploadResponse;
import com.bodeum.global.infrastructure.storage.S3ImageStorage;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
public class PostImageService {

    private static final String IMAGE_DIRECTORY = "community-posts";

    private final S3ImageStorage s3ImageStorage;

    public PostImageUploadResponse uploadImage(MultipartFile image) {
        String imageUrl = s3ImageStorage.upload(image, IMAGE_DIRECTORY);
        return PostImageUploadResponse.from(imageUrl);
    }
}
