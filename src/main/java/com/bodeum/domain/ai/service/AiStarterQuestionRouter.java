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
    // PM 검수 답변과 근거 출처가 확정되면 이 슬롯에 연결한다.
    private static final String DIAGNOSIS_FIRST_STEPS_ANSWER = "";
    private static final List<String> DIAGNOSIS_FIRST_STEPS_SOURCE_HOSTS = List.of(
            "bokjiro.go.kr",
            "broso.or.kr"
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
            case DIAGNOSIS_FIRST_STEPS -> diagnosisFirstSteps();
            case CHILD_MEDICAL_SUPPORT, VOUCHER_APPLICATION -> Optional.empty();
        };
    }

    private Optional<AiStarterQuestionAnswer> diagnosisFirstSteps() {
        if (DIAGNOSIS_FIRST_STEPS_ANSWER.isBlank()) {
            return Optional.empty();
        }
        List<AiExternalSource> sources =
                findRegisteredSources(DIAGNOSIS_FIRST_STEPS_SOURCE_HOSTS);
        if (sources.size() != DIAGNOSIS_FIRST_STEPS_SOURCE_HOSTS.size()) {
            return Optional.of(AiStarterQuestionAnswer.noEvidence());
        }
        return Optional.of(AiStarterQuestionAnswer.answered(
                DIAGNOSIS_FIRST_STEPS_ANSWER,
                persistAsReferences(sources)
        ));
    }

    private AiStarterQuestionAnswer welfareSites() {
        List<AiExternalSource> sources = findRegisteredSources(
                WELFARE_SITES.stream().map(WelfareSiteSpec::host).toList()
        );
        if (sources.size() != WELFARE_SITES.size()) {
            return AiStarterQuestionAnswer.noEvidence();
        }

        List<AiReferenceDocument> references = persistAsReferences(sources);

        String content = java.util.stream.IntStream.range(0, WELFARE_SITES.size())
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
                        "\n\n이 사이트들은 모두 공공기관이 직접 운영해서 정보 신뢰도가 높아요. "
                                + "보듬에서도 이 출처들을 기반으로 최신 정보를 정리해드리고 있습니다."
                ));
        return AiStarterQuestionAnswer.answered(content, references);
    }

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

    private List<AiReferenceDocument> persistAsReferences(
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
        List<AiExternalDocument> documents =
                externalDocumentPersistenceService.saveAll(candidates);

        List<AiReferenceDocument> references = java.util.stream.IntStream
                .range(0, documents.size())
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
        String content = java.util.stream.IntStream.range(0, centers.size())
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

    private String value(String value) {
        return value == null || value.isBlank() ? "확인 필요" : value;
    }

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

    private record WelfareSiteSpec(
            String host,
            String displayName,
            String description
    ) {
    }
}
