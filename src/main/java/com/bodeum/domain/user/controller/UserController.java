package com.bodeum.domain.user.controller;

import com.bodeum.domain.mypage.dto.response.MyCommentListResponse;
import com.bodeum.domain.mypage.dto.response.MyPageProfileResponse;
import com.bodeum.domain.mypage.dto.response.MyPostListResponse;
import com.bodeum.domain.mypage.dto.response.MyScrapListResponse;
import com.bodeum.domain.mypage.service.MyPageService;
import com.bodeum.domain.onboarding.dto.response.OnboardingStatusResponse;
import com.bodeum.domain.onboarding.service.OnboardingService;
import com.bodeum.domain.user.dto.request.CreateUserAgreementRequest;
import com.bodeum.domain.user.dto.request.UpdateUserProfileRequest;
import com.bodeum.domain.user.dto.request.WithdrawUserRequest;
import com.bodeum.domain.user.dto.response.AiTermsAgreementResponse;
import com.bodeum.domain.user.dto.response.UserAgreementResponse;
import com.bodeum.domain.user.dto.response.UserHeaderResponse;
import com.bodeum.domain.user.dto.response.UserProfileResponse;
import com.bodeum.domain.user.dto.response.UserProfileUpdateResponse;
import com.bodeum.domain.user.dto.response.UserWithdrawResponse;
import com.bodeum.domain.user.service.UserService;
import com.bodeum.global.apiPayload.ApiResponse;
import com.bodeum.global.apiPayload.code.GeneralSuccessCode;
import com.bodeum.global.auth.LoginUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@Tag(name = "User", description = "사용자 프로필 및 약관 동의 관리 API")
@Validated
@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final OnboardingService onboardingService;
    private final MyPageService myPageService;

    @Operation(
            summary = "헤더/사이드바용 내 정보 조회",
            description = "헤더·사이드바 공통 조회. 로그인 시 닉네임/레벨/뱃지/자녀 정보/지역을, "
                    + "비로그인이거나 토큰이 만료된 경우에는 인증 없이 200으로 isLoggedIn=false를 반환한다."
    )
    @SecurityRequirements
    @GetMapping("/me/brief")
    public ApiResponse<UserHeaderResponse> getBrief(
            @LoginUser Long userId
    ) {
        return ApiResponse.of(
                GeneralSuccessCode.OK,
                userService.getHeaderInfo(userId)
        );
    }

    @Operation(
            summary = "내 프로필 대시보드 조회",
            description = "현재 로그인한 사용자의 상세 프로필과 저장한 정보, "
                    + "작성 게시글, 작성 댓글 수를 조회한다."
    )
    @GetMapping("/me/profile")
    public ApiResponse<MyPageProfileResponse> getProfile(
            @LoginUser Long userId
    ) {
        return ApiResponse.of(
                GeneralSuccessCode.OK,
                myPageService.getProfile(userId)
        );
    }

    @Operation(
            summary = "저장한 정보 목록 조회",
            description = "현재 로그인한 사용자가 저장한 복지·시설 정보와 "
                    + "뉴스·활동 정보를 최신 저장순으로 조회한다."
    )
    @GetMapping("/me/scraps")
    public ApiResponse<MyScrapListResponse> getScraps(
            @LoginUser Long userId
    ) {
        return ApiResponse.of(
                GeneralSuccessCode.OK,
                myPageService.getScraps(userId)
        );
    }

    @Operation(
            summary = "내 게시글 목록 조회",
            description = "현재 로그인한 사용자가 작성한 게시글을 최신순으로 조회한다."
    )
    @GetMapping("/me/posts")
    public ApiResponse<MyPostListResponse> getPosts(
            @LoginUser Long userId,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "10") @Min(1) int size
    ) {
        return ApiResponse.of(
                GeneralSuccessCode.OK,
                myPageService.getPosts(userId, page, size)
        );
    }

    @Operation(
            summary = "내 댓글 목록 조회",
            description = "현재 로그인한 사용자가 작성한 댓글과 해당 게시글 정보를 최신순으로 조회한다."
    )
    @GetMapping("/me/comments")
    public ApiResponse<MyCommentListResponse> getComments(
            @LoginUser Long userId,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "10") @Min(1) int size
    ) {
        return ApiResponse.of(
                GeneralSuccessCode.OK,
                myPageService.getComments(userId, page, size)
        );
    }

    @Operation(
            summary = "내 프로필 수정",
            description = "닉네임, 자녀 정보(이름/생년월/케어 영역/특징 키워드), "
                    + "관심사, 지역, 보호자 유형, 커뮤니티 성향 등 "
                    + "온보딩에서 입력한 프로필 정보를 수정한다."
    )
    @PatchMapping("/me/profile")
    public ApiResponse<UserProfileUpdateResponse> updateProfile(
            @LoginUser Long userId,
            @Valid @RequestBody UpdateUserProfileRequest request
    ) {
        return ApiResponse.of(
                GeneralSuccessCode.OK,
                userService.updateProfile(userId, request)
        );
    }

    @Operation(
            summary = "프로필 이미지 업로드",
            description = "이미지 파일(multipart/form-data, 필드명 image)을 업로드해 "
                    + "프로필 사진으로 저장하고 수정된 프로필을 반환한다."
    )
    @PostMapping(
            value = "/me/profile-image",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    public ApiResponse<UserProfileResponse> uploadProfileImage(
            @LoginUser Long userId,
            @RequestParam("image") MultipartFile image
    ) {
        return ApiResponse.of(
                GeneralSuccessCode.OK,
                userService.uploadProfileImage(userId, image)
        );
    }

    @Operation(
            summary = "온보딩 진행 상태 조회",
            description = "온보딩 단계별 완료 여부와 다음 이동 화면을 조회한다."
    )
    @GetMapping("/me/onboarding-status")
    public ApiResponse<OnboardingStatusResponse> getOnboardingStatus(
            @LoginUser Long userId
    ) {
        return ApiResponse.of(
                GeneralSuccessCode.OK,
                onboardingService.getStatus(userId)
        );
    }

    @Operation(
            summary = "약관 동의 등록",
            description = "서비스 이용약관, 개인정보 처리방침, AI 챗봇 이용 동의 여부를 등록한다."
    )
    @PostMapping("/me/agreements")
    public ApiResponse<UserAgreementResponse> agreeTerms(
            @LoginUser Long userId,
            @Valid @RequestBody CreateUserAgreementRequest request
    ) {
        return ApiResponse.of(
                GeneralSuccessCode.OK,
                userService.agreeTerms(userId, request)
        );
    }

    @Operation(
            summary = "회원 탈퇴",
            description = "현재 로그인한 사용자를 탈퇴 처리한다."
    )
    @DeleteMapping("/me")
    public ApiResponse<UserWithdrawResponse> withdraw(
            @LoginUser Long userId,
            @Valid @RequestBody(required = false)
            WithdrawUserRequest request
    ) {
        return ApiResponse.of(
                GeneralSuccessCode.OK,
                userService.withdraw(userId, request)
        );
    }

    @Operation(
            summary = "AI 챗봇 이용동의 여부 조회",
            description = "현재 로그인한 사용자의 AI 챗봇 이용동의 여부와 동의 일시를 조회한다."
    )
    @GetMapping("/me/ai-terms")
    public ApiResponse<AiTermsAgreementResponse> getAiTermsAgreement(
            @LoginUser Long userId
    ) {
        return ApiResponse.of(
                GeneralSuccessCode.OK,
                userService.getAiTermsAgreement(userId)
        );
    }

    @Operation(
            summary = "AI 챗봇 이용동의 등록",
            description = "현재 로그인한 사용자의 AI 챗봇 이용동의를 등록한다. "
                    + "이미 동의한 경우 기존 동의 일시를 반환한다."
    )
    @PostMapping("/me/ai-terms")
    public ApiResponse<AiTermsAgreementResponse> agreeAiTerms(
            @LoginUser Long userId
    ) {
        return ApiResponse.of(
                GeneralSuccessCode.OK,
                userService.agreeAiTerms(userId)
        );
    }
}
