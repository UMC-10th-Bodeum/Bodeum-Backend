package com.bodeum.domain.auth.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.bodeum.domain.auth.repository.AuthLoginCodeRepository;
import com.bodeum.domain.auth.repository.OAuthStateRepository;
import com.bodeum.domain.auth.repository.RefreshTokenSessionRepository;
import com.bodeum.domain.user.repository.UserRepository;
import com.bodeum.domain.user.service.UserService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.web.util.UriComponentsBuilder;

@SpringBootTest(properties = {
        "bodeum.auth.jwt-secret=test-jwt-secret-32-bytes-minimum-value",
        "bodeum.oauth.mock-enabled=true",
        "bodeum.oauth.naver.client-id=test-naver-client",
        "bodeum.oauth.naver.client-secret=test-naver-secret"
})
@AutoConfigureMockMvc
class AuthControllerTest {

    private static final String FRONT_CALLBACK_URL = "http://localhost:3000/auth/callback";

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RefreshTokenSessionRepository refreshTokenSessionRepository;

    @Autowired
    private OAuthStateRepository oAuthStateRepository;

    @Autowired
    private AuthLoginCodeRepository authLoginCodeRepository;

    @Autowired
    private UserService userService;

    @BeforeEach
    void setUp() {
        refreshTokenSessionRepository.deleteAll();
        oAuthStateRepository.deleteAll();
        authLoginCodeRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void mockCallbackIssuesTokenThatAuthenticatesProtectedEndpoint() throws Exception {
        JsonNode loginBody = login("mock-code");
        assertThat(loginBody.at("/result/accessToken").asText()).isNotEmpty();
        assertThat(loginBody.at("/result/refreshToken").asText()).isNotEmpty();
        String accessToken = loginBody.at("/result/accessToken").asText();

        mockMvc.perform(get("/api/v1/users/me/profile")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.userId").isNumber())
                .andExpect(jsonPath("$.result.level").isNumber());
    }

    @Test
    void callbackRedirectsToFrontWithOneTimeCode() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/v1/auth/callback/kakao")
                        .param("code", "redirect-code"))
                .andExpect(status().isFound())
                .andReturn();

        String location = result.getResponse().getHeader(HttpHeaders.LOCATION);
        assertThat(location)
                .startsWith(FRONT_CALLBACK_URL)
                .contains("code=");
    }

    @Test
    void exchangeWithUnknownCodeIsUnauthorized() throws Exception {
        mockMvc.perform(post("/api/v1/auth/exchange")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("code", "does-not-exist"))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTH401_7"));
    }

    @Test
    void exchangeWithoutCodeIsBadRequest() throws Exception {
        mockMvc.perform(post("/api/v1/auth/exchange")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void oneTimeCodeCannotBeExchangedTwice() throws Exception {
        String oneTimeCode = issueLoginCode("single-use-code");
        String requestBody = objectMapper.writeValueAsString(Map.of("code", oneTimeCode));

        mockMvc.perform(post("/api/v1/auth/exchange")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/auth/exchange")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTH401_7"));
    }

    @Test
    void refreshRotatesRefreshTokenAndRejectsOldToken() throws Exception {
        JsonNode loginBody = login("refresh-code");
        String refreshToken = loginBody.at("/result/refreshToken").asText();

        String refreshBody = objectMapper.writeValueAsString(Map.of("refreshToken", refreshToken));
        MvcResult refreshResult = mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(refreshBody))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode refreshedBody = readBody(refreshResult);
        assertThat(refreshedBody.at("/result/refreshToken").asText()).isNotEqualTo(refreshToken);

        mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(refreshBody))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void loginRedirectForConfiguredProviderIncludesState() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/v1/auth/login/naver"))
                .andExpect(status().isFound())
                .andReturn();

        String location = result.getResponse().getHeader(HttpHeaders.LOCATION);
        assertThat(location)
                .startsWith("https://nid.naver.com/oauth2.0/authorize")
                .contains("client_id=test-naver-client")
                .contains("state=");
    }

    @Test
    void configuredProviderCallbackWithoutStateRedirectsWithError() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/v1/auth/callback/naver")
                        .param("code", "provider-code"))
                .andExpect(status().isFound())
                .andReturn();

        assertThat(result.getResponse().getHeader(HttpHeaders.LOCATION))
                .startsWith(FRONT_CALLBACK_URL)
                .contains("error=AUTH401_6");
    }

    @Test
    void callbackWithoutCodeRedirectsWithError() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/v1/auth/callback/kakao"))
                .andExpect(status().isFound())
                .andReturn();

        assertThat(result.getResponse().getHeader(HttpHeaders.LOCATION))
                .startsWith(FRONT_CALLBACK_URL)
                .contains("error=AUTH400_2");
    }

    @Test
    void agreementCanBeRegisteredThroughPluralPath() throws Exception {
        String accessToken = login("agreement-code").at("/result/accessToken").asText();

        mockMvc.perform(post("/api/v1/users/me/agreements")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "serviceTermsAgreed": true,
                                  "privacyPolicyAgreed": true,
                                  "aiTermsAgreed": false
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.serviceTermsAgreed").value(true))
                .andExpect(jsonPath("$.result.privacyPolicyAgreed").value(true));
    }

    @Test
    void agreementWithoutRequiredTermsReturnsError() throws Exception {
        String accessToken = login("agreement-required-code").at("/result/accessToken").asText();

        mockMvc.perform(post("/api/v1/users/me/agreements")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "serviceTermsAgreed": true,
                                  "privacyPolicyAgreed": false,
                                  "aiTermsAgreed": false
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("AUTH400_4"));
    }

    @Test
    void profileCanBeReadAndUpdatedThroughProfilePath() throws Exception {
        String accessToken = login("profile-path-code").at("/result/accessToken").asText();

        mockMvc.perform(get("/api/v1/users/me/profile")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.userId").isNumber())
                .andExpect(jsonPath("$.result.childProfile").exists())
                .andExpect(jsonPath("$.result.activitySummary.savedInfoCount").value(0))
                .andExpect(jsonPath("$.result.activitySummary.myPostCount").value(0))
                .andExpect(jsonPath("$.result.activitySummary.myCommentCount").value(0));

        mockMvc.perform(patch("/api/v1/users/me/profile")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "nickname": "민준맘",
                                  "childNickname": "민준",
                                  "childBirth": "2020-03",
                                  "disabilityTypes": ["AUTISM", "CEREBRAL_PALSY"]
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.updated").value(true));
    }

    @Test
    void scrapsCanBeReadThroughMyScrapsPath() throws Exception {
        String accessToken = login("my-scraps-path-code").at("/result/accessToken").asText();

        mockMvc.perform(get("/api/v1/users/me/scraps")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isSuccess").value(true))
                .andExpect(jsonPath("$.code").value("COMMON200_1"))
                .andExpect(jsonPath("$.result.totalCount").value(0))
                .andExpect(jsonPath("$.result.infoScraps").isArray())
                .andExpect(jsonPath("$.result.infoScraps").isEmpty())
                .andExpect(jsonPath("$.result.newsScraps").isArray())
                .andExpect(jsonPath("$.result.newsScraps").isEmpty());
    }

    @Test
    void postsCanBeReadThroughMyPostsPath() throws Exception {
        String accessToken = login("my-posts-path-code").at("/result/accessToken").asText();

        mockMvc.perform(get("/api/v1/users/me/posts")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isSuccess").value(true))
                .andExpect(jsonPath("$.code").value("COMMON200_1"))
                .andExpect(jsonPath("$.result.totalCount").value(0))
                .andExpect(jsonPath("$.result.page").value(0))
                .andExpect(jsonPath("$.result.size").value(10))
                .andExpect(jsonPath("$.result.totalPages").value(0))
                .andExpect(jsonPath("$.result.hasNext").value(false))
                .andExpect(jsonPath("$.result.posts").isArray())
                .andExpect(jsonPath("$.result.posts").isEmpty());
    }

    @Test
    void postsRejectNegativePage() throws Exception {
        String accessToken = login("my-posts-invalid-page-code").at("/result/accessToken").asText();

        mockMvc.perform(get("/api/v1/users/me/posts")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                        .param("page", "-1")
                        .param("size", "10"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("COMMON400_1"));
    }

    @Test
    void briefReturnsLoggedOutWhenAnonymous() throws Exception {
        mockMvc.perform(get("/api/v1/users/me/brief"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.isLoggedIn").value(false));
    }

    @Test
    void protectedEndpointRejectsWithdrawnUserTokenAsInvalidAccessToken() throws Exception {
        String accessToken = login("withdrawn-token-code").at("/result/accessToken").asText();

        userService.withdraw(userRepository.findAll().getFirst().getId(), null);

        mockMvc.perform(get("/api/v1/users/me/profile")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTH401_1"));
    }

    @Test
    void withdrawnUserGetsFreshAccountOnSocialRelogin() throws Exception {
        JsonNode firstLogin = login("withdrawn-user-code");
        long firstUserId = firstLogin.at("/result/userId").asLong();

        userService.withdraw(firstUserId, null);
        assertThat(userRepository.findById(firstUserId).orElseThrow().isWithdrawn()).isTrue();

        // 탈퇴 후 같은 소셜 계정으로 다시 로그인하면 복구가 아니라 새 회원으로 가입된다.
        JsonNode secondLogin = login("withdrawn-user-code");
        assertThat(secondLogin.at("/result/isNewUser").asBoolean()).isTrue();
        assertThat(secondLogin.at("/result/userId").asLong()).isNotEqualTo(firstUserId);
        // 기존 탈퇴 회원은 그대로 탈퇴 상태로 남는다.
        assertThat(userRepository.findById(firstUserId).orElseThrow().isWithdrawn()).isTrue();
    }

    @Test
    void swaggerDocumentsOnlyClientInputFields() throws Exception {
        JsonNode openApi = readBody(mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andReturn());

        JsonNode codeParameter = findCallbackParameter(openApi, "code");
        assertThat(codeParameter).isNotNull();
        assertThat(codeParameter.path("required").asBoolean()).isTrue();
        assertThat(openApi.at("/paths/~1api~1v1~1users~1me~1summary").isMissingNode())
                .isTrue();
        assertThat(hasParameter(openApi, "/paths/~1api~1v1~1users~1me~1profile/patch/parameters", "userId"))
                .isFalse();
        assertThat(hasParameter(openApi, "/paths/~1api~1v1~1onboarding~1child-profile/post/parameters", "userId"))
                .isFalse();
        assertThat(openApi.at("/components/schemas/UpdateUserProfileRequest/properties/childBirth/example").asText())
                .isEqualTo("2020-03");
        assertThat(openApi.at("/components/schemas/CreateChildProfileRequest/properties/birth/example").asText())
                .isEqualTo("2020-03");
        assertThat(openApi.at("/components/schemas/CreateChildProfileRequest/properties/birthValid").isMissingNode())
                .isTrue();
        assertThat(openApi.at("/components/schemas/CreateUserAgreementRequest/properties/requiredAgreementCompleted").isMissingNode())
                .isTrue();
        assertThat(openApi.at("/components/schemas/CreateUserAgreementRequest/properties/aiTermsAgreedValue").isMissingNode())
                .isTrue();
    }

    @Test
    void invalidProviderCallbackRedirectsWithError() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/v1/auth/callback/google")
                        .param("code", "provider-code"))
                .andExpect(status().isFound())
                .andReturn();

        assertThat(result.getResponse().getHeader(HttpHeaders.LOCATION))
                .startsWith(FRONT_CALLBACK_URL)
                .contains("error=AUTH400_1");
    }

    @Test
    void malformedRefreshBodyIsBadRequest() throws Exception {
        mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("잘못된 요청입니다."))
                .andExpect(jsonPath("$.result").doesNotExist());
    }

    @Test
    void logoutWithoutRefreshTokenIsBadRequest() throws Exception {
        String accessToken = login("logout-validation-code").at("/result/accessToken").asText();

        mockMvc.perform(post("/api/v1/auth/logout")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void logoutRequiresAccessToken() throws Exception {
        mockMvc.perform(post("/api/v1/auth/logout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "refreshToken": "refresh-token"
                                }
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTH401_1"));
    }

    /**
     * 모의 소셜 로그인 콜백(302)으로 일회용 code를 발급받아 반환한다.
     */
    private String issueLoginCode(String authorizationCode) throws Exception {
        MvcResult callbackResult = mockMvc.perform(get("/api/v1/auth/callback/kakao")
                        .param("code", authorizationCode))
                .andExpect(status().isFound())
                .andReturn();

        String location = callbackResult.getResponse().getHeader(HttpHeaders.LOCATION);
        assertThat(location).startsWith(FRONT_CALLBACK_URL);

        return UriComponentsBuilder.fromUriString(location).build().getQueryParams().getFirst("code");
    }

    /**
     * 콜백(302) → 일회용 code 교환(exchange)의 2단계를 수행하고 교환 응답 body를 반환한다.
     */
    private JsonNode login(String authorizationCode) throws Exception {
        String oneTimeCode = issueLoginCode(authorizationCode);

        MvcResult exchangeResult = mockMvc.perform(post("/api/v1/auth/exchange")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("code", oneTimeCode))))
                .andExpect(status().isOk())
                .andReturn();

        return readBody(exchangeResult);
    }

    private JsonNode readBody(MvcResult result) throws Exception {
        return objectMapper.readTree(result.getResponse().getContentAsString());
    }

    private JsonNode findCallbackParameter(JsonNode openApi, String name) {
        for (JsonNode parameter : openApi.at("/paths/~1api~1v1~1auth~1callback~1{provider}/get/parameters")) {
            if (name.equals(parameter.path("name").asText())) {
                return parameter;
            }
        }

        return null;
    }

    private boolean hasParameter(JsonNode openApi, String parametersPath, String name) {
        JsonNode parameters = openApi.at(parametersPath);
        if (parameters.isMissingNode()) {
            String operationPath = parametersPath.substring(0, parametersPath.lastIndexOf("/parameters"));
            assertThat(openApi.at(operationPath).isMissingNode())
                    .as("OpenAPI operation path must exist: %s", operationPath)
                    .isFalse();
            return false;
        }

        assertThat(parameters.isArray())
                .as("OpenAPI parameters must be an array: %s", parametersPath)
                .isTrue();

        for (JsonNode parameter : parameters) {
            if (name.equals(parameter.path("name").asText())) {
                return true;
            }
        }

        return false;
    }
}
