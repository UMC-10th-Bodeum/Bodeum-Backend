package com.bodeum.domain.chatbot.entity;

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

//    TODO: User 엔티티 구현 후 user_id FK 및 UNIQUE 제약조건 적용 예정
//    @OneToOne(fetch = FetchType.LAZY)
//    @JoinColumn(name = "user_id", nullable = false, unique = true)
//    private User user;

    @Column(name = "last_message_at")
    private Instant lastMessageAt;

//    TODO: User 엔티티 구현 후 생성 메서드 추가
//    @Builder
//    private AiChatRoom(User user) {
//        this.user = user;
//    }
//
//    public static AiChatRoom create(User user) {
//        return AiChatRoom.builder()
//                .user(user)
//                .build();
//    }

    public void updateLastMessageAt(Instant lastMessageAt) {
        this.lastMessageAt = lastMessageAt;
    }
}