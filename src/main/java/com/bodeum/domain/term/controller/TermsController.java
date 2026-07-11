package com.bodeum.domain.term.controller;

import com.bodeum.domain.term.dto.response.TermsResponse;
import com.bodeum.domain.term.enumtype.TermType;
import com.bodeum.domain.term.service.TermsService;
import com.bodeum.global.apiPayload.ApiResponse;
import com.bodeum.global.apiPayload.code.GeneralSuccessCode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Terms", description = "약관 조회 API")
@RestController
@RequestMapping("/api/v1/terms")
@RequiredArgsConstructor
public class TermsController {

    private final TermsService termsService;

    @Operation(summary = "약관 내용 조회", description = "약관 유형별(서비스 이용약관/개인정보 처리방침 등) 내용을 조회한다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "COMMON400_1: 지원하지 않는 약관 유형"
            )
    })
    @SecurityRequirements
    @GetMapping("/{type}")
    public ApiResponse<TermsResponse> getTerms(
            @Parameter(description = "약관 유형", required = true, example = "service")
            @PathVariable String type
    ) {
        return ApiResponse.of(GeneralSuccessCode.OK, termsService.getTerms(TermType.from(type)));
    }
}
