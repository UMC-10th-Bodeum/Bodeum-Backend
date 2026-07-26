package com.bodeum.domain.info.service;

import com.bodeum.domain.info.dto.response.InfoHelpfulToggleResponse;
import com.bodeum.domain.info.dto.response.InfoScrapToggleResponse;
import com.bodeum.domain.info.entity.InfoItem;
import com.bodeum.domain.info.entity.InfoReview;
import com.bodeum.domain.info.entity.InfoReviewHelpful;
import com.bodeum.domain.info.entity.InfoScrap;
import com.bodeum.domain.info.exception.InfoErrorCode;
import com.bodeum.domain.info.exception.InfoException;
import com.bodeum.domain.info.repository.InfoItemRepository;
import com.bodeum.domain.info.repository.InfoReviewHelpfulRepository;
import com.bodeum.domain.info.repository.InfoReviewRepository;
import com.bodeum.domain.info.repository.InfoScrapRepository;
import com.bodeum.domain.user.entity.User;
import com.bodeum.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class InfoToggleService {

    private final InfoItemRepository infoItemRepository;
    private final InfoReviewRepository infoReviewRepository;
    private final InfoScrapRepository infoScrapRepository;
    private final InfoReviewHelpfulRepository infoReviewHelpfulRepository;
    private final UserRepository userRepository;

    /**
     * 1. 정보 스크랩 토글
     */
    @Transactional
    public InfoScrapToggleResponse toggleScrap(Long userId, Long infoItemId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new InfoException(InfoErrorCode.UNAUTHORIZED));

        InfoItem infoItem = infoItemRepository.findById(infoItemId)
                .orElseThrow(() -> new InfoException(InfoErrorCode.INFO_ITEM_NOT_FOUND));

        Optional<InfoScrap> existingScrap = infoScrapRepository.findByUserAndInfoItem(user, infoItem);

        if (existingScrap.isPresent()) {
            infoScrapRepository.delete(existingScrap.get());
            infoItem.updateScrapCount(-1);
            return InfoScrapToggleResponse.of(false, infoItem.getScrapCount());
        } else {
            InfoScrap scrap = InfoScrap.builder()
                    .user(user)
                    .infoItem(infoItem)
                    .build();
            infoScrapRepository.save(scrap);
            infoItem.updateScrapCount(1);
            return InfoScrapToggleResponse.of(true, infoItem.getScrapCount());
        }
    }

    /**
     * 2. 후기 도움돼요 토글
     */
    @Transactional
    public InfoHelpfulToggleResponse toggleHelpful(Long userId, Long infoItemId, Long infoReviewId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new InfoException(InfoErrorCode.UNAUTHORIZED));

        InfoReview infoReview = infoReviewRepository.findById(infoReviewId)
                .orElseThrow(() -> new InfoException(InfoErrorCode.INFO_REVIEW_NOT_FOUND));

        // 1) 해당 후기가 경로의 infoItemId에 속하는지 검증
        if (!infoReview.getInfoItem().getId().equals(infoItemId)) {
            throw new InfoException(InfoErrorCode.INFO_REVIEW_NOT_FOUND);
        }

        // 2) 본인이 작성한 후기인지 검증
        if (infoReview.getUser().getId().equals(userId)) {
            throw new InfoException(InfoErrorCode.CANNOT_HELPFUL_OWN_REVIEW);
        }

        Optional<InfoReviewHelpful> existingHelpful = infoReviewHelpfulRepository.findByUserAndInfoReview(user, infoReview);

        if (existingHelpful.isPresent()) {
            infoReviewHelpfulRepository.delete(existingHelpful.get());
            infoReview.updateHelpfulCount(-1);
            return InfoHelpfulToggleResponse.of(false, infoReview.getHelpfulCount());
        } else {
            InfoReviewHelpful helpful = InfoReviewHelpful.builder()
                    .user(user)
                    .infoReview(infoReview)
                    .build();
            infoReviewHelpfulRepository.save(helpful);
            infoReview.updateHelpfulCount(1);
            return InfoHelpfulToggleResponse.of(true, infoReview.getHelpfulCount());
        }
    }
}