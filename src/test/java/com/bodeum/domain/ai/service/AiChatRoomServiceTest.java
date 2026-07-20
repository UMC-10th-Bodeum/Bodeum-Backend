package com.bodeum.domain.ai.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

import com.bodeum.domain.ai.dto.response.AiChatRoomResponse;
import com.bodeum.domain.ai.entity.AiChatRoom;
import com.bodeum.domain.ai.repository.AiChatRoomRepository;
import com.bodeum.domain.ai.repository.AiMessageRepository;
import com.bodeum.domain.auth.enums.SocialProvider;
import com.bodeum.domain.user.entity.User;
import com.bodeum.domain.user.entity.UserAgreement;
import com.bodeum.domain.user.repository.UserAgreementRepository;
import com.bodeum.domain.user.repository.UserRepository;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class AiChatRoomServiceTest {

    @Mock
    private AiChatRoomRepository aiChatRoomRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserAgreementRepository userAgreementRepository;

    @Mock
    private AiMessageRepository aiMessageRepository;

    @InjectMocks
    private AiChatRoomService aiChatRoomService;

    @Test
    void getOrCreateChatRoomReturnsConcurrentRequestResultWhenCreationConflicts() {
        long userId = 1L;
        User user = User.createSocialUser(
                SocialProvider.KAKAO, "kakao-1", "parent@example.com", "parent"
        );
        AiChatRoom concurrentlyCreatedRoom = AiChatRoom.create(user);
        ReflectionTestUtils.setField(concurrentlyCreatedRoom, "id", 10L);

        given(userAgreementRepository.findByUserId(userId))
                .willReturn(Optional.of(UserAgreement.create(user, true, true, true)));
        given(aiChatRoomRepository.findByUserId(userId))
                .willReturn(Optional.empty(), Optional.of(concurrentlyCreatedRoom));
        given(userRepository.findById(userId)).willReturn(Optional.of(user));
        given(aiChatRoomRepository.saveAndFlush(org.mockito.ArgumentMatchers.any(AiChatRoom.class)))
                .willThrow(new DataIntegrityViolationException("duplicate chat room"));

        AiChatRoomResponse response = aiChatRoomService.getOrCreateChatRoom(userId);

        assertThat(response.aiChatRoomId()).isEqualTo(10L);
    }
}
