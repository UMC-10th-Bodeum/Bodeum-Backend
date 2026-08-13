package com.bodeum.domain.ai.service.chat;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verifyNoInteractions;

import com.bodeum.domain.ai.entity.AiChatRoom;
import com.bodeum.domain.ai.exception.AiErrorCode;
import com.bodeum.domain.ai.repository.AiChatRoomRepository;
import com.bodeum.domain.ai.repository.AiMessageRepository;
import com.bodeum.domain.auth.enums.SocialProvider;
import com.bodeum.domain.user.entity.User;
import com.bodeum.domain.user.repository.UserRepository;
import com.bodeum.global.apiPayload.exception.ProjectException;
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
    private AiMessageRepository aiMessageRepository;

    @InjectMocks
    private AiChatRoomService aiChatRoomService;

    @Test
    void getsExistingChatRoomWithoutCreatingOne() {
        long userId = 1L;
        AiChatRoom chatRoom = chatRoom(10L);
        given(aiChatRoomRepository.findByUserId(userId)).willReturn(Optional.of(chatRoom));

        var response = aiChatRoomService.getChatRoom(userId);

        assertThat(response.aiChatRoomId()).isEqualTo(10L);
    }

    @Test
    void rejectsChatRoomLookupWhenRoomDoesNotExist() {
        given(aiChatRoomRepository.findByUserId(1L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> aiChatRoomService.getChatRoom(1L))
                .isInstanceOf(ProjectException.class)
                .extracting(exception -> ((ProjectException) exception).getErrorCode())
                .isEqualTo(AiErrorCode.AI_CHAT_ROOM_NOT_FOUND);
    }

    @Test
    void createsChatRoomWhenRoomDoesNotExist() {
        long userId = 1L;
        User user = user();
        AiChatRoom savedChatRoom = chatRoom(10L);
        given(aiChatRoomRepository.findByUserId(userId)).willReturn(Optional.empty());
        given(userRepository.findById(userId)).willReturn(Optional.of(user));
        given(aiChatRoomRepository.saveAndFlush(any(AiChatRoom.class)))
                .willReturn(savedChatRoom);

        var response = aiChatRoomService.createChatRoom(userId);

        assertThat(response.aiChatRoomId()).isEqualTo(10L);
        assertThat(response.showGuideModal()).isTrue();
        assertThat(response.hasTodayMessages()).isFalse();
        assertThat(response.hasPreviousMessages()).isFalse();
        verifyNoInteractions(aiMessageRepository);
    }

    @Test
    void rejectsChatRoomCreationWhenRoomAlreadyExists() {
        given(aiChatRoomRepository.findByUserId(1L))
                .willReturn(Optional.of(chatRoom(10L)));

        assertThatThrownBy(() -> aiChatRoomService.createChatRoom(1L))
                .isInstanceOf(ProjectException.class)
                .extracting(exception -> ((ProjectException) exception).getErrorCode())
                .isEqualTo(AiErrorCode.AI_CHAT_ROOM_ALREADY_EXISTS);
    }

    @Test
    void convertsConcurrentCreationConflictToAlreadyExistsError() {
        long userId = 1L;
        given(aiChatRoomRepository.findByUserId(userId)).willReturn(Optional.empty());
        given(userRepository.findById(userId)).willReturn(Optional.of(user()));
        given(aiChatRoomRepository.saveAndFlush(any(AiChatRoom.class)))
                .willThrow(new DataIntegrityViolationException("duplicate chat room"));

        assertThatThrownBy(() -> aiChatRoomService.createChatRoom(userId))
                .isInstanceOf(ProjectException.class)
                .extracting(exception -> ((ProjectException) exception).getErrorCode())
                .isEqualTo(AiErrorCode.AI_CHAT_ROOM_ALREADY_EXISTS);
    }

    private AiChatRoom chatRoom(Long id) {
        AiChatRoom chatRoom = AiChatRoom.create(user());
        ReflectionTestUtils.setField(chatRoom, "id", id);
        return chatRoom;
    }

    private User user() {
        return User.createSocialUser(
                SocialProvider.KAKAO,
                "kakao-1",
                "parent@example.com",
                "parent"
        );
    }
}
