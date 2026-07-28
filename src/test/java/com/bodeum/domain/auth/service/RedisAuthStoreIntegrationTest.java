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
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * 실제 Redis(Testcontainers) 컨테이너에 붙여 인증 저장소의 Redis 경로를 검증한다.
 * Docker가 없으면 assumeThat으로 스킵한다(로컬 개발 안전). CI는 Docker가 있어 실제로 실행된다.
 *
 * <p>@Transactional은 스프링 프록시가 있어야 동작하는데 여기서는 컴포넌트를 직접 생성하므로 no-op이다.
 * Redis 분기는 트랜잭션이 필요 없으므로 실제 프로덕션 코드가 그대로 실행된다.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class RedisAuthStoreIntegrationTest {

    private static final DockerImageName REDIS_IMAGE = DockerImageName.parse("redis:7-alpine");

    private GenericContainer<?> redis;
    private LettuceConnectionFactory connectionFactory;
    private StringRedisTemplate redisTemplate;
    private AuthTokenProperties properties;

    private RefreshTokenStore refreshTokenStore;
    private AuthPrincipalCache principalCache;
    private AccessTokenDenylist denylist;

    @BeforeAll
    void connect() {
        assumeThat(DockerClientFactory.instance().isDockerAvailable())
                .as("이 통합 테스트는 Docker(Testcontainers)가 필요하다")
                .isTrue();

        redis = new GenericContainer<>(REDIS_IMAGE).withExposedPorts(6379);
        redis.start();

        connectionFactory = new LettuceConnectionFactory(redis.getHost(), redis.getMappedPort(6379));
        connectionFactory.afterPropertiesSet();
        redisTemplate = new StringRedisTemplate(connectionFactory);
        redisTemplate.afterPropertiesSet();
    }

    @AfterAll
    void disconnect() {
        if (connectionFactory != null) {
            connectionFactory.destroy();
        }
        if (redis != null) {
            redis.stop();
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
        // 옛 토큰은 logout 경쟁 처리를 위한 consumed tombstone으로 남고, 활성 세션은 1개만 유지된다.
        assertThat(refreshKeys()).hasSize(2);
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

    @Test
    void refreshConsumedBeforeLogout_cannotResurrectSessionAfterLogout() {
        AuthTokenService service = buildAuthTokenService(userServiceReturning(13L));
        AuthTokenService.AuthTokenPair pair = service.issueTokens(13L);
        String oldTokenHash = hashToken(pair.refreshToken());

        // refresh가 기존 토큰을 소비한 직후 logout이 끼어든 경쟁 순서를 재현한다.
        RefreshTokenStore.ConsumedSession consumed = refreshTokenStore
                .consume(oldTokenHash, Instant.now())
                .orElseThrow();
        refreshTokenStore.revokeFamily(13L, oldTokenHash);

        // 이미 진행 중이던 refresh가 logout 뒤 같은 family로 저장을 시도해도 거부된다.
        String tokenSavedAfterLogout = "rotated-after-logout";
        boolean saved = refreshTokenStore.save(
                hashToken(tokenSavedAfterLogout),
                13L,
                consumed.familyId(),
                Instant.now().plus(properties.getRefreshTokenTtl()),
                properties.getRefreshTokenTtl()
        );

        assertThat(saved).isFalse();
        assertThatThrownBy(() -> service.refresh(tokenSavedAfterLogout))
                .isInstanceOf(ProjectException.class);
    }

    @Test
    void refreshConsumedBeforeWithdrawal_cannotResaveSessionAfterRevokeAll() {
        AuthTokenService service = buildAuthTokenService(userServiceReturning(17L));
        AuthTokenService.AuthTokenPair pair = service.issueTokens(17L);

        // 탈퇴와 경쟁하던 refresh가 토큰을 소비한 상태를 만든다.
        // 소비된 tombstone은 역인덱스에서 빠지므로 revokeAll의 Set 순회로는 찾을 수 없다.
        RefreshTokenStore.ConsumedSession consumed = refreshTokenStore
                .consume(hashToken(pair.refreshToken()), Instant.now())
                .orElseThrow();

        refreshTokenStore.revokeAll(17L);

        // 탈퇴 이후 같은 family로 회전 토큰을 저장하려는 시도는 거부돼야 한다.
        boolean saved = refreshTokenStore.save(
                hashToken("rotated-after-withdrawal"),
                17L,
                consumed.familyId(),
                Instant.now().plus(properties.getRefreshTokenTtl()),
                properties.getRefreshTokenTtl()
        );

        assertThat(saved).isFalse();
        assertThat(redisTemplate.hasKey("bodeum:auth:refresh:" + hashToken("rotated-after-withdrawal")))
                .isFalse();
        // 소비된 tombstone은 TTL까지 남지만 C 상태라 다시 소비될 수 없다.
        assertThat(refreshTokenStore.consume(hashToken(pair.refreshToken()), Instant.now())).isEmpty();
    }

    @Test
    void revokeFamily_keepsOtherDeviceRefreshSessionActive() {
        AuthTokenService service = buildAuthTokenService(userServiceReturning(15L));
        AuthTokenService.AuthTokenPair firstDevice = service.issueTokens(15L);
        AuthTokenService.AuthTokenPair secondDevice = service.issueTokens(15L);

        refreshTokenStore.revokeFamily(15L, hashToken(firstDevice.refreshToken()));

        assertThatThrownBy(() -> service.refresh(firstDevice.refreshToken()))
                .isInstanceOf(ProjectException.class);
        assertThat(service.refresh(secondDevice.refreshToken()).refreshToken()).isNotBlank();
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
    void denylist_blocksTokenIssuedInSameSecondAsRevocation() {
        Instant revokedAt = Instant.now();
        denylist.revokeAllBefore("subject-x", revokedAt);

        // 동일 초 발급 토큰도 차단(<=), 이전 발급도 차단, 이후(+1s) 발급만 허용
        assertThat(denylist.isRevoked("subject-x", revokedAt)).isTrue();
        assertThat(denylist.isRevoked("subject-x", revokedAt.minusSeconds(1))).isTrue();
        assertThat(denylist.isRevoked("subject-x", revokedAt.plusSeconds(1))).isFalse();
    }

    @Test
    void tokenDenylist_blocksOnlySpecifiedAccessToken() {
        Instant expiresAt = Instant.now().plusSeconds(60);
        denylist.revokeToken("token-a", expiresAt);

        assertThat(denylist.isRevoked("subject-x", "token-a", Instant.now())).isTrue();
        assertThat(denylist.isRevoked("subject-x", "token-b", Instant.now())).isFalse();
        assertThat(redisTemplate.getExpire("bodeum:auth:denylist-token:token-a"))
                .isBetween(1L, 60L);
    }

    @Test
    void deviceLogout_blocksOnlyCurrentDeviceTokens() {
        AuthTokenService service = buildAuthTokenService(userServiceReturning(17L));
        AuthTokenService.AuthTokenPair firstDevice = service.issueTokens(17L);
        AuthTokenService.AuthTokenPair secondDevice = service.issueTokens(17L);

        service.revoke(17L, firstDevice.refreshToken());
        service.revokeAccessToken(firstDevice.accessToken());

        assertThat(service.authenticate(firstDevice.accessToken())).isEmpty();
        assertThat(service.authenticate(secondDevice.accessToken())).isPresent();
        assertThatThrownBy(() -> service.refresh(firstDevice.refreshToken()))
                .isInstanceOf(ProjectException.class);
        assertThat(service.refresh(secondDevice.refreshToken()).accessToken()).isNotBlank();
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
        when(userService.findActiveUserByAuthSubject("subject-" + userId))
                .thenReturn(Optional.of(user));
        return userService;
    }

    private Set<String> refreshKeys() {
        return redisTemplate.keys("bodeum:auth:refresh:*");
    }

    private String hashToken(String token) {
        try {
            return HexFormat.of().formatHex(
                    MessageDigest.getInstance("SHA-256")
                            .digest(token.getBytes(StandardCharsets.UTF_8))
            );
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    private void flushTestKeys() {
        Set<String> keys = redisTemplate.keys("bodeum:auth:*");
        if (keys != null && !keys.isEmpty()) {
            redisTemplate.delete(keys);
        }
    }
}
