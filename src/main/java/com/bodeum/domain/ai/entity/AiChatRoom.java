package com.bodeum.domain.ai.entity;

import com.bodeum.domain.user.entity.User;
import com.bodeum.global.common.entity.BaseCreatedEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "ai_chat_room")
public class AiChatRoom extends BaseCreatedEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ai_chat_room_id")
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column(name = "last_message_at")
    private Instant lastMessageAt;

    @Column(name = "last_guide_confirmed_at")
    private Instant lastGuideConfirmedAt;

    @Builder
    private AiChatRoom(User user) {
        this.user = user;
    }

    public static AiChatRoom create(User user) {
        return new AiChatRoom(user);
    }

    public void updateLastMessageAt(Instant lastMessageAt) {
        this.lastMessageAt = lastMessageAt;
    }

    public void confirmGuide() {
        this.lastGuideConfirmedAt = Instant.now();
    }
}