package com.bodeum.domain.community.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.bodeum.domain.community.dto.request.CreatePostRequest;
import com.bodeum.domain.community.dto.response.PostResponse;
import com.bodeum.domain.community.entity.Comment;
import com.bodeum.domain.community.entity.CommentLike;
import com.bodeum.domain.community.entity.Post;
import com.bodeum.domain.community.entity.PostLike;
import com.bodeum.domain.community.entity.PostReport;
import com.bodeum.domain.community.entity.PostScrap;
import com.bodeum.domain.community.enums.DisabilityType;
import com.bodeum.domain.community.enums.PostAnonymityType;
import com.bodeum.domain.community.enums.PostBoardType;
import com.bodeum.domain.community.enums.PostStatus;
import com.bodeum.domain.community.repository.CommentLikeRepository;
import com.bodeum.domain.community.repository.CommentRepository;
import com.bodeum.domain.community.repository.PostDisabilityTagRepository;
import com.bodeum.domain.community.repository.PostHashtagRepository;
import com.bodeum.domain.community.repository.PostImageRepository;
import com.bodeum.domain.community.repository.PostLikeRepository;
import com.bodeum.domain.community.repository.PostReportRepository;
import com.bodeum.domain.community.repository.PostRepository;
import com.bodeum.domain.community.repository.PostScrapRepository;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest(properties = "bodeum.auth.jwt-secret=test-jwt-secret-32-bytes-minimum-value")
@Transactional
class PostServiceIntegrationTest {

    @Autowired
    private PostService postService;
    @Autowired
    private PostRepository postRepository;
    @Autowired
    private PostDisabilityTagRepository postDisabilityTagRepository;
    @Autowired
    private PostHashtagRepository postHashtagRepository;
    @Autowired
    private PostImageRepository postImageRepository;
    @Autowired
    private PostLikeRepository postLikeRepository;
    @Autowired
    private PostScrapRepository postScrapRepository;
    @Autowired
    private PostReportRepository postReportRepository;
    @Autowired
    private CommentRepository commentRepository;
    @Autowired
    private CommentLikeRepository commentLikeRepository;

    @Test
    void createAndReadAnonymousPostWithRelatedData() {
        PostResponse created = postService.createPost(
                10L,
                new CreatePostRequest(
                        PostBoardType.FREE_COMMUNICATION,
                        PostAnonymityType.FULLY_ANONYMOUS,
                        "익명 게시글",
                        "익명 게시글 내용",
                        List.of(DisabilityType.AUTISM, DisabilityType.AUTISM, DisabilityType.ADHD),
                        List.of("육아", "정보공유"),
                        List.of("https://example.com/1.jpg", "https://example.com/2.jpg"),
                        true
                )
        );

        PostResponse viewed = postService.getPost(20L, created.postId());

        assertThat(created.authorId()).isNull();
        assertThat(created.isMine()).isTrue();
        assertThat(created.isQuestion()).isTrue();
        assertThat(viewed.authorId()).isNull();
        assertThat(viewed.isMine()).isFalse();
        assertThat(viewed.viewCount()).isEqualTo(1);
        assertThat(viewed.disabilityTypes()).containsExactly(DisabilityType.AUTISM, DisabilityType.ADHD);
        assertThat(viewed.hashtags()).containsExactly("육아", "정보공유");
        assertThat(viewed.imageUrls()).containsExactly(
                "https://example.com/1.jpg",
                "https://example.com/2.jpg"
        );
    }

    @Test
    void deletePostSoftDeletesPostAndKeepsRelatedData() {
        Post post = postRepository.saveAndFlush(Post.create(
                10L,
                PostBoardType.INFORMATION_QUESTION,
                PostAnonymityType.PROFILE_TAG_VISIBLE,
                "삭제할 게시글",
                "삭제할 게시글 내용",
                true
        ));
        Comment rootComment = commentRepository.saveAndFlush(Comment.create(post, 20L, "댓글"));
        Comment reply = commentRepository.saveAndFlush(Comment.createReply(rootComment, 30L, "답글"));
        commentLikeRepository.saveAndFlush(CommentLike.create(reply, 40L));
        postLikeRepository.saveAndFlush(PostLike.create(post, 20L));
        postScrapRepository.saveAndFlush(PostScrap.create(post, 20L));
        postReportRepository.saveAndFlush(PostReport.create(post, 30L, "신고 사유"));

        postService.deletePost(10L, post.getId());

        Post deletedPost = postRepository.findById(post.getId()).orElseThrow();
        assertThat(deletedPost.getStatus()).isEqualTo(PostStatus.DELETED);
        assertThat(deletedPost.getDeletedAt()).isNotNull();
        assertThat(commentRepository.count()).isEqualTo(2);
        assertThat(commentLikeRepository.count()).isOne();
        assertThat(postLikeRepository.count()).isOne();
        assertThat(postScrapRepository.count()).isOne();
        assertThat(postReportRepository.count()).isOne();
        assertThat(postDisabilityTagRepository.count()).isZero();
        assertThat(postHashtagRepository.count()).isZero();
        assertThat(postImageRepository.count()).isZero();
    }
}
