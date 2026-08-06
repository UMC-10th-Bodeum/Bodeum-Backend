package com.bodeum.domain.ai.service;

import static org.mockito.BDDMockito.then;

import com.bodeum.domain.ai.repository.AiChatRoomRepository;
import com.bodeum.domain.ai.repository.AiFeedbackReasonRepository;
import com.bodeum.domain.ai.repository.AiFeedbackRepository;
import com.bodeum.domain.ai.repository.AiMessageRepository;
import com.bodeum.domain.ai.repository.AiResponseSourceRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AiWithdrawalServiceTest {

    @Mock
    private AiFeedbackReasonRepository aiFeedbackReasonRepository;
    @Mock
    private AiFeedbackRepository aiFeedbackRepository;
    @Mock
    private AiResponseSourceRepository aiResponseSourceRepository;
    @Mock
    private AiMessageRepository aiMessageRepository;
    @Mock
    private AiChatRoomRepository aiChatRoomRepository;

    @InjectMocks
    private AiWithdrawalService aiWithdrawalService;

    @Test
    void deleteUserAiDataDeletesDependentDataBeforeChatRoom() {
        aiWithdrawalService.deleteUserAiData(1L);

        InOrder order = Mockito.inOrder(
                aiFeedbackReasonRepository,
                aiFeedbackRepository,
                aiResponseSourceRepository,
                aiMessageRepository,
                aiChatRoomRepository
        );
        then(aiFeedbackReasonRepository).should(order).deleteByUserId(1L);
        then(aiFeedbackRepository).should(order).deleteByUserId(1L);
        then(aiResponseSourceRepository).should(order).deleteByUserId(1L);
        then(aiMessageRepository).should(order).deleteByUserId(1L);
        then(aiChatRoomRepository).should(order).deleteByUserId(1L);
    }
}
