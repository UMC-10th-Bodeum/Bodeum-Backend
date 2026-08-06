package com.bodeum.domain.community.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

public record PostImageUploadResponse(
        @Schema(
                description = "업로드된 커뮤니티 게시글 이미지의 공개 접근 URL",
                example = "https://bodeum-bucket.s3.ap-northeast-2.amazonaws.com/community-posts/3f1b1c2a-....jpg"
        )
        String imageUrl
) {

    public static PostImageUploadResponse from(String imageUrl) {
        return new PostImageUploadResponse(imageUrl);
    }
}
