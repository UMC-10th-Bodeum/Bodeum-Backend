package com.bodeum.domain.community.controller;

import com.bodeum.domain.community.dto.response.PostImageUploadResponse;
import com.bodeum.domain.community.service.PostImageService;
import com.bodeum.global.apiPayload.ApiResponse;
import com.bodeum.global.apiPayload.code.GeneralSuccessCode;
import com.bodeum.global.auth.LoginUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@Tag(name = "Community Post", description = "커뮤니티 게시글 이미지 업로드 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/community/posts/images")
public class PostImageController {

    private final PostImageService postImageService;

    @Operation(
            summary = "커뮤니티 게시글 이미지 업로드",
            description = "게시글 작성·수정 시 첨부할 이미지를 업로드하고 공개 접근 URL을 반환한다. "
                    + "반환된 URL을 게시글 작성·수정 요청의 imageUrls에 담아 호출한다."
    )
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<PostImageUploadResponse> uploadImage(
            @LoginUser Long userId,
            @Parameter(description = "업로드할 이미지 파일 (jpg, png, webp 허용, 장당 최대 10MB)")
            @RequestParam("image") MultipartFile image
    ) {
        return ApiResponse.of(GeneralSuccessCode.OK, postImageService.uploadImage(image));
    }
}
