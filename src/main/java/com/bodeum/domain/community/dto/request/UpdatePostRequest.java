package com.bodeum.domain.community.dto.request;

import com.bodeum.domain.community.entity.Post;
import com.bodeum.domain.community.entity.PostImage;
import com.bodeum.domain.community.enums.PostAnonymityType;
import com.bodeum.domain.community.enums.PostBoardType;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.List;

@JsonIgnoreProperties({"disabilityTypes", "hashtags"})
public record UpdatePostRequest(
        @Schema(example = "INFORMATION_QUESTION")
        PostBoardType boardType,

        @Schema(example = "FULLY_ANONYMOUS")
        PostAnonymityType anonymityType,

        @Schema(example = "수정된 게시글 제목입니다.")
        @Pattern(regexp = "(?s).*\\S.*", message = "게시글 제목은 비어 있을 수 없습니다.")
        @Size(max = Post.TITLE_MAX_LENGTH, message = "게시글 제목은 150자 이하로 입력해주세요.")
        String title,

        @Schema(example = "수정된 게시글 내용입니다.")
        @Pattern(regexp = "(?s).*\\S.*", message = "게시글 내용은 비어 있을 수 없습니다.")
        @Size(max = Post.CONTENT_MAX_LENGTH, message = "게시글 내용은 2,000자 이하로 입력해주세요.")
        String content,

        @ArraySchema(arraySchema = @Schema(example = "[\"https://example.com/updated-image.jpg\"]"))
        @Size(max = 10, message = "이미지는 최대 10개까지 입력할 수 있습니다.")
        List<
                @NotBlank(message = "이미지 URL은 비어 있을 수 없습니다.")
                @Size(max = PostImage.IMAGE_URL_MAX_LENGTH, message = "이미지 URL은 500자 이하로 입력해주세요.")
                String
                > imageUrls
) {

    @JsonAnySetter
    public void rejectUnknownField(String fieldName, Object value) {
        throw new IllegalArgumentException("지원하지 않는 요청 필드입니다: " + fieldName);
    }

    @JsonIgnore
    @Schema(hidden = true)
    @AssertTrue(message = "수정할 게시글 정보를 하나 이상 입력해주세요.")
    public boolean isUpdateRequested() {
        return boardType != null
                || anonymityType != null
                || title != null
                || content != null
                || imageUrls != null;
    }
}
