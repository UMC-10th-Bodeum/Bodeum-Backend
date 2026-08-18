package com.bodeum.domain.ai.service.support;

import com.bodeum.domain.ai.model.rag.AiScrapInterests;
import com.bodeum.domain.community.enums.PostStatus;
import com.bodeum.domain.mypage.repository.MyPageInfoScrapRepository;
import com.bodeum.domain.mypage.repository.MyPageNewsScrapRepository;
import com.bodeum.domain.mypage.repository.MyPagePostScrapRepository;
import java.util.List;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 사용자의 최근 스크랩 정보를 AI 개인화를 위한 관심사 데이터로 변환한다.
 */
@Service
@RequiredArgsConstructor
public class AiScrapInterestService {

    private static final int RECENT_SCRAP_LIMIT = 5;

    private final MyPageInfoScrapRepository infoScrapRepository;
    private final MyPageNewsScrapRepository newsScrapRepository;
    private final MyPagePostScrapRepository postScrapRepository;

    @Transactional(readOnly = true)
    public AiScrapInterests findRecentInterests(Long userId) {
        PageRequest limit = PageRequest.of(0, RECENT_SCRAP_LIMIT);

        List<String> infoTitles = infoScrapRepository
                .findRecentByUserId(userId, limit).stream()
                .map(scrap -> formatWithCategories(
                        scrap.getInfoItem().getName(),
                        scrap.getInfoItem().getCategoryNames()
                ))
                .filter(Objects::nonNull)
                .toList();
        List<String> newsTitles = newsScrapRepository
                .findRecentVisibleByUserId(userId, limit).stream()
                .map(scrap -> formatWithCategories(
                        scrap.getNews().getTitle(),
                        scrap.getNews().getNewsCategory() == null
                                ? List.of()
                                : List.of(scrap.getNews().getNewsCategory().getLabel())
                ))
                .filter(Objects::nonNull)
                .toList();

        var postScraps = postScrapRepository.findRecentVisibleByUserId(
                userId,
                PostStatus.ACTIVE,
                limit
        );
        List<String> communityTopics = postScraps.stream()
                .map(scrap -> formatCommunityTopic(
                        scrap.getPost().getTitle(),
                        scrap.getPost().getBoardType() == null
                                ? null
                                : scrap.getPost().getBoardType().name()
                ))
                .filter(Objects::nonNull)
                .toList();

        return new AiScrapInterests(infoTitles, newsTitles, communityTopics);
    }

    private String formatCommunityTopic(
            String title,
            String boardType
    ) {
        if (title == null || title.isBlank()) {
            return null;
        }
        return boardType == null || boardType.isBlank()
                ? title
                : "%s (게시판: %s)".formatted(title, boardType);
    }

    private String formatWithCategories(String title, List<String> categories) {
        if (title == null || title.isBlank()) {
            return null;
        }
        return categories.isEmpty()
                ? title
                : "%s (카테고리: %s)".formatted(title, String.join(" > ", categories));
    }
}
