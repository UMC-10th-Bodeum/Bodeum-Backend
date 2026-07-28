package com.bodeum.domain.user.service;

import com.bodeum.domain.ai.service.AiWithdrawalService;
import com.bodeum.domain.auth.exception.AuthErrorCode;
import com.bodeum.domain.community.service.CommentService;
import com.bodeum.domain.community.service.PostService;
import com.bodeum.domain.info.service.InfoScrapService;
import com.bodeum.domain.news.service.NewsScrapService;
import com.bodeum.domain.point.service.PointService;
import com.bodeum.domain.search.service.SearchService;
import com.bodeum.domain.user.dto.response.UserWithdrawResponse;
import com.bodeum.domain.user.entity.User;
import com.bodeum.domain.user.repository.UserRepository;
import com.bodeum.global.apiPayload.code.GeneralErrorCode;
import com.bodeum.global.apiPayload.exception.ProjectException;
import com.bodeum.global.infrastructure.storage.S3ImageStorage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * 회원 탈퇴 후속 처리를 단일 트랜잭션으로 오케스트레이션하는 Facade.
 *
 * <ul>
 *   <li>검색 기록·스크랩·좋아요(공감): 삭제하고 각 콘텐츠의 scrapCount·likeCount를 감소시킨다.</li>
 *   <li>게시글·댓글·답글·정보 리뷰 본문: 보존한다. 작성자 식별은 조회 시점에 '탈퇴한 사용자'로 익명화한다.</li>
 *   <li>AI 데이터: 회원에게 종속된 채팅방·메시지·응답 출처·피드백·피드백 사유를 삭제한다.
 *       공용 출처 데이터, 출처 검토 이력, Chroma의 INFO/NEWS 색인은 보존한다.</li>
 *   <li>포인트: 총점은 GuardianPoint.totalPoint, 적립 내역은 GuardianPointHistory에 있다.
 *       GuardianPoint는 guardianProfileId를 단순 컬럼으로 들고 있어 guardianProfile
 *       orphanRemoval로 지워지지 않으므로 명시적으로 삭제한다. 조회가 GuardianProfile을
 *       경유하므로 {@link UserService#withdraw(Long)}보다 먼저 삭제해야 한다.</li>
 *   <li>프로필 이미지: 개인정보이므로 S3 실제 파일까지 삭제한다. 롤백 불가한 외부 부수효과라 커밋 이후 수행한다.
 *       게시글·리뷰 이미지는 본문을 보존하므로 함께 보존한다.</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AccountWithdrawalService {

    private final UserRepository userRepository;
    private final UserService userService;
    private final SearchService searchService;
    private final PostService postService;
    private final CommentService commentService;
    private final InfoScrapService infoScrapService;
    private final NewsScrapService newsScrapService;
    private final AiWithdrawalService aiWithdrawalService;
    private final PointService pointService;
    private final S3ImageStorage s3ImageStorage;

    @Transactional
    public UserWithdrawResponse withdraw(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ProjectException(GeneralErrorCode.UNAUTHORIZED));
        if (user.isWithdrawn()) {
            throw new ProjectException(AuthErrorCode.ALREADY_WITHDRAWN);
        }
        // user.withdraw()가 profileImageUrl을 null로 만들기 전에 S3 삭제 대상 URL을 먼저 캡처한다.
        String profileImageUrl = user.getProfileImageUrl();

        // 1. 검색 기록 삭제
        searchService.deleteUserSearchLogs(userId);
        // 2 & 6. 스크랩·좋아요(공감) 삭제 + 콘텐츠 scrapCount·likeCount 감소
        postService.deleteUserScrapsAndLikes(userId);
        commentService.deleteUserCommentLikes(userId);
        infoScrapService.deleteUserScraps(userId);
        newsScrapService.deleteUserScraps(userId);
        // 회원에게 종속된 AI 데이터(채팅방·메시지·응답 출처·피드백·피드백 사유)를 삭제
        aiWithdrawalService.deleteUserAiData(userId);
        // 3. 포인트(총점·적립 내역) 삭제. 조회가 guardianProfile을 경유하므로 withdraw()보다 먼저 수행한다.
        pointService.deleteUserPoints(userId);
        // 4 & 5 & 6. 개인정보 파기 + guardianProfile/childProfile/온보딩 orphanRemoval + 인증 수단 폐기
        UserWithdrawResponse response = userService.withdraw(userId);
        // 7. 프로필 이미지 S3 파일 삭제(커밋 이후)
        deleteProfileImageAfterCommit(profileImageUrl);

        return response;
    }

    private void deleteProfileImageAfterCommit(String profileImageUrl) {
        if (profileImageUrl == null || profileImageUrl.isBlank()) {
            return;
        }

        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    safeDeleteProfileImage(profileImageUrl);
                }
            });
        } else {
            safeDeleteProfileImage(profileImageUrl);
        }
    }

    // 프로필 이미지 삭제 실패가 탈퇴 처리(이미 커밋됨)를 방해하지 않도록 best-effort로 처리한다.
    private void safeDeleteProfileImage(String profileImageUrl) {
        try {
            s3ImageStorage.delete(profileImageUrl);
        } catch (RuntimeException e) {
            log.warn("회원 탈퇴 프로필 이미지 S3 삭제 실패: {}", profileImageUrl, e);
        }
    }
}
