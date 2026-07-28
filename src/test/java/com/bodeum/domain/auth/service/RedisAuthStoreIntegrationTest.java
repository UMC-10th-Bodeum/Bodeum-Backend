package com.bodeum.domain.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assumptions.assumeThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bodeum.domain.auth.enums.SocialProvider;
import com.bodeum.domain.auth.repository.AuthLoginCodeRepository;
import com.bodeum.domain.auth.repository.OAuthStateRepository;
import com.bodeum.domain.auth.repository.RefreshTokenSessionRepository;
import com.bodeum.domain.user.entity.User;
import com.bodeum.domain.user.service.UserService;
import com.bodeum.global.apiPayload.exception.ProjectException;
import com.bodeum.global.auth.AuthUserPrincipal;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;

/**
 * 실제 로컬 Redis(localhost:6379)에 붙여 인증 저장소의 Redis 경로를 검증한다.
 * Redis가 없으면 assumeThat으로 스킵한다(CI 안전).
 *
 * <p>@Transactional은 스프링 프록시가 있어야 동작하는데 여기서는 컴포넌트를 직접 생성하므로 no-op이다.
 * Redis 분기는 트랜잭션이 필요 없으므로 실제 프로덕션 코드가 그대로 실행된다.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class RedisAuthStoreIntegrationTest {

    private static final String REDIS_HOST = "localhost";
    private static final int REDIS_PORT = 6379;

    private LettuceConnectionFactory connectionFactory;
    private StringRedisTemplate redisTemplate;
    private AuthTokenProperties properties;

    private RefreshTokenStore refreshTokenStore;
    private AuthPrincipalCache principalCache;
    private AccessTokenDenylist denylist;

    @BeforeAll
    void connect() {
        connectionFactory = new LettuceConnectionFactory(REDIS_HOST, REDIS_PORT);
        connectionFactory.afterPropertiesSet();
        redisTemplate = new StringRedisTemplate(connectionFactory);
        redisTemplate.afterPropertiesSet();

        assumeThat(pingSucceeds())
                .as("localhost:6379 Redis가 떠 있어야 이 통합 테스트가 돈다")
                .isTrue();
    }

    @AfterAll
    void disconnect() {
        if (connectionFactory != null) {
            connectionFactory.destroy();
        }
    }

    @BeforeEach
    void setUp() {
        flushTestKeys();

        properties = new AuthTokenProperties();
        properties.setRedisEnabled(true);
        properties.setJwtSecret("test-jwt-secret-32-bytes-minimum-value");
        properties.setLoginCodeTtl(Duration.ofSeconds(60));

        refreshTokenStore = new RefreshTokenStore(
                properties, mock(RefreshTokenSessionRepository.class), redisTemplate);
        principalCache = new AuthPrincipalCache(properties, redisTemplate);
        denylist = new AccessTokenDenylist(properties, redisTemplate);
    }

    // ---------- OAuthStateStore ----------

    @Test
    void oauthState_issueThenConsume_roundTripsThroughRedis() {
        OAuthStateStore store = new OAuthStateStore(
                mock(OAuthStateRepository.class), properties, redisTemplate);

        String state = store.issue(SocialProvider.KAKAO);

        assertThat(redisTemplate.hasKey("bodeum:auth:oauth-state:" + state)).isTrue();
        assertThat(redisTemplate.getExpire("bodeum:auth:oauth-state:" + state))
                .isBetween(1L, 600L);

        assertThat(store.consume(SocialProvider.KAKAO, state)).isTrue();
        // 소비 후 키가 삭제되어 재사용 불가(getAndDelete)
        assertThat(store.consume(SocialProvider.KAKAO, state)).isFalse();
        assertThat(redisTemplate.hasKey("bodeum:auth:oauth-state:" + state)).isFalse();
    }

    @Test
    void oauthState_consumeWithWrongProvider_fails() {
        OAuthStateStore store = new OAuthStateStore(
                mock(OAuthStateRepository.class), properties, redisTemplate);

        String state = store.issue(SocialProvider.KAKAO);

        assertThat(store.consume(SocialProvider.NAVER, state)).isFalse();
    }

    // ---------- AuthLoginCodeStore ----------

    @Test
    void loginCode_issueThenConsume_roundTripsThroughRedis() {
        AuthLoginCodeStore store = new AuthLoginCodeStore(
                mock(AuthLoginCodeRepository.class), properties, redisTemplate);

        String code = store.issue(42L, true);

        assertThat(redisTemplate.getExpire("bodeum:auth:login-code:" + code))
                .isBetween(1L, 60L);

        AuthLoginCodeStore.Consumed consumed = store.consume(code);
        assertThat(consumed.userId()).isEqualTo(42L);
        assertThat(consumed.isNewUser()).isTrue();

        // 1회용: 두 번째 교환은 실패
        assertThatThrownBy(() -> store.consume(code))
                .isInstanceOf(ProjectException.class);
    }

    // ---------- AuthTokenService (refresh 세션) ----------

    @Test
    void refreshToken_isStoredInRedisAndRotatedOnRefresh() {
        AuthTokenService service = buildAuthTokenService(userServiceReturning(7L));

        AuthTokenService.AuthTokenPair pair = service.issueTokens(7L);

        // refresh 키 + 역인덱스 Set이 함께 저장됨
        assertThat(refreshKeys()).hasSize(1);
        assertThat(redisTemplate.opsForSet().size("bodeum:auth:user-sessions:7")).isEqualTo(1);

        AuthTokenService.AuthTokenPair rotated = service.refresh(pair.refreshToken());
        assertThat(rotated.refreshToken()).isNotEqualTo(pair.refreshToken());
        // 회전 후에도 유효 키는 1개(옛 키 삭제 + 새 키 생성), 역인덱스도 1개 유지
        assertThat(refreshKeys()).hasSize(1);
        assertThat(redisTemplate.opsForSet().size("bodeum:auth:user-sessions:7")).isEqualTo(1);

        // 옛 refresh token 재사용 불가
        assertThatThrownBy(() -> service.refresh(pair.refreshToken()))
                .isInstanceOf(ProjectException.class);
    }

    @Test
    void refresh_withUnknownToken_throws() {
        AuthTokenService service = buildAuthTokenService(mock(UserService.class));

        assertThatThrownBy(() -> service.refresh("never-issued-token"))
                .isInstanceOf(ProjectException.class);
    }

    @Test
    void revoke_removesRefreshKeyFromRedis() {
        AuthTokenService service = buildAuthTokenService(userServiceReturning(9L));

        AuthTokenService.AuthTokenPair pair = service.issueTokens(9L);
        assertThat(refreshKeys()).hasSize(1);

        service.revoke(pair.refreshToken());

        assertThat(refreshKeys()).isEmpty();
        assertThatThrownBy(() -> service.refresh(pair.refreshToken()))
                .isInstanceOf(ProjectException.class);
    }

    // ---------- 세션 역인덱스 (전체 폐기) ----------

    @Test
    void revokeAll_clearsEverySessionForUser() {
        AuthTokenService service = buildAuthTokenService(userServiceReturning(11L));

        service.issueTokens(11L);
        service.issueTokens(11L);
        assertThat(refreshKeys()).hasSize(2);
        assertThat(redisTemplate.opsForSet().size("bodeum:auth:user-sessions:11")).isEqualTo(2);

        refreshTokenStore.revokeAll(11L);

        assertThat(refreshKeys()).isEmpty();
        assertThat(redisTemplate.hasKey("bodeum:auth:user-sessions:11")).isFalse();
    }

    // ---------- principal 캐시 ----------

    @Test
    void principalCache_putGetEvict_roundTrips() {
        AuthUserPrincipal principal = new AuthUserPrincipal(7L, SocialProvider.KAKAO, "민준맘", "a@b.com");

        principalCache.put("subject-7", principal);
        assertThat(redisTemplate.getExpire("bodeum:auth:principal:subject-7"))
                .isBetween(1L, 180L);
        assertThat(principalCache.get("subject-7")).contains(principal);

        principalCache.evict("subject-7");
        assertThat(principalCache.get("subject-7")).isEmpty();
    }

    @Test
    void authenticate_usesCacheThenDenylistBlocksRevokedToken() {
        User user = mock(User.class);
        when(user.getId()).thenReturn(7L);
        when(user.getAuthSubject()).thenReturn("subject-7");
        when(user.getProvider()).thenReturn(SocialProvider.KAKAO);
        when(user.getNickname()).thenReturn("민준맘");
        when(user.getEmail()).thenReturn("a@b.com");

        UserService userService = mock(UserService.class);
        when(userService.findActiveUser(7L)).thenReturn(Optional.of(user));
        when(userService.findActiveUserByAuthSubject("subject-7")).thenReturn(Optional.of(user));

        AuthTokenService service = buildAuthTokenService(userService);
        AuthTokenService.AuthTokenPair pair = service.issueTokens(7L);

        // 최초 인증 → DB 조회 후 캐시 적재
        assertThat(service.authenticate(pair.accessToken()))
                .get()
                .extracting(AuthUserPrincipal::userId)
                .isEqualTo(7L);
        assertThat(redisTemplate.hasKey("bodeum:auth:principal:subject-7")).isTrue();

        // 발급 이후 시각으로 폐기 → 캐시에 있어도 denylist가 먼저 차단
        denylist.revokeAllBefore("subject-7", Instant.now().plusSeconds(2));
        assertThat(service.authenticate(pair.accessToken())).isEmpty();
    }

    // ---------- helpers ----------

    private AuthTokenService buildAuthTokenService(UserService userService) {
        return new AuthTokenService(
                userService,
                new JwtTokenProvider(properties),
                properties,
                refreshTokenStore,
                principalCache,
                denylist);
    }

    private UserService userServiceReturning(Long userId) {
        User user = mock(User.class);
        when(user.getId()).thenReturn(userId);
        when(user.getAuthSubject()).thenReturn("subject-" + userId);

        UserService userService = mock(UserService.class);
        when(userService.findActiveUser(userId)).thenReturn(Optional.of(user));
        return userService;
    }

    private boolean pingSucceeds() {
        try {
            return "PONG".equalsIgnoreCase(connectionFactory.getConnection().ping());
        } catch (RuntimeException e) {
            return false;
        }
    }

    private Set<String> refreshKeys() {
        return redisTemplate.keys("bodeum:auth:refresh:*");
    }

    private void flushTestKeys() {
        Set<String> keys = redisTemplate.keys("bodeum:auth:*");
        if (keys != null && !keys.isEmpty()) {
            redisTemplate.delete(keys);
        }
    }
}
