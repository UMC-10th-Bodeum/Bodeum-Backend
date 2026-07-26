package com.bodeum.domain.info.dto.response;

import static org.assertj.core.api.Assertions.assertThat;

import com.bodeum.domain.auth.enums.SocialProvider;
import com.bodeum.domain.info.entity.InfoReview;
import com.bodeum.domain.user.entity.User;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class InfoReviewResponseTest {

    private InfoReview reviewBy(User author) {
        InfoReview review = InfoReview.builder()
                .user(author)
                .infoItem(null)
                .rating(5)
                .content("좋아요")
                .build();
        ReflectionTestUtils.setField(review, "id", 3L);
        return review;
    }

    @Test
    void exposesNicknameWhenAuthorIsActive() {
        User author = User.createSocialUser(SocialProvider.KAKAO, "kakao-1", "a@example.com", "민준맘");
        ReflectionTestUtils.setField(author, "id", 10L);

        InfoReviewResponse response = InfoReviewResponse.from(reviewBy(author));

        assertThat(response.userId()).isEqualTo(10L);
        assertThat(response.userNickname()).isEqualTo("민준맘");
    }

    @Test
    void anonymizesWithdrawnAuthor() {
        User author = User.createSocialUser(SocialProvider.KAKAO, "kakao-1", "a@example.com", "민준맘");
        ReflectionTestUtils.setField(author, "id", 10L);
        author.withdraw();

        InfoReviewResponse response = InfoReviewResponse.from(reviewBy(author));

        assertThat(response.userId()).isNull();
        assertThat(response.userNickname()).isEqualTo("탈퇴한 사용자");
    }
}
