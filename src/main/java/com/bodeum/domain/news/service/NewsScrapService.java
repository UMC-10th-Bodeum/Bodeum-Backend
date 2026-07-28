package com.bodeum.domain.news.service;

import com.bodeum.domain.news.dto.response.NewsScrapResponse;
import com.bodeum.domain.news.entity.News;
import com.bodeum.domain.news.entity.NewsScrap;
import com.bodeum.domain.news.repository.NewsRepository;
import com.bodeum.domain.news.repository.NewsScrapRepository;
import com.bodeum.global.apiPayload.code.GeneralErrorCode;
import com.bodeum.global.apiPayload.exception.ProjectException;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class NewsScrapService {

    private final NewsRepository newsRepository;
    private final NewsScrapRepository newsScrapRepository;

    @Transactional
    public NewsScrapResponse toggleScrap(Long userId, Long newsId) {
        validateAuthenticatedUser(userId);

        News news = newsRepository.findVisibleByIdForUpdate(newsId)
                .orElseThrow(() -> new ProjectException(GeneralErrorCode.NOT_FOUND));
        Optional<NewsScrap> existingScrap = newsScrapRepository.findByNewsIdAndUserId(newsId, userId);

        boolean scrapped;
        if (existingScrap.isPresent()) {
            newsScrapRepository.delete(existingScrap.get());
            news.decreaseScrapCount();
            scrapped = false;
        } else {
            newsScrapRepository.save(NewsScrap.create(news, userId));
            news.increaseScrapCount();
            scrapped = true;
        }

        return new NewsScrapResponse(news.getId(), scrapped, news.getScrapCount());
    }

    private void validateAuthenticatedUser(Long userId) {
        if (userId == null) {
            throw new ProjectException(GeneralErrorCode.UNAUTHORIZED);
        }
    }
}
