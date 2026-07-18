package com.bodeum.domain.community.dto.request;

import com.bodeum.domain.community.entity.Hashtag;
import com.bodeum.domain.community.entity.Post;
import com.bodeum.domain.community.entity.PostImage;
import com.bodeum.domain.community.enums.DisabilityType;
import com.bodeum.domain.community.enums.PostAnonymityType;
import com.bodeum.domain.community.enums.PostBoardType;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;
import org.hibernate.validator.constraints.UniqueElements;

public record CreatePostRequest(
        @Schema(example = "FREE_COMMUNICATION")
        @NotNull(message = "게시판 유형은 필수입니다.")
        PostBoardType boardType,

        @Schema(example = "PROFILE_TAG_VISIBLE")
        @NotNull(message = "익명 설정은 필수입니다.")
        PostAnonymityType anonymityType,

        @Schema(example = "아이와 함께 갈 수 있는 공원을 추천해주세요.")
        @NotBlank(message = "게시글 제목은 비어 있을 수 없습니다.")
        @Size(max = Post.TITLE_MAX_LENGTH, message = "게시글 제목은 150자 이하로 입력해주세요.")
        String title,

        @Schema(example = "주말에 방문하기 좋은 조용한 공원을 찾고 있습니다.")
        @NotBlank(message = "게시글 내용은 비어 있을 수 없습니다.")
        @Size(max = Post.CONTENT_MAX_LENGTH, message = "게시글 내용은 2,000자 이하로 입력해주세요.")
        String content,

        @ArraySchema(
                arraySchema = @Schema(example = "[\"AUTISM\", \"DEVELOPMENTAL_DELAY\"]"),
                schema = @Schema(implementation = DisabilityType.class)
        )
        @UniqueElements(message = "장애 유형 태그는 중복 선택할 수 없습니다.")
        List<DisabilityType> disabilityTypes,

        @ArraySchema(arraySchema = @Schema(example = "[\"육아\", \"공원추천\"]"))
        @UniqueElements(message = "해시태그는 중복 입력할 수 없습니다.")
        List<
                @NotBlank(message = "해시태그는 비어 있을 수 없습니다.")
                @Size(max = Hashtag.NAME_MAX_LENGTH, message = "해시태그는 50자 이하로 입력해주세요.")
                String
                > hashtags,

        @ArraySchema(arraySchema = @Schema(example = "[\"https://example.com/post-image.jpg\"]"))
        List<
                @NotBlank(message = "이미지 URL은 비어 있을 수 없습니다.")
                @Size(max = PostImage.IMAGE_URL_MAX_LENGTH, message = "이미지 URL은 500자 이하로 입력해주세요.")
                String
                > imageUrls
) {
}
