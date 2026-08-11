package com.bodeum.domain.community.dto.response;

import com.bodeum.domain.community.entity.Post;
import com.bodeum.domain.community.enums.PostAnonymityType;
import com.bodeum.domain.community.enums.PostBoardType;
import com.bodeum.domain.user.enums.DisabilityType;
import com.bodeum.global.common.constant.WithdrawalConstants;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.List;

public record PostResponse(
        @Schema(example = "1")
        Long postId,

        @Schema(
                description = "작성자 ID. 완전 익명 게시글이거나 탈퇴 회원이면 null",
                example = "10",
                nullable = true
        )
        Long authorId,

        @Schema(
                description = "작성자 닉네임. 완전 익명이면 null, 탈퇴 회원이면 '탈퇴한 사용자'",
                example = "보듬맘",
                nullable = true
        )
        String authorNickname,

        @Schema(description = "작성자 레벨. 완전 익명 게시글이거나 탈퇴 회원이면 null", example = "2", nullable = true)
        Integer authorLevel,

        @Schema(description = "작성자 아이의 만 나이. 미등록·완전 익명·탈퇴 회원이면 null", example = "7", nullable = true)
        Integer childAge,

        @Schema(description = "현재 로그인 사용자의 게시글 여부", example = "true")
        boolean isMine,

        PostBoardType boardType,
        PostAnonymityType anonymityType,

        @Schema(example = "아이와 함께 갈 수 있는 공원을 추천해주세요.")
        String title,

        @Schema(example = "주말에 방문하기 좋은 조용한 공원을 찾고 있습니다.")
        String content,

        @Schema(description = "질문글 여부", example = "true")
        boolean isQuestion,

        int viewCount,
        int likeCount,
        @Schema(description = "현재 사용자의 좋아요 여부", example = "true")
        boolean isLiked,
        int commentCount,
        int scrapCount,
        @Schema(description = "현재 사용자의 스크랩 여부", example = "false")
        boolean isScrapped,

        @Schema(description = "작성자가 온보딩에서 등록한 장애 유형. 완전 익명·탈퇴 회원이면 빈 목록")
        List<DisabilityType> disabilityTypes,
        List<String> imageUrls,
        Instant createdAt,
        Instant updatedAt
) {

    public static PostResponse of(
            Post post,
            Long viewerId,
            boolean liked,
            boolean scrapped,
            boolean authorWithdrawn,
            String activeAuthorNickname,
            Integer authorLevel,
            Integer childAge,
            List<DisabilityType> disabilityTypes,
            List<String> imageUrls
    ) {
        boolean anonymous = post.getAnonymityType() == PostAnonymityType.FULLY_ANONYMOUS;
        // 완전 익명이 우선한다(탈퇴 사실을 드러내지 않음). 실명 게시글의 탈퇴 저자만 '탈퇴한 사용자'로 노출한다.
        Long authorId = anonymous || authorWithdrawn ? null : post.getUserId();
        String authorNickname = anonymous
                ? null
                : authorWithdrawn ? WithdrawalConstants.WITHDRAWN_DISPLAY_NAME : activeAuthorNickname;
        Integer visibleAuthorLevel = anonymous || authorWithdrawn ? null : authorLevel;
        Integer visibleChildAge = anonymous || authorWithdrawn ? null : childAge;
        List<DisabilityType> visibleDisabilityTypes = anonymous || authorWithdrawn
                ? List.of()
                : List.copyOf(disabilityTypes);

        return new PostResponse(
                post.getId(),
                authorId,
                authorNickname,
                visibleAuthorLevel,
                visibleChildAge,
                post.getUserId().equals(viewerId),
                post.getBoardType(),
                post.getAnonymityType(),
                post.getTitle(),
                post.getContent(),
                post.isQuestion(),
                post.getViewCount(),
                post.getLikeCount(),
                liked,
                post.getCommentCount(),
                post.getScrapCount(),
                scrapped,
                visibleDisabilityTypes,
                List.copyOf(imageUrls),
                post.getCreatedAt(),
                post.getUpdatedAt()
        );
    }
}
