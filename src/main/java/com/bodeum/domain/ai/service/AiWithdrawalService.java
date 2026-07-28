package com.bodeum.domain.ai.service;

import com.bodeum.domain.ai.repository.AiChatRoomRepository;
import com.bodeum.domain.ai.repository.AiFeedbackReasonRepository;
import com.bodeum.domain.ai.repository.AiFeedbackRepository;
import com.bodeum.domain.ai.repository.AiMessageRepository;
import com.bodeum.domain.ai.repository.AiResponseSourceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AiWithdrawalService {

    private final AiFeedbackReasonRepository aiFeedbackReasonRepository;
    private final AiFeedbackRepository aiFeedbackRepository;
    private final AiResponseSourceRepository aiResponseSourceRepository;
    private final AiMessageRepository aiMessageRepository;
    private final AiChatRoomRepository aiChatRoomRepository;

    /**
     * 회원에게 종속된 AI 채팅 및 피드백 데이터를
     * 외래 키 제약을 고려하여 자식 엔티티부터 삭제 처리
     *
     * 공용 출처 데이터(ai_external_source, ai_external_document),
     * 출처 검토 이력(ai_source_review),
     * Chroma의 INFO/NEWS 색인은 유지
     */
    @Transactional
    public void deleteUserAiData(Long userId) {
        aiFeedbackReasonRepository.deleteByUserId(userId);
        aiFeedbackRepository.deleteByUserId(userId);
        aiResponseSourceRepository.deleteByUserId(userId);
        aiMessageRepository.deleteByUserId(userId);
        aiChatRoomRepository.deleteByUserId(userId);
    }
}
