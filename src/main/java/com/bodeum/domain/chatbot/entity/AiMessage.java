package com.bodeum.domain.chatbot.entity;

import com.bodeum.domain.chatbot.entity.enums.SenderType;
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

    @Lob
    @Column(name = "content", nullable = false)
    private String content;

    @Builder
    private AiMessage(
            AiChatRoom chatRoom,
            SenderType senderType,
            String content
    ) {
        this.chatRoom = chatRoom;
        this.senderType = senderType;
        this.content = content;
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
            String content
    ) {
        return AiMessage.builder()
                .chatRoom(chatRoom)
                .senderType(SenderType.AI)
                .content(content)
                .build();
    }
}
