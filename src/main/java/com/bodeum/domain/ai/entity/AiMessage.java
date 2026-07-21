package com.bodeum.domain.ai.entity;

import com.bodeum.domain.ai.enums.AiResponseProcessingStatus;
import com.bodeum.domain.ai.enums.SenderType;
import com.bodeum.global.common.entity.BaseCreatedEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "ai_message")
public class AiMessage extends BaseCreatedEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ai_message_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ai_chat_room_id", nullable = false)
    private AiChatRoom chatRoom;

    @Enumerated(EnumType.STRING)
    @Column(name = "sender_type", nullable = false)
    private SenderType senderType;

    @Column(name = "content", nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(name = "is_warning", nullable = false)
    private boolean warning = false;

    @Enumerated(EnumType.STRING)
    @Column(name = "ai_response_status", length = 20)
    private AiResponseProcessingStatus aiResponseStatus;

    @Builder
    private AiMessage(
            AiChatRoom chatRoom,
            SenderType senderType,
            String content,
            boolean warning,
            AiResponseProcessingStatus aiResponseStatus
    ) {
        this.chatRoom = chatRoom;
        this.senderType = senderType;
        this.content = content;
        this.warning = warning;
        this.aiResponseStatus = aiResponseStatus;
    }

    public static AiMessage createUserMessage(
            AiChatRoom chatRoom,
            String content
    ) {
        return AiMessage.builder()
                .chatRoom(chatRoom)
                .senderType(SenderType.USER)
                .content(content)
                .build();
    }

    public static AiMessage createAiMessage(
            AiChatRoom chatRoom,
            String content,
            boolean warning
    ) {
        return AiMessage.builder()
                .chatRoom(chatRoom)
                .senderType(SenderType.AI)
                .content(content)
                .warning(warning)
                .aiResponseStatus(null)
                .build();
    }

    public void completeAiResponse() {
        validateUserMessage();
        if (aiResponseStatus == AiResponseProcessingStatus.PROCESSING) {
            aiResponseStatus = AiResponseProcessingStatus.COMPLETED;
        }
    }

    public void failAiResponse() {
        validateUserMessage();
        if (aiResponseStatus == AiResponseProcessingStatus.PROCESSING) {
            aiResponseStatus = AiResponseProcessingStatus.FAILED;
        }
    }

    private void validateUserMessage() {
        if (senderType != SenderType.USER) {
            throw new IllegalStateException("AI response status belongs only to USER messages");
        }
    }
}
