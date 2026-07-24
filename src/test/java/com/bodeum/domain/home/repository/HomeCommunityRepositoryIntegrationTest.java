package com.bodeum.domain.home.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.bodeum.domain.community.entity.Comment;
import com.bodeum.domain.community.entity.Post;
import com.bodeum.domain.community.enums.CommentStatus;
import com.bodeum.domain.community.enums.PostAnonymityType;
import com.bodeum.domain.community.enums.PostBoardType;
import com.bodeum.domain.community.enums.PostStatus;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest(properties = "bodeum.auth.jwt-secret=test-jwt-secret-32-bytes-minimum-value")
@Transactional
class HomeCommunityRepositoryIntegrationTest {

    private static final int DEFAULT_PAGE_SIZE = 10;

    @Autowired
    private HomePostRepository homePostRepository;

    @Autowired
    private HomeCommentRepository homeCommentRepository;

    @Test
    void postQueriesExcludeHiddenAndDeletedPosts() {
        Post activePost = homePostRepository.save(post("활성 게시글"));

        Post hiddenPost = post("숨김 게시글");
        hiddenPost.hide();
        homePostRepository.save(hiddenPost);

        Post deletedPost = post("삭제 게시글");
        deletedPost.delete();
        homePostRepository.save(deletedPost);
        homePostRepository.flush();

        List<Post> latestPosts = homePostRepository
                .findAllByStatusAndDeletedAtIsNullOrderByCreatedAtDesc(
                        PostStatus.ACTIVE,
                        PageRequest.of(0, DEFAULT_PAGE_SIZE)
                );
        List<Post> popularPosts = homePostRepository.findTopByPopularity(
                PostStatus.ACTIVE,
                PageRequest.of(0, DEFAULT_PAGE_SIZE)
        );

        assertThat(latestPosts).extracting(Post::getId).containsExactly(activePost.getId());
        assertThat(popularPosts).extracting(Post::getId).containsExactly(activePost.getId());
    }

    @Test
    void commentCountExcludesDeletedComments() {
        Post post = homePostRepository.save(post("댓글 수 게시글"));
        Comment activeComment = Comment.create(post, 20L, "활성 댓글");
        Comment deletedComment = Comment.create(post, 21L, "삭제 댓글");
        deletedComment.delete();

        homeCommentRepository.saveAllAndFlush(List.of(activeComment, deletedComment));

        List<Object[]> counts = homeCommentRepository.countGroupByPostIdIn(
                List.of(post.getId()),
                CommentStatus.ACTIVE
        );

        assertThat(counts).hasSize(1);
        assertThat(counts.getFirst()[0]).isEqualTo(post.getId());
        assertThat(counts.getFirst()[1]).isEqualTo(1L);
    }

    @Test
    void popularityQueryOrdersPostsBySynchronizedLikeCount() {
        Post popularPost = post("인기 게시글");
        popularPost.increaseLikeCount();
        popularPost.increaseLikeCount();
        homePostRepository.saveAndFlush(popularPost);

        Post recentPost = post("최근 게시글");
        recentPost.increaseLikeCount();
        homePostRepository.saveAndFlush(recentPost);

        List<Post> posts = homePostRepository.findTopByPopularity(
                PostStatus.ACTIVE,
                PageRequest.of(0, DEFAULT_PAGE_SIZE)
        );

        assertThat(posts).extracting(Post::getId).containsExactly(
                popularPost.getId(),
                recentPost.getId()
        );
    }

    private Post post(String title) {
        return Post.create(
                10L,
                PostBoardType.FREE_COMMUNICATION,
                PostAnonymityType.PROFILE_TAG_VISIBLE,
                title,
                title + " 내용",
                false
        );
    }
}
