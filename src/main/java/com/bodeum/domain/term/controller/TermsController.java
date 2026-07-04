package com.bodeum.domain.term.controller;

import com.bodeum.domain.term.dto.response.TermsResDTO;
import com.bodeum.domain.term.enumtype.TermType;
import com.bodeum.domain.term.service.TermsService;
import com.bodeum.global.apiPayload.ApiResponse;
import com.bodeum.global.apiPayload.code.GeneralSuccessCode;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/terms")
@RequiredArgsConstructor
public class TermsController {

    private final TermsService termsService;

    @GetMapping("/{type}")
    public ApiResponse<TermsResDTO> getTerms(
            @PathVariable String type
    ) {
        return ApiResponse.of(GeneralSuccessCode.OK, termsService.getTerms(TermType.from(type)));
    }
}
