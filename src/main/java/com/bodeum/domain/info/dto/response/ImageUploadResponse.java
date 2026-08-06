package com.bodeum.domain.info.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

public record ImageUploadResponse(

        @Schema(description = "업로드된 이미지의 공개 접근 URL",
                example = "https://bodeum-bucket.s3.ap-northeast-2.amazonaws.com/info-reviews/3f1b1c2a-....jpg")
        String imageUrl

) {
    public static ImageUploadResponse from(String imageUrl) {
        return new ImageUploadResponse(imageUrl);
    }
}