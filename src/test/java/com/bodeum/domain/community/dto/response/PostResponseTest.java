package com.bodeum.domain.community.dto.response;

import static org.assertj.core.api.Assertions.assertThat;

import com.bodeum.domain.community.entity.Post;
import com.bodeum.domain.community.enums.PostAnonymityType;
import com.bodeum.domain.community.enums.PostBoardType;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class PostResponseTest {

    private Post post(PostAnonymityType anonymityType) {
        Post post = Post.create(10L, PostBoardType.FREE_COMMUNICATION, anonymityType, "제목", "내용");
        ReflectionTestUtils.setField(post, "id", 1L);
        return post;
    }

    @Test
    void exposesAuthorIdWhenAuthorIsActive() {
        PostResponse response = PostResponse.of(
                post(PostAnonymityType.PROFILE_TAG_VISIBLE), 99L, false, false, false, 3, 7,
                List.of(), List.of(), List.of());

        assertThat(response.authorId()).isEqualTo(10L);
        assertThat(response.authorNickname()).isNull();
        assertThat(response.authorLevel()).isEqualTo(3);
        assertThat(response.childAge()).isEqualTo(7);
    }

    @Test
    void anonymizesWithdrawnAuthor() {
        PostResponse response = PostResponse.of(
                post(PostAnonymityType.PROFILE_TAG_VISIBLE), 99L, false, false, true, 3, 7,
                List.of(), List.of(), List.of());

        assertThat(response.authorId()).isNull();
        assertThat(response.authorNickname()).isEqualTo("탈퇴한 사용자");
        assertThat(response.authorLevel()).isNull();
        assertThat(response.childAge()).isNull();
    }

    @Test
    void fullyAnonymousPostDoesNotRevealWithdrawal() {
        // 완전 익명 게시글은 탈퇴 저자여도 '탈퇴한 사용자'를 노출하지 않는다(탈퇴 사실 비노출).
        PostResponse response = PostResponse.of(
                post(PostAnonymityType.FULLY_ANONYMOUS), 99L, false, false, true, 3, 7,
                List.of(), List.of(), List.of());

        assertThat(response.authorId()).isNull();
        assertThat(response.authorNickname()).isNull();
        assertThat(response.authorLevel()).isNull();
        assertThat(response.childAge()).isNull();
    }
}
