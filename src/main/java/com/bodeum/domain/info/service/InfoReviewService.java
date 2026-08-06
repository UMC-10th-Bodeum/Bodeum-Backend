package com.bodeum.domain.info.service;

import com.bodeum.domain.info.dto.request.CreateInfoReviewRequest;
import com.bodeum.domain.info.dto.request.UpdateInfoReviewRequest;
import com.bodeum.domain.info.dto.response.InfoReviewListResponse;
import com.bodeum.domain.info.dto.response.InfoReviewResponse;
import com.bodeum.domain.info.entity.InfoItem;
import com.bodeum.domain.info.entity.InfoReview;
import com.bodeum.domain.info.exception.InfoErrorCode;
import com.bodeum.domain.info.exception.InfoException;
import com.bodeum.domain.info.repository.InfoItemRepository;
import com.bodeum.domain.info.repository.InfoReviewHelpfulRepository;
import com.bodeum.domain.info.repository.InfoReviewRepository;
import com.bodeum.domain.user.entity.User;
import com.bodeum.domain.user.exception.UserErrorCode;
import com.bodeum.domain.user.repository.UserRepository;
import com.bodeum.global.apiPayload.exception.ProjectException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class InfoReviewService {

    private final InfoReviewRepository infoReviewRepository;
    private final InfoItemRepository infoItemRepository;
    private final UserRepository userRepository;
    private final InfoReviewHelpfulRepository infoReviewHelpfulRepository;

    // 1. 정보 후기 목록 및 평균 평점 조회 (비회원 가능)
    public InfoReviewListResponse getReviews(Long userId, Long infoId, Pageable pageable) {
        if (!infoItemRepository.existsById(infoId)) {
            throw new InfoException(InfoErrorCode.INFO_ITEM_NOT_FOUND);
        }

        Double averageRating = infoReviewRepository.findAverageRatingByInfoItemId(infoId);

        Page<InfoReview> reviews = infoReviewRepository.findByInfoItemId(infoId, pageable);

        List<Long> reviewIds = reviews.getContent().stream().map(InfoReview::getId).toList();
        Set<Long> helpfulReviewIds = (userId != null && !reviewIds.isEmpty())
                ? infoReviewHelpfulRepository.findInfoReviewIdsByUserIdAndInfoReviewIdIn(userId, reviewIds)
                : Set.of();

        Page<InfoReviewResponse> reviewPage = reviews.map(review ->
                InfoReviewResponse.from(review, helpfulReviewIds.contains(review.getId()))
        );

        return InfoReviewListResponse.of(averageRating, reviewPage);
    }

    // 2. 정보 후기 작성 (회원)
    @Transactional
    public InfoReviewResponse createReview(Long infoId, Long userId, CreateInfoReviewRequest request) {
        User user = getUserById(userId);
        InfoItem infoItem = getInfoItemById(infoId);

        InfoReview review = InfoReview.builder()
                .user(user)
                .infoItem(infoItem)
                .rating(request.rating())
                .content(request.content())
                .build();

        if (request.imageUrls() != null && !request.imageUrls().isEmpty()) {
            review.updateReview(request.content(), request.rating(), request.imageUrls());
        }

        // InfoItem의 reviewCount 카운트 증가
        infoItem.updateReviewCount(1);

        InfoReview savedReview = infoReviewRepository.save(review);
        return InfoReviewResponse.from(savedReview);
    }

    // 3. 정보 후기 수정 (회원, 본인 글)
    @Transactional
    public InfoReviewResponse updateReview(Long infoReviewId, Long userId, UpdateInfoReviewRequest request) {
        InfoReview review = getInfoReviewByIdWithImages(infoReviewId);
        validateReviewOwner(review, userId, InfoErrorCode.FORBIDDEN_REVIEW_UPDATE);

        review.updateReview(request.content(), request.rating(), request.imageUrls());
        return InfoReviewResponse.from(review);
    }

    // 4. 정보 후기 삭제 (회원, 본인 글)
    @Transactional
    public void deleteReview(Long infoReviewId, Long userId) {
        InfoReview review = getInfoReviewByIdWithImages(infoReviewId);
        validateReviewOwner(review, userId, InfoErrorCode.FORBIDDEN_REVIEW_DELETE);

        // InfoItem의 reviewCount 카운트 차감
        review.getInfoItem().updateReviewCount(-1);

        infoReviewRepository.delete(review);
    }

    private User getUserById(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ProjectException(UserErrorCode.USER_NOT_FOUND));
    }

    private InfoItem getInfoItemById(Long infoId) {
        return infoItemRepository.findById(infoId)
                .orElseThrow(() -> new InfoException(InfoErrorCode.INFO_ITEM_NOT_FOUND));
    }

    private InfoReview getInfoReviewByIdWithImages(Long infoReviewId) {
        return infoReviewRepository.findByIdWithUserAndImages(infoReviewId)
                .orElseThrow(() -> new InfoException(InfoErrorCode.INFO_REVIEW_NOT_FOUND));
    }

    private void validateReviewOwner(InfoReview review, Long userId, InfoErrorCode errorCode) {
        if (!review.getUser().getId().equals(userId)) {
            throw new InfoException(errorCode);
        }
    }
}