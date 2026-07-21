package com.bodeum.domain.info.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

public record UpdateInfoReviewRequest(
        @NotNull(message = "별점은 필수 항목입니다.")
        @Min(value = 1, message = "별점은 최소 1점 이상이어야 합니다.")
        @Max(value = 5, message = "별점은 최대 5점까지 가능합니다.")
        Integer rating,

        @NotBlank(message = "후기 내용은 필수 입력 사항입니다.")
        @Size(max = 2000, message = "후기는 최대 2000자까지 입력할 수 있습니다.")
        String content,

        @Size(max = 5, message = "이미지는 최대 5장까지 첨부할 수 있습니다.")
        List<String> imageUrls
) {}