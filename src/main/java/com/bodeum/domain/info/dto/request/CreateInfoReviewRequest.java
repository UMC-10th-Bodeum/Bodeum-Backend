package com.bodeum.domain.info.dto.request;

import jakarta.validation.constraints.*;

import java.util.List;

public record CreateInfoReviewRequest(
        @NotNull(message = "별점은 필수 항목입니다.")
        @Min(value = 1, message = "별점은 최소 1점 이상이어야 합니다.")
        @Max(value = 5, message = "별점은 최대 5점까지 가능합니다.")
        @Digits(integer = 1, fraction = 0, message = "별점은 소수점 없이 정수만 입력 가능합니다.")
        Integer rating,

        @NotBlank(message = "후기 내용은 필수 입력 사항입니다.")
        @Size(max = 2000, message = "후기는 최대 2000자까지 입력할 수 있습니다.")
        String content,

        @Size(max = 5, message = "이미지는 최대 5장까지 첨부할 수 있습니다.")
        List<String> imageUrls
) {}