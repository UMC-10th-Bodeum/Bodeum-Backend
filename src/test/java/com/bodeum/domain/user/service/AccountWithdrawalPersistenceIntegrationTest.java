package com.bodeum.domain.user.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.bodeum.domain.auth.enums.SocialProvider;
import com.bodeum.domain.community.entity.Comment;
import com.bodeum.domain.community.entity.CommentLike;
import com.bodeum.domain.community.entity.Post;
import com.bodeum.domain.community.entity.PostLike;
import com.bodeum.domain.community.entity.PostScrap;
import com.bodeum.domain.community.enums.PostAnonymityType;
import com.bodeum.domain.community.enums.PostBoardType;
import com.bodeum.domain.community.repository.CommentLikeRepository;
import com.bodeum.domain.community.repository.PostLikeRepository;
import com.bodeum.domain.community.repository.PostScrapRepository;
import com.bodeum.domain.info.entity.InfoCategory;
import com.bodeum.domain.info.entity.InfoItem;
import com.bodeum.domain.info.entity.InfoScrap;
import com.bodeum.domain.info.entity.enums.MainCategory;
import com.bodeum.domain.info.repository.InfoScrapRepository;
import com.bodeum.domain.news.collector.NewsCandidate;
import com.bodeum.domain.news.entity.News;
import com.bodeum.domain.news.entity.NewsCategory;
import com.bodeum.domain.news.entity.NewsCategoryCode;
import com.bodeum.domain.news.entity.NewsScrap;
import com.bodeum.domain.news.entity.NewsType;
import com.bodeum.domain.news.repository.NewsScrapRepository;
import com.bodeum.domain.onboarding.enums.CommunityRoleType;
import com.bodeum.domain.onboarding.enums.GuardianType;
import com.bodeum.domain.point.entity.GuardianPoint;
import com.bodeum.domain.point.entity.GuardianPointHistory;
import com.bodeum.domain.point.enums.PointType;
import com.bodeum.domain.point.service.PointService;
import com.bodeum.domain.search.entity.SearchLog;
import com.bodeum.domain.search.enums.SearchType;
import com.bodeum.domain.search.repository.SearchLogRepository;
import com.bodeum.domain.user.entity.User;
import com.bodeum.domain.user.repository.UserRepository;
import com.bodeum.global.config.JpaAuditingConfig;
import com.bodeum.global.config.QueryDslConfig;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * 회원 탈퇴 후속 처리의 실제 DB 동작(벌크 JPQL·카운트 감소·orphanRemoval)을 H2로 검증한다.
 * 단위 테스트는 리포지토리를 mock 처리하므로 실제 쿼리 문법과 카운트 정확도를 검증하지 못한다.
 * 시크릿이 필요한 전체 컨텍스트를 피하기 위해 @DataJpaTest 슬라이스로 실행한다.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({QueryDslConfig.class, JpaAuditingConfig.class, PointService.class})
class AccountWithdrawalPersistenceIntegrationTest {

    @Autowired
    private TestEntityManager em;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private PointService pointService;
    @Autowired
    private SearchLogRepository searchLogRepository;
    @Autowired
    private PostScrapRepository postScrapRepository;
    @Autowired
    private PostLikeRepository postLikeRepository;
    @Autowired
    private CommentLikeRepository commentLikeRepository;
    @Autowired
    private InfoScrapRepository infoScrapRepository;
    @Autowired
    private NewsScrapRepository newsScrapRepository;

    @Test
    @DisplayName("게시글 스크랩 삭제: 탈퇴자 스크랩만 삭제되고 scrapCount는 1만 감소한다")
    void postScrapWithdrawal() {
        Long withdrawer = persistUser("kakao-1").getId();
        Long other = persistUser("kakao-2").getId();
        Post post = persistPost(withdrawer);
        em.persist(PostScrap.create(post, withdrawer));
        em.persist(PostScrap.create(post, other));
        post.increaseScrapCount();
        post.increaseScrapCount();
        Long postId = post.getId();
        em.flush();
        em.clear();

        postScrapRepository.decreaseScrapCountForUserScraps(withdrawer);
        postScrapRepository.deleteByUserId(withdrawer);
        em.clear();

        Post reloaded = em.find(Post.class, postId);
        assertThat(reloaded).isNotNull();                                   // 본문 보존
        assertThat(reloaded.getScrapCount()).isEqualTo(1);                  // 탈퇴자 몫만 감소
        assertThat(postScrapRepository.findByPost_IdAndUserId(postId, withdrawer)).isEmpty();
        assertThat(postScrapRepository.findByPost_IdAndUserId(postId, other)).isPresent();
    }

    @Test
    @DisplayName("게시글 좋아요 삭제: 탈퇴자 좋아요만 삭제되고 likeCount는 1만 감소한다")
    void postLikeWithdrawal() {
        Long withdrawer = persistUser("kakao-1").getId();
        Long other = persistUser("kakao-2").getId();
        Post post = persistPost(withdrawer);
        em.persist(PostLike.create(post, withdrawer));
        em.persist(PostLike.create(post, other));
        post.increaseLikeCount();
        post.increaseLikeCount();
        Long postId = post.getId();
        em.flush();
        em.clear();

        postLikeRepository.decreaseLikeCountForUserLikes(withdrawer);
        postLikeRepository.deleteByUserId(withdrawer);
        em.clear();

        Post reloaded = em.find(Post.class, postId);
        assertThat(reloaded.getLikeCount()).isEqualTo(1);
        assertThat(postLikeRepository.findByPost_IdAndUserId(postId, withdrawer)).isEmpty();
        assertThat(postLikeRepository.findByPost_IdAndUserId(postId, other)).isPresent();
    }

    @Test
    @DisplayName("댓글·답글 공감 삭제: 탈퇴자 공감만 삭제되고 likeCount는 1만 감소한다")
    void commentLikeWithdrawal() {
        Long withdrawer = persistUser("kakao-1").getId();
        Long other = persistUser("kakao-2").getId();
        Post post = persistPost(withdrawer);
        Comment comment = Comment.create(post, withdrawer, "댓글");
        em.persist(comment);
        em.persist(CommentLike.create(comment, withdrawer));
        em.persist(CommentLike.create(comment, other));
        comment.increaseLikeCount();
        comment.increaseLikeCount();
        Long commentId = comment.getId();
        em.flush();
        em.clear();

        commentLikeRepository.decreaseLikeCountForUserLikes(withdrawer);
        commentLikeRepository.deleteByUserId(withdrawer);
        em.clear();

        Comment reloaded = em.find(Comment.class, commentId);
        assertThat(reloaded).isNotNull();                                   // 본문 보존
        assertThat(reloaded.getLikeCount()).isEqualTo(1);
        assertThat(commentLikeRepository.findByComment_IdAndUserId(commentId, withdrawer)).isEmpty();
        assertThat(commentLikeRepository.findByComment_IdAndUserId(commentId, other)).isPresent();
    }

    @Test
    @DisplayName("정보 스크랩 삭제: InfoItem.scrapCount가 1만 감소한다")
    void infoScrapWithdrawal() {
        User withdrawerUser = persistUser("kakao-1");
        User other = persistUser("kakao-2");
        Long withdrawer = withdrawerUser.getId();
        Long otherId = other.getId();
        InfoItem item = persistInfoItem();
        em.persist(InfoScrap.builder().user(withdrawerUser).infoItem(item).build());
        em.persist(InfoScrap.builder().user(other).infoItem(item).build());
        item.updateScrapCount(2);
        Long itemId = item.getId();
        em.flush();
        em.clear();

        infoScrapRepository.decreaseScrapCountForUserScraps(withdrawer);
        infoScrapRepository.deleteByUserId(withdrawer);
        em.clear();

        InfoItem reloaded = em.find(InfoItem.class, itemId);
        assertThat(reloaded.getScrapCount()).isEqualTo(1);
        assertThat(countInfoScrapsByUser(withdrawer)).isZero();             // 탈퇴자 스크랩 삭제
        assertThat(countInfoScrapsByUser(otherId)).isEqualTo(1);            // 타 사용자 스크랩 보존
    }

    @Test
    @DisplayName("소식 스크랩 삭제: News.scrapCount가 1만 감소한다")
    void newsScrapWithdrawal() {
        Long withdrawer = persistUser("kakao-1").getId();
        Long other = persistUser("kakao-2").getId();
        News news = persistNews();
        em.persist(NewsScrap.create(news, withdrawer));
        em.persist(NewsScrap.create(news, other));
        news.increaseScrapCount();
        news.increaseScrapCount();
        Long newsId = news.getId();
        em.flush();
        em.clear();

        newsScrapRepository.decreaseScrapCountForUserScraps(withdrawer);
        newsScrapRepository.deleteByUserId(withdrawer);
        em.clear();

        News reloaded = em.find(News.class, newsId);
        assertThat(reloaded.getScrapCount()).isEqualTo(1L);
        assertThat(countNewsScrapsByUser(withdrawer)).isZero();             // 탈퇴자 스크랩 삭제
        assertThat(countNewsScrapsByUser(other)).isEqualTo(1);             // 타 사용자 스크랩 보존
    }

    @Test
    @DisplayName("검색 기록 삭제: 탈퇴자 로그만 전부 삭제된다")
    void searchLogWithdrawal() {
        Long withdrawer = persistUser("kakao-1").getId();
        Long other = persistUser("kakao-2").getId();
        em.persist(SearchLog.create(withdrawer, "키워드1", SearchType.INFO, 3L));
        em.persist(SearchLog.create(withdrawer, "키워드2", SearchType.INFO, 1L));
        em.persist(SearchLog.create(other, "키워드3", SearchType.INFO, 2L));
        em.flush();
        em.clear();

        int deleted = searchLogRepository.deleteByUserId(withdrawer);

        assertThat(deleted).isEqualTo(2);
        assertThat(searchLogRepository.count()).isEqualTo(1);               // other의 로그만 남음
    }

    @Test
    @DisplayName("작성자 익명화 판정: DELETED 회원 id만 반환한다")
    void findWithdrawnUserIds() {
        User withdrawn = persistUser("kakao-1");
        User active = persistUser("kakao-2");
        withdrawn.withdraw();
        em.flush();
        em.clear();

        List<Long> result = userRepository.findWithdrawnUserIdsByIdIn(
                List.of(withdrawn.getId(), active.getId()));

        assertThat(result).containsExactly(withdrawn.getId());
    }

    @Test
    @DisplayName("보호자 프로필: 탈퇴 시 guardianProfile 행이 orphanRemoval로 삭제된다")
    void withdrawalRemovesGuardianProfile() {
        User user = persistUser("kakao-1");
        user.updateGuardianProfile("보호자", GuardianType.PARENT, CommunityRoleType.INFO_SEEKER);
        Long userId = user.getId();
        em.flush();
        em.clear();
        assertThat(countGuardianProfiles()).isEqualTo(1L);

        User managed = em.find(User.class, userId);
        managed.withdraw();
        em.flush();
        em.clear();

        assertThat(countGuardianProfiles()).isZero();
        assertThat(em.find(User.class, userId)).isNotNull();                // User 행은 묘비로 유지
    }

    @Test
    @DisplayName("포인트: 탈퇴 시 guardian_point와 적립 내역이 삭제된다")
    void withdrawalDeletesGuardianPointAndHistory() {
        // 포인트는 GuardianPoint로 분리돼 guardianProfile orphanRemoval로 지워지지 않으므로
        // PointService가 명시적으로 삭제한다(#176).
        User user = persistUser("kakao-1");
        user.updateGuardianProfile("보호자", GuardianType.PARENT, CommunityRoleType.INFO_SEEKER);
        Long userId = user.getId();
        em.flush();

        Long guardianProfileId = (Long) ReflectionTestUtils.getField(
                ReflectionTestUtils.getField(user, "guardianProfile"), "id");
        persistGuardianPointWithHistory(guardianProfileId);
        em.flush();
        em.clear();
        assertThat(countGuardianPoints()).isEqualTo(1L);
        assertThat(countGuardianPointHistories()).isEqualTo(1L);

        pointService.deleteUserPoints(userId);
        em.flush();
        em.clear();

        assertThat(countGuardianPointHistories()).isZero();
        assertThat(countGuardianPoints()).isZero();
    }

    // --- seeding helpers ---

    private User persistUser(String providerUserId) {
        User user = User.createSocialUser(
                SocialProvider.KAKAO, providerUserId, providerUserId + "@example.com", "닉네임");
        em.persist(user);
        em.flush();
        return user;
    }

    private Post persistPost(Long userId) {
        Post post = Post.create(userId, PostBoardType.FREE_COMMUNICATION,
                PostAnonymityType.PROFILE_TAG_VISIBLE, "제목", "내용");
        em.persist(post);
        return post;
    }

    private InfoItem persistInfoItem() {
        InfoCategory category = InfoCategory.builder()
                .mainCategory(MainCategory.WELFARE)
                .mainCategoryKo("복지")
                .subCategory(com.bodeum.domain.info.entity.enums.InfoSubCategory.PRIVATE_WELFARE)
                .subCategoryKo("서브")
                .build();
        em.persist(category);
        InfoItem item = InfoItem.builder()
                .externalId("ext-1")
                .infoCategory(category)
                .name("정보")
                .address("서울시 강남구")
                .sido("서울")
                .sigungu("강남")
                .syncedAt(LocalDateTime.now())
                .build();
        em.persist(item);
        return item;
    }

    private News persistNews() {
        NewsCategory category = NewsCategory.create(NewsCategoryCode.LOCAL_NEWS);
        em.persist(category);
        NewsCandidate candidate = new NewsCandidate(
                null, "뉴스 제목", null, null, null, null, null, null, null, null, null,
                LocalDateTime.now(), null, null, null, null,
                NewsCategoryCode.LOCAL_NEWS, NewsType.LOCAL, null);
        News news = News.create(category, null, null, candidate);
        em.persist(news);
        return news;
    }

    private long countGuardianProfiles() {
        return em.getEntityManager()
                .createQuery("SELECT COUNT(g) FROM GuardianProfile g", Long.class)
                .getSingleResult();
    }

    // GuardianPoint·GuardianPointHistory는 공개 생성자·정적 팩토리가 없어 리플렉션으로 시드한다.
    private void persistGuardianPointWithHistory(Long guardianProfileId) {
        GuardianPoint guardianPoint = BeanUtils.instantiateClass(GuardianPoint.class);
        ReflectionTestUtils.setField(guardianPoint, "guardianProfileId", guardianProfileId);
        ReflectionTestUtils.setField(guardianPoint, "totalPoint", 100);
        em.persist(guardianPoint);

        GuardianPointHistory history = BeanUtils.instantiateClass(GuardianPointHistory.class);
        ReflectionTestUtils.setField(history, "guardianPoint", guardianPoint);
        ReflectionTestUtils.setField(history, "pointType", PointType.POST_CREATED);
        ReflectionTestUtils.setField(history, "pointValue", 100);
        em.persist(history);
    }

    private long countGuardianPoints() {
        return em.getEntityManager()
                .createQuery("SELECT COUNT(p) FROM GuardianPoint p", Long.class)
                .getSingleResult();
    }

    private long countGuardianPointHistories() {
        return em.getEntityManager()
                .createQuery("SELECT COUNT(h) FROM GuardianPointHistory h", Long.class)
                .getSingleResult();
    }

    private long countInfoScrapsByUser(Long userId) {
        return em.getEntityManager()
                .createQuery("SELECT COUNT(s) FROM InfoScrap s WHERE s.user.id = :userId", Long.class)
                .setParameter("userId", userId)
                .getSingleResult();
    }

    private long countNewsScrapsByUser(Long userId) {
        return em.getEntityManager()
                .createQuery("SELECT COUNT(s) FROM NewsScrap s WHERE s.userId = :userId", Long.class)
                .setParameter("userId", userId)
                .getSingleResult();
    }
}
