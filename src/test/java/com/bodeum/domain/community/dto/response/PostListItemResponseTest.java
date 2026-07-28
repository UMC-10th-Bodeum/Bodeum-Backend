package com.bodeum.domain.community.dto.response;

import static org.assertj.core.api.Assertions.assertThat;

import com.bodeum.domain.community.entity.Post;
import com.bodeum.domain.community.enums.PostAnonymityType;
import com.bodeum.domain.community.enums.PostBoardType;
import org.junit.jupiter.api.Test;

class PostListItemResponseTest {

    @Test
    void ofLimitsContentToPreviewLength() {
        String content = "가".repeat(PostListItemResponse.CONTENT_PREVIEW_MAX_LENGTH + 1);
        Post post = post(content);

        PostListItemResponse response = PostListItemResponse.of(post, null, null, null, false);

        assertThat(response.content())
                .hasSize(PostListItemResponse.CONTENT_PREVIEW_MAX_LENGTH)
                .isEqualTo(content.substring(0, PostListItemResponse.CONTENT_PREVIEW_MAX_LENGTH));
    }

    @Test
    void ofKeepsShortContentUnchanged() {
        String content = "짧은 게시글 본문";
        Post post = post(content);

        PostListItemResponse response = PostListItemResponse.of(post, null, null, null, false);

        assertThat(response.content()).isEqualTo(content);
    }

    private Post post(String content) {
        return Post.create(
                10L,
                PostBoardType.FREE_COMMUNICATION,
                PostAnonymityType.FULLY_ANONYMOUS,
                "게시글 제목",
                content,
                false
        );
    }
}
