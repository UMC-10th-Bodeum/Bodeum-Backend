package com.bodeum.domain.community.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.bodeum.domain.community.entity.Post;
import com.bodeum.domain.community.enums.PostAnonymityType;
import com.bodeum.domain.community.enums.PostBoardType;
import com.bodeum.domain.community.enums.PostStatus;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest(properties = "bodeum.auth.jwt-secret=test-jwt-secret-32-bytes-minimum-value")
@Transactional
class PostRepositoryIntegrationTest {

    @Autowired
    private PostRepository postRepository;

    @Test
    void findActivePostsSearchesTitleAndContentAndExcludesUnavailablePosts() {
        Post titleMatch = post("언어치료 후기", "도움이 되었어요", 5);
        Post contentMatch = post("치료 정보", "언어치료 기관을 공유합니다", 3);
        Post noMatch = post("일상 이야기", "오늘의 기록", 10);
        Post hiddenMatch = post("언어치료 질문", "궁금합니다", 20);
        hiddenMatch.hide();
        Post deletedMatch = post("언어치료 기록", "삭제된 글", 30);
        deletedMatch.delete();

        postRepository.saveAll(List.of(titleMatch, contentMatch, noMatch, hiddenMatch, deletedMatch));
        postRepository.flush();

        var result = postRepository.findActivePosts(
                PostStatus.ACTIVE,
                "언어치료",
                PageRequest.of(
                        0,
                        10,
                        Sort.by(
                                Sort.Order.desc("viewCount"),
                                Sort.Order.desc("createdAt"),
                                Sort.Order.desc("id")
                        )
                )
        );

        assertThat(result.getContent()).extracting(Post::getTitle)
                .containsExactly("언어치료 후기", "치료 정보");
        assertThat(result.getTotalElements()).isEqualTo(2);
    }

    @Test
    void findActivePostsReturnsWholeListWhenKeywordIsNull() {
        Post first = post("첫 번째 글", "첫 번째 내용", 1);
        Post second = post("두 번째 글", "두 번째 내용", 2);
        postRepository.saveAllAndFlush(List.of(first, second));

        var result = postRepository.findActivePosts(
                PostStatus.ACTIVE,
                null,
                PageRequest.of(0, 10, Sort.by(Sort.Order.desc("viewCount")))
        );

        assertThat(result.getContent()).extracting(Post::getTitle)
                .containsExactly("두 번째 글", "첫 번째 글");
    }

    @Test
    void findActivePostsTreatsEscapedLikeWildcardsAsLiteralCharacters() {
        Post percentMatch = post("달성률 100% 기록", "퍼센트 검색 대상", 2);
        Post underscoreMatch = post("센터_A 후기", "언더스코어 검색 대상", 1);
        Post noMatch = post("달성률 100점 기록", "일반 검색 대상", 3);
        postRepository.saveAllAndFlush(List.of(percentMatch, underscoreMatch, noMatch));

        var percentResult = postRepository.findActivePosts(
                PostStatus.ACTIVE,
                "100!%",
                PageRequest.of(0, 10)
        );
        var underscoreResult = postRepository.findActivePosts(
                PostStatus.ACTIVE,
                "센터!_A",
                PageRequest.of(0, 10)
        );

        assertThat(percentResult.getContent()).extracting(Post::getTitle)
                .containsExactly("달성률 100% 기록");
        assertThat(underscoreResult.getContent()).extracting(Post::getTitle)
                .containsExactly("센터_A 후기");
    }

    private Post post(String title, String content, int viewCount) {
        Post post = Post.create(
                10L,
                PostBoardType.FREE_COMMUNICATION,
                PostAnonymityType.PROFILE_TAG_VISIBLE,
                title,
                content
        );
        ReflectionTestUtils.setField(post, "viewCount", viewCount);
        return post;
    }
}
