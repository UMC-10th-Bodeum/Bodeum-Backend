package com.bodeum.domain.ai.service;

import com.bodeum.domain.ai.model.rag.AiScrapInterests;
import com.bodeum.domain.community.enums.PostStatus;
import com.bodeum.domain.community.repository.PostHashtagRepository;
import com.bodeum.domain.mypage.repository.MyPageInfoScrapRepository;
import com.bodeum.domain.mypage.repository.MyPageNewsScrapRepository;
import com.bodeum.domain.mypage.repository.MyPagePostScrapRepository;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AiScrapInterestService {

    private static final int RECENT_SCRAP_LIMIT = 5;

    private final MyPageInfoScrapRepository infoScrapRepository;
    private final MyPageNewsScrapRepository newsScrapRepository;
    private final MyPagePostScrapRepository postScrapRepository;
    private final PostHashtagRepository postHashtagRepository;

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
        List<Long> postIds = postScraps.stream()
                .map(scrap -> scrap.getPost().getId())
                .toList();
        Map<Long, List<String>> hashtagsByPostId = postIds.isEmpty()
                ? Map.of()
                : postHashtagRepository.findAllByPost_IdIn(postIds).stream()
                        .collect(Collectors.groupingBy(
                                postHashtag -> postHashtag.getPost().getId(),
                                Collectors.mapping(
                                        postHashtag -> postHashtag.getHashtag().getName(),
                                        Collectors.toList()
                                )
                        ));
        List<String> communityTopics = postScraps.stream()
                .map(scrap -> formatCommunityTopic(
                        scrap.getPost().getTitle(),
                        scrap.getPost().getBoardType() == null
                                ? null
                                : scrap.getPost().getBoardType().name(),
                        hashtagsByPostId.getOrDefault(scrap.getPost().getId(), List.of())
                ))
                .filter(Objects::nonNull)
                .toList();

        return new AiScrapInterests(infoTitles, newsTitles, communityTopics);
    }

    private String formatCommunityTopic(
            String title,
            String boardType,
            List<String> hashtags
    ) {
        if (title == null || title.isBlank()) {
            return null;
        }
        List<String> details = new ArrayList<>();
        if (boardType != null && !boardType.isBlank()) {
            details.add("게시판: " + boardType);
        }
        List<String> validHashtags = hashtags.stream()
                .filter(hashtag -> hashtag != null && !hashtag.isBlank())
                .toList();
        if (!validHashtags.isEmpty()) {
            details.add("태그: " + String.join(", ", validHashtags));
        }
        return details.isEmpty()
                ? title
                : "%s (%s)".formatted(title, String.join(", ", details));
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
