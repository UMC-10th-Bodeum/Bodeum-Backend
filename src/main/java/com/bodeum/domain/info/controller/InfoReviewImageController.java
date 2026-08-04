package com.bodeum.domain.info.controller;

import com.bodeum.domain.info.dto.response.ImageUploadResponse;
import com.bodeum.global.apiPayload.ApiResponse;
import com.bodeum.global.apiPayload.code.GeneralSuccessCode;
import com.bodeum.global.apiPayload.exception.ProjectException;
import com.bodeum.global.auth.LoginUser;
import com.bodeum.global.infrastructure.storage.S3ImageStorage;
import com.bodeum.global.infrastructure.storage.StorageErrorCode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.Locale;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@Tag(name = "Info", description = "정보 후기 이미지 업로드 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/info/reviews/images")
public class InfoReviewImageController {

    // S3 상의 저장 디렉터리. 리뷰 도메인 전용 prefix로 다른 도메인 이미지와 구분한다.
    private static final String IMAGE_DIRECTORY = "info-reviews";

    // 기획 스펙상 정보 후기 이미지는 jpg/png만 허용한다 (S3ImageStorage 자체는 webp도 지원하지만 이 기능에서는 제외).
    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of("image/jpeg", "image/png");

    private final S3ImageStorage s3ImageStorage;

    @Operation(
            summary = "정보 후기 이미지 업로드",
            description = "정보 후기 작성/수정 시 첨부할 이미지를 업로드하고 공개 접근 URL을 반환한다. "
                    + "발급받은 URL을 CreateInfoReviewRequest.imageUrls에 담아 후기 작성/수정 API를 호출한다."
    )
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<ImageUploadResponse> uploadImage(
            @LoginUser Long userId,
            @Parameter(description = "업로드할 이미지 파일 (jpg, png만 허용)")
            @RequestParam("image") MultipartFile image
    ) {
        validateContentType(image);
        String imageUrl = s3ImageStorage.upload(image, IMAGE_DIRECTORY);
        return ApiResponse.of(GeneralSuccessCode.OK, ImageUploadResponse.from(imageUrl));
    }

    // jpg/png 외 형식(webp 포함)은 이 API 단에서 먼저 차단한다.
    private void validateContentType(MultipartFile image) {
        String contentType = image.getContentType();
        if (!StringUtils.hasText(contentType)) {
            throw new ProjectException(StorageErrorCode.INVALID_IMAGE_TYPE);
        }

        String mediaType = contentType.split(";", 2)[0].trim().toLowerCase(Locale.ROOT);
        if (!ALLOWED_CONTENT_TYPES.contains(mediaType)) {
            throw new ProjectException(StorageErrorCode.INVALID_IMAGE_TYPE);
        }
    }
}