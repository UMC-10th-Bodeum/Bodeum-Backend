package com.bodeum.domain.ai.service;

import com.bodeum.domain.ai.model.rag.AiUserProfile;
import com.bodeum.domain.region.entity.Region;
import com.bodeum.domain.region.repository.RegionRepository;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

@Component
public class AiQuestionRegionResolver {

    private static final String GWANGJU_METROPOLITAN_CITY = "광주광역시";
    private static final Pattern REGION_LEVEL_2_PATTERN =
            Pattern.compile("([가-힣]+(?:시|군|구))");
    private static final Pattern KOREAN_WORD_PATTERN = Pattern.compile("[가-힣]{2,}");
    private static final Pattern REGION_ONLY_REMAINDER_PATTERN = Pattern.compile(
            "^(?:은|는|도|은요|는요|요|에|에서|에선|에서는|쪽|지역)?$");
    private static final Map<String, String> REGION_FULL_NAME_ALIASES = Map.of(
            "경기광주", "경기도 광주시"
    );
    private static final Map<String, String> REGION_LEVEL_1_ALIASES = Map.ofEntries(
            Map.entry("서울", "서울특별시"), Map.entry("서울시", "서울특별시"),
            Map.entry("부산", "부산광역시"), Map.entry("부산시", "부산광역시"),
            Map.entry("대구", "대구광역시"), Map.entry("대구시", "대구광역시"),
            Map.entry("인천", "인천광역시"), Map.entry("인천시", "인천광역시"),
            Map.entry("광주광역시", "광주광역시"),
            Map.entry("대전", "대전광역시"), Map.entry("대전시", "대전광역시"),
            Map.entry("울산", "울산광역시"), Map.entry("울산시", "울산광역시"),
            Map.entry("세종", "세종특별자치시"), Map.entry("세종시", "세종특별자치시"),
            Map.entry("경기", "경기도"), Map.entry("강원", "강원특별자치도"),
            Map.entry("충북", "충청북도"), Map.entry("충남", "충청남도"),
            Map.entry("전북", "전북특별자치도"), Map.entry("전남", "전라남도"),
            Map.entry("경북", "경상북도"), Map.entry("경남", "경상남도"),
            Map.entry("제주", "제주특별자치도")
    );

    private final RegionRepository regionRepository;

    public AiQuestionRegionResolver(RegionRepository regionRepository) {
        this.regionRepository = regionRepository;
    }

    public RegionResolution resolve(String question, AiUserProfile profile) {
        String normalizedQuestion = normalize(question);
        if (isBareGwangju(normalizedQuestion)) {
            Optional<Region> gwangjuSubregion = resolveGwangjuSubregion(question);
            if (gwangjuSubregion.isPresent()) {
                return RegionResolution.resolved(gwangjuSubregion.get());
            }
            return RegionResolution.ambiguous(List.of(
                    GWANGJU_METROPOLITAN_CITY, "경기도 광주시"));
        }

        for (Map.Entry<String, String> alias : REGION_FULL_NAME_ALIASES.entrySet()) {
            if (normalizedQuestion.contains(alias.getKey())) {
                Optional<Region> region = regionRepository.findByFullName(alias.getValue());
                if (region.isPresent()) {
                    return RegionResolution.resolved(region.get());
                }
            }
        }

        Optional<Region> fullNameRegion = regionRepository.findMentionedInQuestion(
                        normalizeSpacing(question), PageRequest.of(0, 1))
                .stream().findFirst();
        if (fullNameRegion.isPresent()) {
            return RegionResolution.resolved(fullNameRegion.get());
        }

        String regionLevel1 = resolveRegionLevel1(normalizedQuestion).orElse(null);
        Matcher matcher = REGION_LEVEL_2_PATTERN.matcher(normalizeSpacing(question));
        while (matcher.find()) {
            RegionResolution resolution = selectCandidate(
                    regionRepository.findAllByRegionLevel2OrderByIdAsc(matcher.group(1)),
                    regionLevel1,
                    profile.regionLevel1()
            );
            if (!resolution.isNotFound()) {
                return resolution;
            }
        }

        RegionResolution bareResolution = selectCandidate(
                findBareRegionCandidates(question), regionLevel1, profile.regionLevel1());
        if (!bareResolution.isNotFound()) {
            return bareResolution;
        }
        return regionLevel1 == null
                ? RegionResolution.notFound()
                : RegionResolution.resolvedRegionLevel1(regionLevel1);
    }

    public boolean isRegionOnlyQuestion(
            String question,
            RegionResolution resolution
    ) {
        if (resolution == null || !resolution.isResolved()) {
            return false;
        }
        String remainder = normalize(question);
        for (String mention : regionMentions(resolution)) {
            remainder = remainder.replace(normalize(mention), "");
        }
        return REGION_ONLY_REMAINDER_PATTERN.matcher(remainder).matches();
    }

    public String replaceRegion(
            String question,
            RegionResolution previousResolution,
            RegionResolution currentResolution
    ) {
        String replacement = displayName(currentResolution);
        if (question == null || question.isBlank() || replacement == null) {
            return question;
        }
        if (previousResolution != null && previousResolution.isResolved()) {
            for (String mention : regionMentions(previousResolution)) {
                Matcher matcher = Pattern.compile(Pattern.quote(mention))
                        .matcher(question);
                if (matcher.find()) {
                    return matcher.replaceFirst(Matcher.quoteReplacement(replacement));
                }
            }
        }
        return replacement + " " + question.trim();
    }

    private List<String> regionMentions(RegionResolution resolution) {
        Set<String> mentions = new HashSet<>();
        if (resolution.region() != null) {
            Region region = resolution.region();
            mentions.add(region.getFullName());
            mentions.add(region.getRegionLevel1());
            mentions.add(region.getRegionLevel2());
            mentions.add(removeAdministrativeSuffix(region.getRegionLevel2()));
        } else if (resolution.regionLevel1() != null) {
            mentions.add(resolution.regionLevel1());
        }
        REGION_LEVEL_1_ALIASES.forEach((alias, fullName) -> {
            if (fullName.equals(resolution.regionLevel1())) {
                mentions.add(alias);
            }
        });
        return mentions.stream()
                .filter(mention -> mention != null && !mention.isBlank())
                .sorted((left, right) -> Integer.compare(right.length(), left.length()))
                .toList();
    }

    private String removeAdministrativeSuffix(String regionLevel2) {
        return regionLevel2 == null
                ? null
                : regionLevel2.replaceFirst("(시|군|구)$", "");
    }

    private String displayName(RegionResolution resolution) {
        if (resolution == null || !resolution.isResolved()) {
            return null;
        }
        return resolution.region() == null
                ? resolution.regionLevel1()
                : resolution.region().getFullName();
    }

    private Optional<String> resolveRegionLevel1(String normalizedQuestion) {
        return REGION_LEVEL_1_ALIASES.entrySet().stream()
                .filter(entry -> normalizedQuestion.contains(normalize(entry.getKey())))
                .map(Map.Entry::getValue)
                .filter(regionLevel1 -> regionRepository
                        .findFirstByRegionLevel1OrderByIdAsc(regionLevel1)
                        .isPresent())
                .findFirst();
    }

    private RegionResolution selectCandidate(
            List<Region> candidates,
            String explicitRegionLevel1,
            String profileRegionLevel1
    ) {
        if (explicitRegionLevel1 != null) {
            candidates = candidates.stream()
                    .filter(region -> explicitRegionLevel1.equals(region.getRegionLevel1()))
                    .toList();
        }
        Optional<Region> profileCandidate = candidates.stream()
                .filter(region -> region.getRegionLevel1().equals(profileRegionLevel1))
                .findFirst();
        if (profileCandidate.isPresent()) {
            return RegionResolution.resolved(profileCandidate.get());
        }
        if (candidates.size() == 1) {
            return RegionResolution.resolved(candidates.getFirst());
        }
        if (candidates.size() > 1) {
            return RegionResolution.ambiguous(candidates.stream()
                    .map(Region::getFullName).distinct().sorted().toList());
        }
        return RegionResolution.notFound();
    }

    private List<Region> findBareRegionCandidates(String question) {
        Matcher matcher = KOREAN_WORD_PATTERN.matcher(normalizeSpacing(question));
        Set<String> names = new HashSet<>();
        while (matcher.find()) {
            String word = matcher.group().replaceFirst("(에서|으로|에|의)$", "");
            if (word.endsWith("시") || word.endsWith("군") || word.endsWith("구")) {
                continue;
            }
            names.add(word + "시");
            names.add(word + "군");
            names.add(word + "구");
        }
        return names.isEmpty()
                ? List.of()
                : regionRepository.findAllByRegionLevel2InOrderByIdAsc(
                        names.stream().sorted().toList());
    }

    private boolean isBareGwangju(String normalizedQuestion) {
        return normalizedQuestion.contains("광주")
                && !normalizedQuestion.contains("광주광역시")
                && !normalizedQuestion.contains("경기도광주")
                && !normalizedQuestion.contains("경기광주")
                && !normalizedQuestion.contains("광주시");
    }

    private Optional<Region> resolveGwangjuSubregion(String question) {
        Matcher matcher = REGION_LEVEL_2_PATTERN.matcher(normalizeSpacing(question));
        while (matcher.find()) {
            Optional<Region> candidate = regionRepository
                    .findAllByRegionLevel2OrderByIdAsc(matcher.group(1))
                    .stream()
                    .filter(region -> GWANGJU_METROPOLITAN_CITY.equals(
                            region.getRegionLevel1()))
                    .findFirst();
            if (candidate.isPresent()) {
                return candidate;
            }
        }
        return Optional.empty();
    }

    private String normalize(String value) {
        return normalizeSpacing(value).replaceAll("\\s+", "");
    }

    private String normalizeSpacing(String value) {
        return value == null
                ? ""
                : value.trim().replaceAll("[.!?]+$", "").replaceAll("\\s+", " ");
    }

    public record RegionResolution(
            Status status,
            Region region,
            String regionLevel1,
            List<String> candidates
    ) {
        public enum Status { RESOLVED, AMBIGUOUS, NOT_FOUND }

        static RegionResolution resolved(Region region) {
            return new RegionResolution(Status.RESOLVED, region,
                    region.getRegionLevel1(), List.of());
        }

        static RegionResolution resolvedRegionLevel1(String regionLevel1) {
            return new RegionResolution(
                    Status.RESOLVED, null, regionLevel1, List.of());
        }

        static RegionResolution ambiguous(List<String> candidates) {
            return new RegionResolution(
                    Status.AMBIGUOUS, null, null, List.copyOf(candidates));
        }

        static RegionResolution notFound() {
            return new RegionResolution(Status.NOT_FOUND, null, null, List.of());
        }

        public boolean isResolved() {
            return status == Status.RESOLVED;
        }

        public boolean isAmbiguous() {
            return status == Status.AMBIGUOUS;
        }

        public boolean isNotFound() {
            return status == Status.NOT_FOUND;
        }

        public AiUserProfile applyTo(AiUserProfile profile) {
            if (region != null) {
                return profile.withRegion(region.getFullName(),
                        region.getRegionLevel1(), region.getRegionLevel2());
            }
            return profile.withRegion(regionLevel1, regionLevel1, "");
        }

        public String ambiguityMessage() {
            return "확인할 지역이 여러 곳입니다. "
                    + candidates.stream().collect(Collectors.joining(", "))
                    + " 중 어느 지역을 말씀하시나요?";
        }
    }
}
