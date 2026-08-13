package com.bodeum.domain.ai.service.support;

import static org.assertj.core.api.Assertions.assertThat;

import com.bodeum.domain.ai.entity.AiChatRoom;
import com.bodeum.domain.ai.entity.AiFeedback;
import com.bodeum.domain.ai.entity.AiFeedbackReason;
import com.bodeum.domain.ai.entity.AiMessage;
import com.bodeum.domain.ai.entity.AiResponseSource;
import com.bodeum.domain.ai.enums.AiAnswerStatus;
import com.bodeum.domain.ai.enums.AiFeedbackReasonType;
import com.bodeum.domain.ai.enums.AiFeedbackType;
import com.bodeum.domain.ai.enums.AiResponseSourceType;
import com.bodeum.domain.auth.enums.SocialProvider;
import com.bodeum.domain.user.entity.User;
import com.bodeum.global.config.JpaAuditingConfig;
import com.bodeum.global.config.QueryDslConfig;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;

@DataJpaTest
@Import({
        AiWithdrawalService.class,
        QueryDslConfig.class,
        JpaAuditingConfig.class
})
class AiWithdrawalPersistenceTest {

    @Autowired
    private EntityManager entityManager;
    @Autowired
    private AiWithdrawalService aiWithdrawalService;

    @Test
    void deleteUserAiDataDeletesOnlyTargetUsersAiData() {
        User target = persistUser("target");
        User other = persistUser("other");
        persistAiConversation(target);
        persistAiConversation(other);
        entityManager.flush();
        entityManager.clear();

        aiWithdrawalService.deleteUserAiData(target.getId());
        entityManager.flush();
        entityManager.clear();

        assertThat(countByUser("AiChatRoom", target.getId())).isZero();
        assertThat(countByUser("AiMessage", target.getId())).isZero();
        assertThat(countByUser("AiResponseSource", target.getId())).isZero();
        assertThat(countByUser("AiFeedback", target.getId())).isZero();
        assertThat(countByUser("AiFeedbackReason", target.getId())).isZero();

        assertThat(countByUser("AiChatRoom", other.getId())).isOne();
        assertThat(countByUser("AiMessage", other.getId())).isOne();
        assertThat(countByUser("AiResponseSource", other.getId())).isOne();
        assertThat(countByUser("AiFeedback", other.getId())).isOne();
        assertThat(countByUser("AiFeedbackReason", other.getId())).isOne();
    }

    private User persistUser(String providerUserId) {
        User user = User.createSocialUser(
                SocialProvider.KAKAO,
                providerUserId,
                providerUserId + "@example.com",
                providerUserId
        );
        entityManager.persist(user);
        return user;
    }

    private void persistAiConversation(User user) {
        AiChatRoom chatRoom = AiChatRoom.create(user);
        entityManager.persist(chatRoom);

        AiMessage message = AiMessage.createAiMessage(
                chatRoom,
                "답변",
                false,
                AiAnswerStatus.ANSWERED
        );
        entityManager.persist(message);
        entityManager.persist(AiResponseSource.create(
                message,
                AiResponseSourceType.INFO,
                1L,
                "출처",
                "https://example.com",
                null
        ));

        AiFeedback feedback = AiFeedback.create(message, AiFeedbackType.INCORRECT);
        entityManager.persist(feedback);
        entityManager.persist(AiFeedbackReason.create(feedback, AiFeedbackReasonType.ETC));
    }

    private long countByUser(String entityName, Long userId) {
        String association = switch (entityName) {
            case "AiChatRoom" -> "entity.user.id";
            case "AiMessage" -> "entity.chatRoom.user.id";
            case "AiResponseSource" -> "entity.aiMessage.chatRoom.user.id";
            case "AiFeedback" -> "entity.aiMessage.chatRoom.user.id";
            case "AiFeedbackReason" -> "entity.aiFeedback.aiMessage.chatRoom.user.id";
            default -> throw new IllegalArgumentException("Unsupported entity: " + entityName);
        };
        return entityManager.createQuery(
                        "select count(entity) from " + entityName + " entity where " + association + " = :userId",
                        Long.class
                )
                .setParameter("userId", userId)
                .getSingleResult();
    }
}
