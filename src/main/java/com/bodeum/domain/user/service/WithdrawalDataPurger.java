package com.bodeum.domain.user.service;

import com.bodeum.domain.ai.repository.AiChatRoomRepository;
import com.bodeum.domain.ai.repository.AiFeedbackRepository;
import com.bodeum.domain.ai.repository.AiMessageRepository;
import com.bodeum.domain.ai.repository.AiResponseSourceRepository;
import com.bodeum.domain.auth.repository.AuthLoginCodeRepository;
import com.bodeum.domain.community.repository.PostScrapRepository;
import com.bodeum.domain.info.repository.InfoScrapRepository;
import com.bodeum.domain.mypage.repository.GuardianPointHistoryRepository;
import com.bodeum.domain.mypage.repository.GuardianPointRepository;
import com.bodeum.domain.news.repository.NewsScrapRepository;
import com.bodeum.domain.search.repository.SearchLogRepository;
import com.bodeum.domain.user.repository.GuardianProfileRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * 회원 탈퇴 시 개인정보성 데이터를 파기한다.
 * 공개 콘텐츠(글·댓글·리뷰)와 좋아요·신고 등 보존 대상은 건드리지 않고, 개인 활동 데이터만 삭제한다.
 * 자식 → 부모 순으로 삭제해 FK 무결성을 지킨다. 반드시 User.withdraw()가 연관을 정리하기 전에 호출한다.
 */
@Component
@RequiredArgsConstructor
public class WithdrawalDataPurger {

    private final AuthLoginCodeRepository authLoginCodeRepository;
    private final SearchLogRepository searchLogRepository;
    private final PostScrapRepository postScrapRepository;
    private final InfoScrapRepository infoScrapRepository;
    private final NewsScrapRepository newsScrapRepository;
    private final AiChatRoomRepository aiChatRoomRepository;
    private final AiMessageRepository aiMessageRepository;
    private final AiResponseSourceRepository aiResponseSourceRepository;
    private final AiFeedbackRepository aiFeedbackRepository;
    private final GuardianProfileRepository guardianProfileRepository;
    private final GuardianPointRepository guardianPointRepository;
    private final GuardianPointHistoryRepository guardianPointHistoryRepository;

    @Transactional
    public void purge(Long userId) {
        // 일회용 로그인 code, 검색 기록
        authLoginCodeRepository.deleteByUserId(userId);
        searchLogRepository.deleteByUserId(userId);

        // 스크랩(비공개 개인 활동)
        postScrapRepository.deleteByUserId(userId);
        infoScrapRepository.deleteByUserId(userId);
        newsScrapRepository.deleteByUserId(userId);

        purgeAiConversation(userId);
        purgeGuardianPoints(userId);
    }

    // AI 대화: 응답 출처·피드백 → 메시지 → 채팅방 순으로 삭제한다(회원당 채팅방 1개).
    private void purgeAiConversation(Long userId) {
        aiChatRoomRepository.findByUserId(userId).ifPresent(chatRoom -> {
            Long chatRoomId = chatRoom.getId();
            aiResponseSourceRepository.deleteByChatRoomId(chatRoomId);
            aiFeedbackRepository.deleteByChatRoomId(chatRoomId);
            aiMessageRepository.deleteByChatRoomId(chatRoomId);
            aiChatRoomRepository.delete(chatRoom);
        });
    }

    // 포인트: 이력 → 포인트 순으로 삭제한다. GuardianProfile은 이후 User.withdraw()가 orphanRemoval로 지운다.
    private void purgeGuardianPoints(Long userId) {
        guardianProfileRepository.findIdByUserId(userId).ifPresent(guardianProfileId -> {
            List<Long> pointIds = guardianPointRepository.findIdsByGuardianProfileId(guardianProfileId);
            if (!pointIds.isEmpty()) {
                guardianPointHistoryRepository.deleteByGuardianPointIdIn(pointIds);
            }
            guardianPointRepository.deleteByGuardianProfileId(guardianProfileId);
        });
    }
}
