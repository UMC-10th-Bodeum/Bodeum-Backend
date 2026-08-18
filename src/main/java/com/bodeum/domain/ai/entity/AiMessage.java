package com.bodeum.domain.ai.entity;

import com.bodeum.domain.ai.enums.AiResponseProcessingStatus;
import com.bodeum.domain.ai.enums.AiAnswerStatus;
import com.bodeum.domain.ai.enums.SenderType;
import com.bodeum.domain.ai.model.context.AiResolvedContext;
import com.bodeum.global.common.entity.BaseCreatedEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

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

    @Column(name = "resolved_question", columnDefinition = "TEXT")
    private String resolvedQuestion;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "resolved_context", columnDefinition = "JSON")
    private AiResolvedContext resolvedContext;

    @Column(name = "context_parent_message_id")
    private Long contextParentMessageId;

    @Column(name = "context_root_message_id")
    private Long contextRootMessageId;

    @Column(name = "is_warning", nullable = false)
    private boolean warning = false;

    @Enumerated(EnumType.STRING)
    @Column(name = "ai_processing_status", length = 20)
    private AiResponseProcessingStatus aiProcessingStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "ai_answer_status", length = 50)
    private AiAnswerStatus aiAnswerStatus;

    @Builder
    private AiMessage(
            AiChatRoom chatRoom,
            SenderType senderType,
            String content,
            boolean warning,
            AiResponseProcessingStatus aiProcessingStatus,
            AiAnswerStatus aiAnswerStatus
    ) {
        this.chatRoom = chatRoom;
        this.senderType = senderType;
        this.content = content;
        this.warning = warning;
        this.aiProcessingStatus = aiProcessingStatus;
        this.aiAnswerStatus = aiAnswerStatus;
    }

    public static AiMessage createUserMessage(
            AiChatRoom chatRoom,
            String content
    ) {
        return AiMessage.builder()
                .chatRoom(chatRoom)
                .senderType(SenderType.USER)
                .content(content)
                .aiProcessingStatus(AiResponseProcessingStatus.PROCESSING)
                .build();
    }

    public static AiMessage createAiMessage(
            AiChatRoom chatRoom,
            String content,
            boolean warning,
            AiAnswerStatus answerStatus
    ) {
        return AiMessage.builder()
                .chatRoom(chatRoom)
                .senderType(SenderType.AI)
                .content(content)
                .warning(warning)
                .aiProcessingStatus(null)
                .aiAnswerStatus(answerStatus)
                .build();
    }

    public void completeAiResponse() {
        validateUserMessage();
        if (aiProcessingStatus == AiResponseProcessingStatus.PROCESSING) {
            aiProcessingStatus = AiResponseProcessingStatus.COMPLETED;
        }
    }

    public boolean isAiResponseProcessing() {
        return senderType == SenderType.USER
                && aiProcessingStatus == AiResponseProcessingStatus.PROCESSING;
    }

    public void updateConversationContext(
            String resolvedQuestion,
            AiResolvedContext resolvedContext,
            Long contextParentMessageId,
            Long contextRootMessageId
    ) {
        validateUserMessage();
        this.resolvedQuestion = resolvedQuestion == null || resolvedQuestion.isBlank()
                ? content
                : resolvedQuestion.trim();
        this.resolvedContext = resolvedContext == null || resolvedContext.isEmpty()
                ? null
                : resolvedContext;
        this.contextParentMessageId = contextParentMessageId;
        this.contextRootMessageId = contextRootMessageId;
    }

    public void inheritConversationContext(AiMessage userMessage) {
        if (senderType != SenderType.AI || userMessage.senderType != SenderType.USER) {
            throw new IllegalStateException(
                    "AI message can inherit context only from a USER message");
        }
        this.resolvedQuestion = userMessage.resolvedQuestion;
        this.resolvedContext = userMessage.resolvedContext;
        this.contextParentMessageId = userMessage.id;
        this.contextRootMessageId = userMessage.contextRootMessageId;
    }

    public void failAiResponse() {
        validateUserMessage();
        if (aiProcessingStatus == AiResponseProcessingStatus.PROCESSING) {
            aiProcessingStatus = AiResponseProcessingStatus.FAILED;
        }
    }

    private void validateUserMessage() {
        if (senderType != SenderType.USER) {
            throw new IllegalStateException("AI response status belongs only to USER messages");
        }
    }
}
