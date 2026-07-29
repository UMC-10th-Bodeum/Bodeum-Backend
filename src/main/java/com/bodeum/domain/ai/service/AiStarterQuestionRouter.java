package com.bodeum.domain.ai.service;

import com.bodeum.domain.ai.entity.AiExternalDocument;
import com.bodeum.domain.ai.entity.AiExternalSource;
import com.bodeum.domain.ai.enums.AiExternalSourceType;
import com.bodeum.domain.ai.enums.AiResponseSourceType;
import com.bodeum.domain.ai.enums.AiStarterQuestionType;
import com.bodeum.domain.ai.exception.AiErrorCode;
import com.bodeum.domain.ai.infrastructure.external.AiExternalDocumentCandidate;
import com.bodeum.domain.ai.infrastructure.external.AiExternalDocumentPersistenceService;
import com.bodeum.domain.ai.model.answer.AiStarterQuestionAnswer;
import com.bodeum.domain.ai.model.rag.AiReferenceDocument;
import com.bodeum.domain.ai.model.rag.AiUserProfile;
import com.bodeum.domain.ai.repository.AiExternalSourceRepository;
import com.bodeum.domain.info.entity.InfoItem;
import com.bodeum.domain.info.repository.InfoItemRepository;
import com.bodeum.global.common.constant.TimeConstants;
import com.bodeum.global.apiPayload.exception.ProjectException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AiStarterQuestionRouter {

    private static final int LOCAL_CENTER_LIMIT = 5;
    private static final String REGION_REQUIRED_MESSAGE =
            "활동 지역이 설정되어 있지 않습니다. "
                    + "확인할 시·도와 시·군·구를 알려주세요.";
    private static final List<String> CIRCLED_NUMBERS =
            List.of("①", "②", "③", "④", "⑤");
    private static final List<WelfareSiteSpec> WELFARE_SITES = List.of(
            new WelfareSiteSpec(
                    "bokjiro.go.kr",
                    "복지로 (bokjiro.go.kr)",
                    "바우처·복지급여 온라인 신청, 나에게 맞는 혜택 모의 조회 가능"
            ),
            new WelfareSiteSpec(
                    "broso.or.kr",
                    "보건복지부 발달장애인지원포털",
                    "발달장애인 지원사업, 부모교육 정보 안내"
            ),
            new WelfareSiteSpec(
                    "socialservice.or.kr",
                    "사회서비스 전자바우처 (socialservice.or.kr)",
                    "발달재활서비스 등 바우처 지정기관 조회"
            ),
            new WelfareSiteSpec(
                    "ggdf.co.kr",
                    "경기도 장애인가족지원센터",
                    "경기도 거주 가정 대상 상담·정보연계·부모교육"
            ),
            new WelfareSiteSpec(
                    "gov.kr",
                    "정부24 (gov.kr)",
                    "장애인 등록, 각종 증명서 발급 등 행정 절차 안내"
            )
    );
    private static final String CHILD_MEDICAL_SUPPORT_ANSWER = """
            의료비 관련 제도를 조건별로 나눠서 정리해드릴게요!

            **장애아동이 받을 수 있는 의료비 지원**

            - **장애인 의료비 지원** — 의료급여 2종 또는 차상위 본인부담 경감대상인 등록장애인이 대상이에요. 18세 미만 등록장애인도 대상에 포함되며, 진료비 본인부담금 일부를 지원받을 수 있어요.
            - **본인부담액상한제** — 소득과 관계없이 모든 건강보험 가입자에게 적용돼요. 1년간 낸 건강보험 본인부담금이 개인별 상한액을 넘으면 초과분을 공단에서 돌려줘요. (비급여·상급병실료 등은 제외)
            - **재난적의료비 지원사업** — 2026년 기준, 기준 중위소득 100% 이하 가구가 감당하기 어려운 의료비를 부담한 경우, 비급여 포함 의료비 일부를 연간 한도 내에서 지원받을 수 있어요.

            소득 구간과 등록 여부에 따라 해당되는 제도가 달라서, 국민건강보험공단(1577-1000)이나 거주지 주민센터에서 우리 가구 상황에 맞는 지원을 확인해보시는 게 가장 정확해요.
            """;
    private static final List<ExternalDocumentSpec> CHILD_MEDICAL_SUPPORT_SOURCES = List.of(
            new ExternalDocumentSpec(
                    "mohw.go.kr",
                    "장애인 의료비 지원",
                    "https://www.mohw.go.kr/menu.es?mid=a10710060700"
            ),
            new ExternalDocumentSpec(
                    "nhis.or.kr",
                    "본인부담액상한제",
                    "https://www.nhis.or.kr/nhis/minwon/minwonServiceBoard.do"
                            + "?mode=list&etcChar1=446&etcChar2=447&etcChar3=448"
                            + "&categories1=446%2C447%2C448&articleLimit=12"
                            + "&nhisOrderTy=ORDER_DT&srSearchVal=%EB%B3%B8%EC%9D%B8"
                            + "%EB%B6%80%EB%8B%B4%EA%B8%88"
            ),
            new ExternalDocumentSpec(
                    "nhis.or.kr",
                    "재난적의료비 지원사업",
                    "https://www.nhis.or.kr/nhis/minwon/minwonServiceBoard.do"
                            + "?mode=view&articleNo=11009687&article.offset=0"
                            + "&articleLimit=12&srSearchVal=%EB%B3%B8%EC%9D%B8"
                            + "%EB%B6%80%EB%8B%B4"
            )
    );
    private static final String DIAGNOSIS_FIRST_STEPS_ANSWER = """
            지금 챙기시면 좋은 것들을 순서대로 정리해드릴게요!

            **진단 이후 챙기면 좋은 순서**

            **① 장애인 등록** — 병원에서 받은 진단서로 주민센터에서 등록 신청하시면, 이후 대부분의 지원제도를 이용할 기본 자격이 마련돼요.

            **② 발달재활서비스 바우처 신청** — 등록과 동시에 신청 가능하며, 언어·감각통합 등 치료비 부담을 크게 줄일 수 있어요.

            **③ 의료비 지원 대상 확인** — 가구 소득 구간에 따라 의료비 본인부담을 덜 수 있는 제도가 있어요.

            **④ 지역 기관 연결** — 중앙장애아동·발달장애인지원센터 경기지역센터에서 초기 부모 상담과 지역 정보 연계를 받아보실 수 있어요.

            소득 구간과 등록 여부에 따라 해당되는 제도 및 서비스가 달라서, 거주지 주민센터에서 우리 가구 상황에 맞는 지원을 확인해보시는 게 가장 정확해요.
            """;
    private static final List<ExternalDocumentSpec> DIAGNOSIS_FIRST_STEPS_SOURCES = List.of(
            new ExternalDocumentSpec(
                    "mohw.go.kr",
                    "2026년 장애아동가족지원 사업안내",
                    "https://www.mohw.go.kr/board.es?mid=a10411010100&bid=0019"
                            + "&act=view&list_no=1489566&tag=&nPage=1"
            ),
            new ExternalDocumentSpec(
                    "socialservice.or.kr",
                    "발달재활서비스",
                    "https://www.socialservice.or.kr:444/user/htmlEditor/view2.do?p_sn=11"
            ),
            new ExternalDocumentSpec(
                    "bokjiro.go.kr",
                    "복지서비스 신청",
                    "https://www.bokjiro.go.kr/ssis-tbu/twatzzza/intgSearch/"
                            + "moveTWZZ01000M.do"
            ),
            new ExternalDocumentSpec(
                    "129.go.kr",
                    "보건복지상담센터",
                    "https://www.129.go.kr/"
            )
    );
    private static final String VOUCHER_APPLICATION_ANSWER = """
            네, 발달재활서비스 바우처 신청 절차를 정리해드릴게요!

            **발달재활서비스 바우처 신청 안내**

            - **자격 확인** — 2026년 기준, 만 18세 미만 등록 장애아동(자폐성 포함)이며, 기준 중위소득 180% 이하 가구면 신청할 수 있어요. 만 9세 미만은 장애 미등록 상태여도 전문의 발달재활서비스 의뢰서, 세부영역검사결과서 및 검사자료로 대체하여 신청 가능해요.
            - **신청 방법** — 복지로(bokjiro.go.kr) 온라인 신청 또는 관할 주민센터 방문 신청, 두 가지 방법이 있어요.
            - **필요 서류** — 장애인등록증(또는 발달재활서비스 의뢰서·검사자료), 건강보험료 납부확인서, 사회복지서비스 이용권 제공 신청서예요.
            - **이용 가능 기관** — 사회서비스 전자바우처(socialservice.or.kr)에서 {{activityRegion}} 지정기관을 확인하실 수 있어요.

            소득 구간에 따라 월 지원금액이 차등 지급되며, 언어·청능, 미술심리·음악·놀이심리·행동발달·재활심리·심리운동, 감각발달·운동발달 재활영역에 사용하실 수 있어요. 보다 정확한 사항은 보건복지부나 거주지 주민센터에서 우리 가구 상황에 맞는 지원을 확인해보시는 게 가장 정확해요.
            """;
    private static final List<ExternalDocumentSpec> VOUCHER_APPLICATION_SOURCES = List.of(
            new ExternalDocumentSpec(
                    "mohw.go.kr",
                    "발달재활서비스",
                    "https://www.mohw.go.kr/menu.es?mid=a10710060600"
            ),
            new ExternalDocumentSpec(
                    "socialservice.or.kr",
                    "발달재활서비스",
                    "https://www.socialservice.or.kr:444/user/htmlEditor/view2.do?p_sn=11"
            )
    );

    private final AiExternalSourceRepository externalSourceRepository;
    private final AiExternalDocumentPersistenceService externalDocumentPersistenceService;
    private final InfoItemRepository infoItemRepository;

    public AiStarterQuestionRouter(
            AiExternalSourceRepository externalSourceRepository,
            AiExternalDocumentPersistenceService externalDocumentPersistenceService,
            InfoItemRepository infoItemRepository
    ) {
        this.externalSourceRepository = externalSourceRepository;
        this.externalDocumentPersistenceService = externalDocumentPersistenceService;
        this.infoItemRepository = infoItemRepository;
    }

    @Transactional
    public Optional<AiStarterQuestionAnswer> route(
            AiStarterQuestionType type,
            AiUserProfile profile
    ) {
        return switch (type) {
            case WELFARE_SITES -> Optional.of(welfareSites());
            case LOCAL_REHAB_CENTERS -> Optional.of(localRehabCenters(profile));
            case CHILD_MEDICAL_SUPPORT -> Optional.of(childMedicalSupport());
            case DIAGNOSIS_FIRST_STEPS -> Optional.of(diagnosisFirstSteps());
            case VOUCHER_APPLICATION -> Optional.of(voucherApplication(profile));
        };
    }

    // -------------------------------------------------------------------------
    // 질문별 답변 생성
    // -------------------------------------------------------------------------

    private AiStarterQuestionAnswer welfareSites() {
        List<AiExternalSource> sources = findRegisteredSources(
                WELFARE_SITES.stream().map(WelfareSiteSpec::host).toList()
        );
        if (sources.size() != WELFARE_SITES.size()) {
            return AiStarterQuestionAnswer.noEvidence();
        }

        List<AiReferenceDocument> references = persistSourceEntryPagesAsReferences(sources);

        String content = IntStream.range(0, WELFARE_SITES.size())
                .mapToObj(index -> {
                    WelfareSiteSpec spec = WELFARE_SITES.get(index);
                    return String.format(
                            "- **%s** — %s",
                            spec.displayName(),
                            spec.description()
                    );
                })
                .collect(Collectors.joining(
                        "\n",
                        "네, 참고하면 좋을 공식 복지 사이트 5개를 추천드리겠습니다!\n\n"
                                + "**자주 확인하면 좋은 공식 복지 사이트**\n\n",
                        "\n\n이 사이트들은 모두 정부·공공기관 및 공식 지원기관이 직접 운영해서 정보 신뢰도가 높아요. "
                                + "보듬에서도 이 출처들을 기반으로 최신 정보를 정리해드리고 있습니다."
                ));
        return AiStarterQuestionAnswer.answered(content, references);
    }

    private AiStarterQuestionAnswer localRehabCenters(AiUserProfile profile) {
        Optional<RegionParts> region = RegionParts.from(profile.region());
        if (region.isEmpty()) {
            return AiStarterQuestionAnswer.regionRequired(REGION_REQUIRED_MESSAGE);
        }

        List<InfoItem> centers = infoItemRepository.findRehabCentersByRegion(
                region.get().sido(),
                region.get().sigungu(),
                PageRequest.of(0, LOCAL_CENTER_LIMIT)
        );
        if (centers.isEmpty()) {
            return AiStarterQuestionAnswer.noEvidence();
        }

        List<AiReferenceDocument> references = centers.stream()
                .map(this::toReferenceDocument)
                .toList();
        String content = IntStream.range(0, centers.size())
                .mapToObj(index -> centerCard(centers.get(index), index))
                .collect(Collectors.joining(
                        "\n\n",
                        region.get().displayName()
                                + "에서 확인 가능한 재활센터를 정리해드렸어요!\n"
                                + "조회, 저장, 후기를 기준으로 정렬된 것이며, 기관의 우수성을 "
                                + "판단한 결과는 아닙니다.\n"
                                + "방문 전 꼭 직접 확인하시는 것을 권장합니다.\n\n",
                        "\n\n> 기관별 대기 여부와 상담 가능 시간은 자주 바뀔 수 있으므로 "
                                + "방문 전 꼭 전화로 확인해보시는 것을 추천드려요 🍀"
                ));
        return AiStarterQuestionAnswer.answered(content, references);
    }

    private AiStarterQuestionAnswer childMedicalSupport() {
        return fixedAnswerWithRequiredSources(
                CHILD_MEDICAL_SUPPORT_ANSWER,
                CHILD_MEDICAL_SUPPORT_SOURCES
        );
    }

    private AiStarterQuestionAnswer diagnosisFirstSteps() {
        return fixedAnswerWithRequiredSources(
                DIAGNOSIS_FIRST_STEPS_ANSWER,
                DIAGNOSIS_FIRST_STEPS_SOURCES
        );
    }

    private AiStarterQuestionAnswer voucherApplication(AiUserProfile profile) {
        String activityRegion = profile.region() == null || profile.region().isBlank()
                ? "지역별"
                : profile.region().trim() + " 내";
        return fixedAnswerWithRequiredSources(
                VOUCHER_APPLICATION_ANSWER.replace(
                        "{{activityRegion}}",
                        activityRegion
                ),
                VOUCHER_APPLICATION_SOURCES
        );
    }

    // -------------------------------------------------------------------------
    // 고정 답변 공통 처리
    // -------------------------------------------------------------------------

    private AiStarterQuestionAnswer fixedAnswerWithRequiredSources(
            String answer,
            List<ExternalDocumentSpec> sourceSpecs
    ) {
        List<String> requiredHosts = sourceSpecs.stream()
                .map(ExternalDocumentSpec::host)
                .distinct()
                .toList();
        Map<String, AiExternalSource> sourcesByHost =
                findRegisteredSources(requiredHosts).stream()
                .collect(Collectors.toMap(
                        source -> normalizedHost(source.getBaseUrl()),
                        Function.identity()
                ));
        if (!sourcesByHost.keySet().containsAll(requiredHosts)) {
            return AiStarterQuestionAnswer.noEvidence();
        }

        List<AiExternalDocumentCandidate> candidates = sourceSpecs.stream()
                .map(spec -> {
                    AiExternalSource source = sourcesByHost.get(spec.host());
                    String url = normalizeUrl(spec.url());
                    return new AiExternalDocumentCandidate(
                            source,
                            spec.title(),
                            url,
                            sha256(url)
                    );
                })
                .toList();

        return AiStarterQuestionAnswer.answered(
                answer,
                persistCandidatesAsReferences(candidates)
        );
    }

    // -------------------------------------------------------------------------
    // 외부 SITE 출처 처리
    // -------------------------------------------------------------------------

    // 활성 외부 사이트의 baseUrl에서 도메인 추출 후, 요청한 hosts 순서대로 사이트 반환
    private List<AiExternalSource> findRegisteredSources(List<String> hosts) {
        Map<String, AiExternalSource> sourcesByHost = externalSourceRepository
                .findAllBySourceTypeAndActiveTrue(AiExternalSourceType.WEBSITE)
                .stream()
                .collect(Collectors.toMap(
                        source -> normalizedHost(source.getBaseUrl()),
                        Function.identity(),
                        (first, ignored) -> first,
                        LinkedHashMap::new
                ));
        return hosts.stream()
                .map(sourcesByHost::get)
                .filter(java.util.Objects::nonNull)
                .toList();
    }

    // 등록된 외부 사이트의 대표 진입 페이지를 AI 응답 출처로 변환
    private List<AiReferenceDocument> persistSourceEntryPagesAsReferences(
            List<AiExternalSource> sources
    ) {
        List<AiExternalDocumentCandidate> candidates = sources.stream()
                .map(source -> {
                    String url = normalizeUrl(preferredUrl(source));
                    return new AiExternalDocumentCandidate(
                            source,
                            source.getName(),
                            url,
                            sha256(url)
                    );
                })
                .toList();
        return persistCandidatesAsReferences(candidates);
    }

    // 외부 문서 후보를 ai_external_document에 저장 또는 갱신하고, AI 응답 출처로 변환
    private List<AiReferenceDocument> persistCandidatesAsReferences(
            List<AiExternalDocumentCandidate> candidates
    ) {
        List<AiExternalDocument> documents =
                externalDocumentPersistenceService.saveAll(candidates);

        List<AiReferenceDocument> references = IntStream.range(0, documents.size())
                .mapToObj(index -> {
                    AiExternalDocument document = documents.get(index);
                    AiExternalSource source = candidates.get(index).externalSource();
                    return new AiReferenceDocument(
                            "SITE-" + document.getId(),
                            source.getDescription(),
                            AiResponseSourceType.SITE,
                            document.getId(),
                            source.getName(),
                            document.getSourceUrl(),
                            document.getSourceUpdatedAt()
                    );
                })
                .toList();
        return references;
    }

    // -------------------------------------------------------------------------
    // URL·해시 등 공통 유틸리티
    // -------------------------------------------------------------------------

    // entryUrl이 있으면 우선 사용하고, 없으면 baseUrl을 사용
    private String preferredUrl(AiExternalSource source) {
        return source.getEntryUrl() == null || source.getEntryUrl().isBlank()
                ? source.getBaseUrl()
                : source.getEntryUrl();
    }

    private String normalizedHost(String url) {
        String host = URI.create(normalizeUrl(url)).getHost();
        if (host == null) {
            return "";
        }
        String normalized = host.toLowerCase(Locale.ROOT);
        return normalized.startsWith("www.") ? normalized.substring(4) : normalized;
    }

    // URL 정규화
    private String normalizeUrl(String url) {
        URI uri = URI.create(url.trim()).normalize();
        try {
            return new URI(
                    uri.getScheme() == null ? "https" : uri.getScheme().toLowerCase(Locale.ROOT),
                    uri.getUserInfo(),
                    uri.getHost() == null ? null : uri.getHost().toLowerCase(Locale.ROOT),
                    uri.getPort(),
                    uri.getPath(),
                    uri.getQuery(),
                    null
            ).toString();
        } catch (Exception e) {
            throw new ProjectException(AiErrorCode.AI_RESPONSE_FAILED, e);
        }
    }

    // 중복 문서 식별에 사용할 URL SHA-256 해시를 생성
    private String sha256(String value) {
        try {
            return HexFormat.of().formatHex(
                    MessageDigest.getInstance("SHA-256")
                            .digest(value.getBytes(StandardCharsets.UTF_8))
            );
        } catch (NoSuchAlgorithmException e) {
            throw new ProjectException(AiErrorCode.AI_RESPONSE_FAILED, e);
        }
    }

    // -------------------------------------------------------------------------
    // INFO 출처 처리
    // -------------------------------------------------------------------------

    // InfoItem을 AI 응답 출처 모델로 변환
    private AiReferenceDocument toReferenceDocument(InfoItem info) {
        return new AiReferenceDocument(
                "INFO-" + info.getId(),
                infoContent(info),
                AiResponseSourceType.INFO,
                info.getId(),
                info.getName(),
                info.getHomepageUrl(),
                sourceUpdatedAt(info)
        );
    }

    // InfoItem의 상세 내용을 AI 근거 문서 본문으로 변환
    private String infoContent(InfoItem info) {
        return String.format(
                """
                정보명: %s
                분류: %s
                소개: %s
                주소: %s
                지역: %s %s
                전화번호: %s
                홈페이지: %s
                """,
                info.getName(),
                info.getInfoCategory().getSubCategoryKo(),
                value(info.getIntroduction()),
                info.getAddress(),
                info.getSido(),
                info.getSigungu(),
                value(info.getPhone()),
                value(info.getHomepageUrl())
        ).trim();
    }

    // 재활센터 정보를 사용자 답변에 표시할 Markdown 카드로 변환
    private String centerCard(InfoItem info, int index) {
        StringBuilder card = new StringBuilder("**")
                .append(CIRCLED_NUMBERS.get(index))
                .append(" ")
                .append(info.getName())
                .append("**\n\n`")
                .append(info.getInfoCategory().getSubCategoryKo())
                .append("` — ")
                .append(info.getAddress())
                .append("\n\n")
                .append(value(info.getIntroduction()));
        return card.toString();
    }

    // INFO 출처의 갱신 시각 결정
    private Instant sourceUpdatedAt(InfoItem info) {
        if (info.getUpdatedAt() != null) {
            return info.getUpdatedAt();
        }
        if (info.getCreatedAt() != null) {
            return info.getCreatedAt();
        }
        return info.getSyncedAt() == null
                ? null
                : info.getSyncedAt().atZone(TimeConstants.SERVICE_ZONE_ID).toInstant();
    }

    private String value(String value) {
        return value == null || value.isBlank() ? "확인 필요" : value;
    }

    // -------------------------------------------------------------------------
    // 내부 데이터 구조
    // -------------------------------------------------------------------------

    // 복지 사이트 고정 답변에 사용할 표시 정보
    private record WelfareSiteSpec(
            String host,
            String displayName,
            String description
    ) {
    }

    // 검수된 고정 답변에 연결할 공식 상세 페이지 정보
    private record ExternalDocumentSpec(
            String host,
            String title,
            String url
    ) {
    }

    // 전체 지역명을 시·도와 시·군·구로 분리해 관리
    private record RegionParts(String sido, String sigungu) {

        private static Optional<RegionParts> from(String fullName) {
            if (fullName == null || fullName.isBlank()) {
                return Optional.empty();
            }
            String trimmed = fullName.trim();
            int separator = trimmed.indexOf(' ');
            if (separator < 1 || separator == trimmed.length() - 1) {
                return Optional.empty();
            }
            return Optional.of(new RegionParts(
                    trimmed.substring(0, separator),
                    trimmed.substring(separator + 1).trim()
            ));
        }

        private String displayName() {
            return sido + " " + sigungu;
        }
    }
}
