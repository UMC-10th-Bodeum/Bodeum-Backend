package com.bodeum.domain.mypage.service;

import com.bodeum.domain.community.entity.Comment;
import com.bodeum.domain.community.entity.Post;
import com.bodeum.domain.community.entity.PostScrap;
import com.bodeum.domain.community.enums.CommentStatus;
import com.bodeum.domain.community.enums.PostStatus;
import com.bodeum.domain.community.service.CommentService;
import com.bodeum.domain.community.service.PostService;
import com.bodeum.domain.info.entity.InfoScrap;
import com.bodeum.domain.mypage.dto.response.MyCommentListResponse;
import com.bodeum.domain.mypage.dto.response.MyPageDashboardResponse;
import com.bodeum.domain.mypage.dto.response.MyPageDashboardResponse.ActivitySummary;
import com.bodeum.domain.mypage.dto.response.MyPostListResponse;
import com.bodeum.domain.mypage.dto.response.MyScrapListResponse;
import com.bodeum.domain.mypage.entity.enums.ScrapType;
import com.bodeum.domain.mypage.repository.MyPageCommentRepository;
import com.bodeum.domain.mypage.repository.MyPageInfoScrapRepository;
import com.bodeum.domain.mypage.repository.MyPageNewsScrapRepository;
import com.bodeum.domain.mypage.repository.MyPagePostRepository;
import com.bodeum.domain.mypage.repository.MyPagePostScrapRepository;
import com.bodeum.domain.news.entity.NewsScrap;
import com.bodeum.domain.user.dto.response.UserProfileResponse;
import com.bodeum.domain.user.service.UserService;
import com.bodeum.global.apiPayload.code.GeneralErrorCode;
import com.bodeum.global.apiPayload.exception.ProjectException;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class MyPageService {

    private final UserService userService;
    private final MyPageInfoScrapRepository infoScrapRepository;
    private final MyPageNewsScrapRepository newsScrapRepository;
    private final MyPagePostScrapRepository postScrapRepository;
    private final MyPagePostRepository postRepository;
    private final MyPageCommentRepository commentRepository;
    private final PostService postService;
    private final CommentService commentService;

    @Transactional(readOnly = true)
    public MyPageDashboardResponse getDashboard(Long userId) {
        UserProfileResponse profile =
                userService.getProfile(userId);

        long savedInfoCount =
                infoScrapRepository.countByUserId(userId)
                        + newsScrapRepository.countVisibleByUserId(userId)
                        + postScrapRepository.countVisibleByUserId(userId, PostStatus.ACTIVE);

        long myPostCount =
                postRepository.countVisibleByUserId(
                        userId,
                        PostStatus.ACTIVE
                );

        long myCommentCount =
                commentRepository.countVisibleByUserId(
                        userId,
                        CommentStatus.ACTIVE,
                        PostStatus.ACTIVE
                );

        ActivitySummary activitySummary =
                new ActivitySummary(
                        savedInfoCount,
                        myPostCount,
                        myCommentCount
                );

        return MyPageDashboardResponse.of(
                profile,
                activitySummary
        );
    }

    @Transactional(readOnly = true)
    public MyScrapListResponse getScraps(Long userId) {
        userService.getCurrentUser(userId);

        List<InfoScrap> infoScraps =
                infoScrapRepository
                        .findAllByUserIdOrderByCreatedAtDesc(userId);

        List<NewsScrap> newsScraps =
                newsScrapRepository
                        .findAllVisibleByUserIdOrderByCreatedAtDesc(userId);

        List<PostScrap> postScraps =
                postScrapRepository
                        .findAllVisibleByUserIdOrderByCreatedAtDesc(
                                userId,
                                PostStatus.ACTIVE
                        );

        return MyScrapListResponse.of(
                infoScraps,
                newsScraps,
                postScraps
        );
    }

    @Transactional
    public void deleteScrap(
            Long userId,
            Long scrapId,
            ScrapType scrapType
    ) {
        userService.getCurrentUser(userId);

        switch (scrapType) {
            case INFO -> deleteInfoScrap(userId, scrapId);
            case NEWS -> deleteNewsScrap(userId, scrapId);
        }
    }

    @Transactional(readOnly = true)
    public MyPostListResponse getPosts(
            Long userId,
            int page,
            int size
    ) {
        userService.getCurrentUser(userId);

        Page<Post> posts =
                postRepository
                        .findAllVisibleByUserIdOrderByCreatedAtDesc(
                                userId,
                                PostStatus.ACTIVE,
                                PageRequest.of(page, size)
                        );

        return MyPostListResponse.from(posts);
    }

    @Transactional
    public void deletePost(Long userId, Long postId) {
        postService.deletePost(userId, postId);
    }

    @Transactional(readOnly = true)
    public MyCommentListResponse getComments(
            Long userId,
            int page,
            int size
    ) {
        userService.getCurrentUser(userId);

        Page<Comment> comments =
                commentRepository
                        .findAllVisibleByUserIdOrderByCreatedAtDesc(
                                userId,
                                CommentStatus.ACTIVE,
                                PostStatus.ACTIVE,
                                PageRequest.of(page, size)
                        );

        return MyCommentListResponse.from(comments);
    }

    @Transactional
    public void deleteComment(Long userId, Long commentId) {
        commentService.deleteComment(userId, commentId);
    }

    private void deleteInfoScrap(Long userId, Long scrapId) {
        InfoScrap scrap = infoScrapRepository
                .findOwnedById(scrapId, userId)
                .orElseThrow(MyPageService::scrapNotFound);

        scrap.getInfoItem().updateScrapCount(-1);
        infoScrapRepository.delete(scrap);
    }

    private void deleteNewsScrap(Long userId, Long scrapId) {
        NewsScrap scrap = newsScrapRepository
                .findOwnedById(scrapId, userId)
                .orElseThrow(MyPageService::scrapNotFound);

        scrap.getNews().decreaseScrapCount();
        newsScrapRepository.delete(scrap);
    }

    private static ProjectException scrapNotFound() {
        return new ProjectException(GeneralErrorCode.NOT_FOUND);
    }
}
