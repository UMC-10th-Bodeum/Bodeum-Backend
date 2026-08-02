package com.bodeum.domain.community.dto.response;

import static org.assertj.core.api.Assertions.assertThat;

import com.bodeum.domain.community.entity.Comment;
import com.bodeum.domain.community.entity.Post;
import com.bodeum.domain.community.enums.PostAnonymityType;
import com.bodeum.domain.community.enums.PostBoardType;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class CommentResponseTest {

    private Comment comment() {
        Post post = Post.create(1L, PostBoardType.FREE_COMMUNICATION,
                PostAnonymityType.PROFILE_TAG_VISIBLE, "제목", "내용");
        Comment comment = Comment.create(post, 10L, "댓글");
        ReflectionTestUtils.setField(comment, "id", 5L);
        return comment;
    }

    @Test
    void exposesAuthorIdWhenAuthorIsActive() {
        CommentResponse response = CommentResponse.of(comment(), null, 99L, false, false, List.of());

        assertThat(response.authorId()).isEqualTo(10L);
        assertThat(response.authorNickname()).isNull();
    }

    @Test
    void anonymizesWithdrawnAuthor() {
        CommentResponse response = CommentResponse.of(comment(), null, 99L, false, true, List.of());

        assertThat(response.authorId()).isNull();
        assertThat(response.authorNickname()).isEqualTo("탈퇴한 사용자");
    }
}
