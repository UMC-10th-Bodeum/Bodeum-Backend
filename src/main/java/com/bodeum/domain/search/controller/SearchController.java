package com.bodeum.domain.search.controller;

import com.bodeum.domain.search.dto.response.InfoSearchResponse;
import com.bodeum.domain.search.dto.response.SearchHistoryResponse;
import com.bodeum.domain.search.service.SearchService;
import com.bodeum.global.apiPayload.ApiResponse;
import com.bodeum.global.apiPayload.code.GeneralSuccessCode;
import com.bodeum.global.auth.AuthUserPrincipal;
import com.bodeum.global.auth.LoginUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1")
@Tag(name = "Search", description = "검색 API")
public class SearchController {

    private final SearchService searchService;

    @GetMapping("/search")
    @Operation(summary = "INFO 검색")
    public ApiResponse<InfoSearchResponse> searchInfo(
            @RequestParam String keyword,
            @LoginUser AuthUserPrincipal user
    ) {
        Long userId = user != null ? user.userId() : null;
        return ApiResponse.of(GeneralSuccessCode.OK, searchService.searchInfo(keyword, userId));
    }

    @GetMapping("/search-history")
    @Operation(summary = "최근 검색어 조회")
    public ApiResponse<SearchHistoryResponse> getSearchHistory(@LoginUser AuthUserPrincipal user) {
        return ApiResponse.of(GeneralSuccessCode.OK, searchService.getSearchHistory(user.userId()));
    }

    @DeleteMapping("/search-history/{keyword}")
    @Operation(summary = "최근 검색어 삭제")
    public ApiResponse<Void> deleteSearchHistory(
            @PathVariable String keyword,
            @LoginUser AuthUserPrincipal user
    ) {
        searchService.deleteSearchHistory(user.userId(), keyword);
        return ApiResponse.of(GeneralSuccessCode.OK, null);
    }
}
