package com.bodeum.domain.point.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.bodeum.domain.auth.enums.SocialProvider;
import com.bodeum.domain.community.dto.request.CreateCommentRequest;
import com.bodeum.domain.community.dto.request.CreatePostRequest;
import com.bodeum.domain.community.dto.response.CommentResponse;
import com.bodeum.domain.community.dto.response.PostResponse;
import com.bodeum.domain.community.enums.PostAnonymityType;
import com.bodeum.domain.community.enums.PostBoardType;
import com.bodeum.domain.community.service.CommentService;
import com.bodeum.domain.community.service.PostService;
import com.bodeum.domain.point.dto.response.MyPointResponse;
import com.bodeum.domain.point.enums.PointType;
import com.bodeum.domain.user.entity.User;
import com.bodeum.domain.user.repository.UserRepository;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest(properties = "bodeum.auth.jwt-secret=test-jwt-secret-32-bytes-minimum-value")
@Transactional
class CommunityPointRewardIntegrationTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PostService postService;

    @Autowired
    private CommentService commentService;

    @Autowired
    private PointService pointService;

    @Test
    void appliesCommunityPointPolicyAcrossPostAnswerLikeAndAcceptanceFlows() {
        User author = saveUser("point-author", "작성자");
        User answerer = saveUser("point-answerer", "답변자");
        User helper = saveUser("point-helper", "공감 사용자");

        List<PostResponse> posts = java.util.stream.IntStream.rangeClosed(1, 4)
                .mapToObj(index -> postService.createPost(
                        author.getId(),
                        new CreatePostRequest(
                                PostBoardType.INFORMATION_QUESTION,
                                PostAnonymityType.PROFILE_TAG_VISIBLE,
                                "포인트 질문 " + index,
                                "게시글 작성 포인트 일일 제한을 검증합니다.",
                                List.of()
                        )
                ))
                .toList();

        List<CommentResponse> answers = java.util.stream.IntStream.rangeClosed(1, 4)
                .mapToObj(index -> commentService.createComment(
                        answerer.getId(),
                        posts.getFirst().postId(),
                        new CreateCommentRequest("제한 없는 답변 " + index)
                ))
                .toList();

        postService.likePost(helper.getId(), posts.getFirst().postId());
        commentService.likeComment(helper.getId(), answers.getFirst().commentId());
        commentService.toggleCommentAdoption(author.getId(), answers.getFirst().commentId());

        MyPointResponse authorPoints = pointService.getMyPoints(author.getId());
        assertThat(authorPoints.totalPoint()).isEqualTo(20);
        assertActivity(authorPoints, PointType.POST_CREATED, 15L, 3L);
        assertActivity(authorPoints, PointType.LIKE_RECEIVED, 5L, 1L);

        MyPointResponse answererPoints = pointService.getMyPoints(answerer.getId());
        assertThat(answererPoints.totalPoint()).isEqualTo(41);
        assertActivity(answererPoints, PointType.ANSWER_CREATED, 16L, 4L);
        assertActivity(answererPoints, PointType.LIKE_RECEIVED, 5L, 1L);
        assertActivity(answererPoints, PointType.ANSWER_ACCEPTED, 20L, 1L);

        assertThat(pointService.getMyPoints(helper.getId()).totalPoint()).isZero();
    }

    private User saveUser(String providerUserId, String nickname) {
        return userRepository.saveAndFlush(User.createSocialUser(
                SocialProvider.KAKAO,
                providerUserId,
                providerUserId + "@example.com",
                nickname
        ));
    }

    private void assertActivity(
            MyPointResponse response,
            PointType pointType,
            long earnedPoint,
            long activityCount
    ) {
        assertThat(response.activities())
                .filteredOn(activity -> activity.pointType() == pointType)
                .singleElement()
                .satisfies(activity -> {
                    assertThat(activity.earnedPoint()).isEqualTo(earnedPoint);
                    assertThat(activity.activityCount()).isEqualTo(activityCount);
                });
    }
}
