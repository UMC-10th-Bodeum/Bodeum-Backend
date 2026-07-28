package com.bodeum.domain.ai.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bodeum.domain.ai.dto.request.CreateAiFeedbackRequest;
import com.bodeum.domain.ai.entity.AiChatRoom;
import com.bodeum.domain.ai.entity.AiFeedback;
import com.bodeum.domain.ai.entity.AiMessage;
import com.bodeum.domain.ai.enums.AiAnswerStatus;
import com.bodeum.domain.ai.enums.AiFeedbackReasonType;
import com.bodeum.domain.ai.enums.AiFeedbackType;
import com.bodeum.domain.ai.enums.SenderType;
import com.bodeum.domain.ai.exception.AiErrorCode;
import com.bodeum.domain.ai.repository.AiFeedbackReasonRepository;
import com.bodeum.domain.ai.repository.AiFeedbackRepository;
import com.bodeum.domain.ai.repository.AiMessageRepository;
import com.bodeum.domain.ai.repository.AiSourceReviewRepository;
import com.bodeum.domain.user.entity.User;
import com.bodeum.global.apiPayload.exception.ProjectException;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.CannotAcquireLockException;

@ExtendWith(MockitoExtension.class)
class AiFeedbackServiceTest {

    @Mock
    private AiMessageRepository aiMessageRepository;
    @Mock
    private AiFeedbackRepository aiFeedbackRepository;
    @Mock
    private AiFeedbackReasonRepository aiFeedbackReasonRepository;
    @Mock
    private AiSourceReviewRepository aiSourceReviewRepository;
    @Mock
    private AiMessage message;
    @Mock
    private AiChatRoom chatRoom;
    @Mock
    private User user;
    @Mock
    private AiFeedback savedFeedback;

    private AiFeedbackService aiFeedbackService;

    @BeforeEach
    void setUp() {
        aiFeedbackService = new AiFeedbackService(
                aiMessageRepository,
                aiFeedbackRepository,
                aiFeedbackReasonRepository,
                aiSourceReviewRepository
        );
    }

    @Test
    void createsIncorrectFeedbackAndReasons() {
        prepareOwnedAiMessage();
        when(message.getAiAnswerStatus()).thenReturn(AiAnswerStatus.ANSWERED);
        when(aiFeedbackRepository.existsByAiMessageId(12L)).thenReturn(false);
        when(aiFeedbackRepository.save(any(AiFeedback.class))).thenReturn(savedFeedback);
        when(savedFeedback.getId()).thenReturn(1L);
        when(savedFeedback.getFeedbackType()).thenReturn(AiFeedbackType.INCORRECT);

        var response = aiFeedbackService.createFeedback(
                10L,
                12L,
                new CreateAiFeedbackRequest(
                        AiFeedbackType.INCORRECT,
                        List.of(AiFeedbackReasonType.TIME, AiFeedbackReasonType.ELIGIBILITY)
                )
        );

        assertThat(response.aiFeedbackId()).isEqualTo(1L);
        assertThat(response.reasons())
                .containsExactly(AiFeedbackReasonType.TIME, AiFeedbackReasonType.ELIGIBILITY);
        verify(aiFeedbackReasonRepository).saveAll(any());
        verify(aiSourceReviewRepository)
                .markReviewRequiredByIncorrectFeedbackCount(12L, 3);
    }

    @Test
    void rejectsAnotherUsersMessage() {
        when(aiMessageRepository.findByIdForFeedback(12L)).thenReturn(Optional.of(message));
        when(message.getChatRoom()).thenReturn(chatRoom);
        when(chatRoom.getUser()).thenReturn(user);
        when(user.getId()).thenReturn(99L);

        assertThatThrownBy(() -> aiFeedbackService.createFeedback(
                10L,
                12L,
                new CreateAiFeedbackRequest(AiFeedbackType.HELPFUL, null)
        ))
                .isInstanceOf(ProjectException.class)
                .extracting(exception -> ((ProjectException) exception).getErrorCode())
                .isEqualTo(AiErrorCode.AI_MESSAGE_FORBIDDEN);

        verify(aiFeedbackRepository, never()).save(any());
    }

    @Test
    void rejectsUserMessage() {
        prepareOwnedMessage(SenderType.USER);

        assertThatThrownBy(() -> aiFeedbackService.createFeedback(
                10L,
                12L,
                new CreateAiFeedbackRequest(AiFeedbackType.HELPFUL, null)
        ))
                .isInstanceOf(ProjectException.class)
                .extracting(exception -> ((ProjectException) exception).getErrorCode())
                .isEqualTo(AiErrorCode.AI_FEEDBACK_AI_MESSAGE_ONLY);
    }

    @Test
    void rejectsDuplicateFeedback() {
        prepareOwnedAiMessage();
        when(aiFeedbackRepository.existsByAiMessageId(12L)).thenReturn(true);

        assertThatThrownBy(() -> aiFeedbackService.createFeedback(
                10L,
                12L,
                new CreateAiFeedbackRequest(AiFeedbackType.HELPFUL, null)
        ))
                .isInstanceOf(ProjectException.class)
                .extracting(exception -> ((ProjectException) exception).getErrorCode())
                .isEqualTo(AiErrorCode.ALREADY_FEEDBACK);
    }

    @Test
    void convertsMessageLockFailureToTemporaryUnavailableError() {
        when(aiMessageRepository.findByIdForFeedback(12L))
                .thenThrow(new CannotAcquireLockException("lock timeout"));

        assertThatThrownBy(() -> aiFeedbackService.createFeedback(
                10L,
                12L,
                new CreateAiFeedbackRequest(AiFeedbackType.HELPFUL, null)
        ))
                .isInstanceOf(ProjectException.class)
                .extracting(exception -> ((ProjectException) exception).getErrorCode())
                .isEqualTo(AiErrorCode.AI_FEEDBACK_TEMPORARILY_UNAVAILABLE);
    }

    @Test
    void convertsSourceReviewLockFailureToTemporaryUnavailableError() {
        prepareOwnedAiMessage();
        when(message.getAiAnswerStatus()).thenReturn(AiAnswerStatus.ANSWERED);
        when(aiFeedbackRepository.existsByAiMessageId(12L)).thenReturn(false);
        when(aiFeedbackRepository.save(any(AiFeedback.class))).thenReturn(savedFeedback);
        when(aiSourceReviewRepository.markReviewRequiredByIncorrectFeedbackCount(12L, 3))
                .thenThrow(new CannotAcquireLockException("deadlock"));

        assertThatThrownBy(() -> aiFeedbackService.createFeedback(
                10L,
                12L,
                new CreateAiFeedbackRequest(
                        AiFeedbackType.INCORRECT,
                        List.of(AiFeedbackReasonType.TIME)
                )
        ))
                .isInstanceOf(ProjectException.class)
                .extracting(exception -> ((ProjectException) exception).getErrorCode())
                .isEqualTo(AiErrorCode.AI_FEEDBACK_TEMPORARILY_UNAVAILABLE);
    }

    @Test
    void savesLinkGuidanceFeedbackWithoutUpdatingSourceReview() {
        prepareOwnedAiMessage();
        when(message.getAiAnswerStatus()).thenReturn(AiAnswerStatus.LINK_GUIDANCE);
        when(aiFeedbackRepository.existsByAiMessageId(12L)).thenReturn(false);
        when(aiFeedbackRepository.save(any(AiFeedback.class))).thenReturn(savedFeedback);
        when(savedFeedback.getId()).thenReturn(1L);
        when(savedFeedback.getFeedbackType()).thenReturn(AiFeedbackType.INCORRECT);

        var response = aiFeedbackService.createFeedback(
                10L,
                12L,
                new CreateAiFeedbackRequest(
                        AiFeedbackType.INCORRECT,
                        List.of(AiFeedbackReasonType.ETC)
                )
        );

        assertThat(response.feedbackType()).isEqualTo(AiFeedbackType.INCORRECT);
        verify(aiSourceReviewRepository, never())
                .markReviewRequiredByIncorrectFeedbackCount(anyLong(), anyInt());
    }

    @Test
    void savesNoEvidenceFeedbackWithoutUpdatingSourceReview() {
        prepareOwnedAiMessage();
        when(message.getAiAnswerStatus()).thenReturn(AiAnswerStatus.NO_EVIDENCE);
        when(aiFeedbackRepository.existsByAiMessageId(12L)).thenReturn(false);
        when(aiFeedbackRepository.save(any(AiFeedback.class))).thenReturn(savedFeedback);
        when(savedFeedback.getId()).thenReturn(1L);
        when(savedFeedback.getFeedbackType()).thenReturn(AiFeedbackType.INCORRECT);

        var response = aiFeedbackService.createFeedback(
                10L,
                12L,
                new CreateAiFeedbackRequest(
                        AiFeedbackType.INCORRECT,
                        List.of(AiFeedbackReasonType.ETC)
                )
        );

        assertThat(response.feedbackType()).isEqualTo(AiFeedbackType.INCORRECT);
        verify(aiSourceReviewRepository, never())
                .markReviewRequiredByIncorrectFeedbackCount(anyLong(), anyInt());
    }

    private void prepareOwnedAiMessage() {
        prepareOwnedMessage(SenderType.AI);
    }

    private void prepareOwnedMessage(SenderType senderType) {
        when(aiMessageRepository.findByIdForFeedback(12L)).thenReturn(Optional.of(message));
        when(message.getChatRoom()).thenReturn(chatRoom);
        when(chatRoom.getUser()).thenReturn(user);
        when(user.getId()).thenReturn(10L);
        when(message.getSenderType()).thenReturn(senderType);
    }
}
