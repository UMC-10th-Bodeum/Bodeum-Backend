package com.bodeum.domain.ai.service;

import com.bodeum.domain.ai.dto.response.AiChatRoomResponse;
import com.bodeum.domain.ai.dto.response.AiGuideConfirmationResponse;
import com.bodeum.domain.ai.entity.AiChatRoom;
import com.bodeum.domain.ai.exception.AiErrorCode;
import com.bodeum.domain.ai.repository.AiChatRoomRepository;
import com.bodeum.domain.ai.repository.AiMessageRepository;
import com.bodeum.domain.user.entity.User;
import com.bodeum.domain.user.entity.UserAgreement;
import com.bodeum.domain.user.exception.UserErrorCode;
import com.bodeum.domain.user.repository.UserAgreementRepository;
import com.bodeum.domain.user.repository.UserRepository;
import com.bodeum.global.apiPayload.exception.ProjectException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

import static com.bodeum.global.common.constant.TimeConstants.SERVICE_ZONE_ID;

@Service
@RequiredArgsConstructor
public class AiChatRoomService {

    private static final long GUIDE_RESHOW_DAYS = 20;

    private final AiChatRoomRepository aiChatRoomRepository;
    private final UserRepository userRepository;
    private final UserAgreementRepository userAgreementRepository;
    private final AiMessageRepository aiMessageRepository;

    @Transactional
    public AiGuideConfirmationResponse confirmGuide(Long userId) {
        AiChatRoom chatRoom = aiChatRoomRepository.findByUserId(userId)
                .orElseThrow(() -> new ProjectException(AiErrorCode.AI_CHAT_ROOM_NOT_FOUND));

        chatRoom.confirmGuide();

        return AiGuideConfirmationResponse.of(
                chatRoom.getLastGuideConfirmedAt()
        );
    }

    @Transactional
    public AiChatRoomResponse getOrCreateChatRoom(Long userId) {
        validateAiTermsAgreement(userId);

        AiChatRoom chatRoom = aiChatRoomRepository.findByUserId(userId)
                .orElseGet(() -> createChatRoom(userId));

        // 한국 시간 기준 오늘 0시를 Instant로 변환
        Instant now = Instant.now();
        Instant startOfToday = LocalDate.now(SERVICE_ZONE_ID)
                .atStartOfDay(SERVICE_ZONE_ID)
                .toInstant();

        boolean showGuideModal = shouldShowGuideModal(chatRoom, now);

        boolean hasTodayMessages =
                chatRoom.getLastMessageAt() != null
                        && !chatRoom.getLastMessageAt().isBefore(startOfToday);

        // 한국 시간 기준 7일 전 0시를 Instant로 변환
        Instant previousMessageStart = LocalDate.now(SERVICE_ZONE_ID)
                .minusDays(7)
                .atStartOfDay(SERVICE_ZONE_ID)
                .toInstant();

        boolean hasPreviousMessages =
                aiMessageRepository
                        .existsByChatRoomIdAndCreatedAtGreaterThanEqualAndCreatedAtLessThan(
                                chatRoom.getId(),
                                previousMessageStart,
                                startOfToday
                        );

        return AiChatRoomResponse.of(
                chatRoom.getId(),
                chatRoom.getCreatedAt(),
                showGuideModal,
                hasTodayMessages,
                hasPreviousMessages
        );
    }

    private void validateAiTermsAgreement(Long userId) {
        UserAgreement agreement = userAgreementRepository.findByUserId(userId)
                .orElseThrow(() -> new ProjectException(UserErrorCode.USER_AGREEMENT_NOT_FOUND));

        if (!agreement.isAiTermsAgreed()) {
            throw new ProjectException(AiErrorCode.AI_TERMS_NOT_AGREED);
        }
    }

    private AiChatRoom createChatRoom(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ProjectException(UserErrorCode.USER_NOT_FOUND));

        return aiChatRoomRepository.save(
                AiChatRoom.create(user)
        );
    }

    private boolean shouldShowGuideModal(
            AiChatRoom chatRoom,
            Instant now
    ) {
        Instant lastGuideConfirmedAt = chatRoom.getLastGuideConfirmedAt();

        if (lastGuideConfirmedAt == null) {
            return true;
        }

        Instant lastActivityAt = getLastActivityAt(chatRoom);

        Instant guideReshowAt =
                lastActivityAt.plus(GUIDE_RESHOW_DAYS, ChronoUnit.DAYS);

        return !guideReshowAt.isAfter(now);
    }

    private Instant getLastActivityAt(AiChatRoom chatRoom) {
        Instant lastGuideConfirmedAt = chatRoom.getLastGuideConfirmedAt();
        Instant lastMessageAt = chatRoom.getLastMessageAt();

        if (lastMessageAt == null) {
            return lastGuideConfirmedAt;
        }

        return lastMessageAt.isAfter(lastGuideConfirmedAt)
                ? lastMessageAt
                : lastGuideConfirmedAt;
    }
}
