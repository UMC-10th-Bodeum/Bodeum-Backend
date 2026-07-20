package com.bodeum.global.infrastructure.mapper;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.bodeum.domain.info.entity.InfoCategory;
import com.bodeum.domain.info.entity.InfoItem;
import com.bodeum.global.infrastructure.constant.OpenApiCategory;
import com.bodeum.global.infrastructure.constant.OpenApiSourceSpec;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
public class CommonJsonMapper implements OpenApiMapper {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final XmlMapper xmlMapper = new XmlMapper();

    @Override
    public boolean supports(OpenApiSourceSpec sourceSpec) {
        // 다른 전용 매퍼가 선점한 카테고리가 아닌 모든 공통 API 처리
        return sourceSpec.getCategory() != OpenApiCategory.EMERGENCY_CLINIC
                && sourceSpec.getCategory() != OpenApiCategory.REALTIME_JOB;
    }

    @Override
    public InfoItem mapToEntity(String rawData, InfoCategory category, OpenApiSourceSpec sourceSpec) {
        if (rawData == null || rawData.isBlank()) {
            log.warn("공통 매퍼 단건 정규화 건너뜀 - JSON이 아니거나 XML/HTML 에러 응답임. API: {}, 데이터 일부: {}",
                    sourceSpec.name(), rawData != null ? rawData.substring(0, Math.min(rawData.length(), 150)) : "null");
            return null;
        }

        try {
            JsonNode root = parseToTree(rawData);
            JsonNode itemsNode = extractItemNode(root, sourceSpec);
            JsonNode item = itemsNode.isArray() ? itemsNode.get(0) : itemsNode;

            return parseItemNode(item, category, sourceSpec);

        } catch (Exception e) {
            log.error("공통 매퍼 정규화 실패 - API: {}", sourceSpec.name(), e);
            throw new IllegalArgumentException("공통 API 데이터 파싱 에러 [" + sourceSpec.name() + "]");
        }
    }

    @Override
    public List<InfoItem> mapToEntityList(String rawData, InfoCategory category, OpenApiSourceSpec sourceSpec) {
        List<InfoItem> resultList = new ArrayList<>();

        if (rawData == null || rawData.isBlank()) {
            log.warn("공통 매퍼 리스트 정규화 건너뜀 - JSON이 아니거나 XML/HTML 에러 응답임. API: {}, 데이터 일부: {}",
                    sourceSpec.name(), rawData != null ? rawData.substring(0, Math.min(rawData.length(), 150)) : "null");
            return resultList;
        }

        try {
            JsonNode root = parseToTree(rawData);
            JsonNode itemsNode = extractItemNode(root, sourceSpec);

            if (itemsNode != null && itemsNode.isArray()) {
                for (JsonNode item : itemsNode) {
                    try {
                        InfoItem entity = parseItemNode(item, category, sourceSpec);
                        if (entity != null) {
                            resultList.add(entity);
                        }
                    } catch (Exception e) {
                        log.warn("공통 매퍼 루프 개별 파싱 패스 - API: {}", sourceSpec.name());
                    }
                }
            } else if (itemsNode != null && !itemsNode.isMissingNode()) {
                InfoItem entity = parseItemNode(itemsNode, category, sourceSpec);
                if (entity != null) {
                    resultList.add(entity);
                }
            }
        } catch (Exception e) {
            log.error("공통 매퍼 리스트 정규화 실패 - API: {}", sourceSpec.name(), e);
        }
        return resultList;
    }

    private JsonNode parseToTree(String rawData) throws Exception {
        String trimmed = rawData.trim();
        if (trimmed.startsWith("<")) {
            return xmlMapper.readTree(trimmed);
        }
        return objectMapper.readTree(trimmed);
    }

    private InfoItem parseItemNode(JsonNode item, InfoCategory category, OpenApiSourceSpec sourceSpec) {
        if (item == null || item.isMissingNode()) {
            throw new IllegalArgumentException("유효한 JSON item 노드를 찾을 수 없습니다.");
        }

        // 💡 [디버깅 로그] null 값이 나오는 API의 실제 키 구조를 확인하기 위해 콘솔에 출력
        log.info("[JSON 구조 확인 - API: {}] {}", sourceSpec.name(), item.toString());

        // 1. 시설/기관명 추출 키 후보군
        String name = findFirstValidField(item, List.of(
                "의료기관명", "요양기관명", "의원명", "yadmNm", "시설명", "제목", "servNm", "wantedTitle", "facltNm", "instNm", "orgNm", "schNm", "title", "기관명", "학교명",
                "dutyName", "hospNm", "medcareInstNm", "fcltyNm", "centerNm", "name", "사업장명",
                "약국명", "병원명", "welfArName"
        ));

        // 2. 주소 추출 및 시도/시군구 분리 키 후보군
        String address = findFirstValidField(item, List.of(
                "소재지도로명주소", "소재지지번주소", "상세주소", "시설 주소", "시설주소", "refineRoadnmAddr", "addr", "address", "roadAddr", "주소",
                "dutyAddr", "dutyLotmuAddr", "refineLotnoAddr", "locAddr", "locplcDetailAddr",
                "ADDR", "ROAD_ADDR", "locplcAddr", "rdnmadr", "lnmadr"
        ));

        if (name == null && address == null) {
            String rawCheck = item.toString();
            if (rawCheck.contains("null") && !rawCheck.matches(".*\"[^\"]+\":\"[^\"]+\".*")) {
                log.warn("유효 데이터가 없는 빈 항목이므로 스킵합니다 - API: {}", sourceSpec.name());
                return null;
            }
        }

        String finalName = (name != null && !name.isBlank()) ? name : "이름 없는 기관";
        if (finalName.length() > 80) {
            finalName = finalName.substring(0, 77) + "...";
        }

        String sido = "미분류";
        String sigungu = "미분류";

        if (address != null && !address.isBlank()) {
            String cleanAddr = address.replaceAll("\\s+", " ").trim();
            String[] addrTokens = cleanAddr.split(" ");
            sido = addrTokens.length > 0 ? addrTokens[0] : "미분류";
            sigungu = addrTokens.length > 1 ? addrTokens[1] : "미분류";
        } else {
            address = "주소 정보 없음";
        }

        // 3. 전화번호 추출 키 후보군
        String phone = findFirstValidField(item, List.of(
                "대표전화번호", "전화번호", "facltTelno", "telNo", "phone", "tel", "연락처",
                "dutyTel1", "telno", "contTel", "TELNO", "contact"
        ));

        // 4. 홈페이지 URL 추출 키 후보군
        String homepageUrl = findFirstValidField(item, List.of(
                "누리집", "homepage", "hpUrl", "url", "홈페이지", "hmpgUrl", "siteUrl", "HMPG_URL", "servDtlLink"
        ));

        // 5. 소개글 / 상세설명 추출 키 후보군
        String introduction = findFirstValidField(item, List.of(
                "introduction", "servDtl", "상세내용", "개요", "dtlInfo", "servSmmry", "dtilInfo", "servDgree"
        ));

        if (introduction == null) {
            String researcher = findFirstValidField(item, List.of("연구자"));
            String publishYear = findFirstValidField(item, List.of("발간년도"));
            String issueNo = findFirstValidField(item, List.of("제호"));

            if (researcher != null || publishYear != null) {
                StringBuilder sb = new StringBuilder();
                sb.append("[연구 자료] ");
                if (researcher != null) sb.append("연구자: ").append(researcher).append(" | ");
                if (publishYear != null) sb.append("발간년도: ").append(publishYear).append("년 | ");
                if (issueNo != null) sb.append("출처: ").append(issueNo);
                introduction = sb.toString();
            } else {
                introduction = String.format("[%s] 수집 데이터 요약: %s", category.getSubCategoryKo(), finalName);
            }
        }

        if (introduction != null && introduction.length() > 250) {
            introduction = introduction.substring(0, 247) + "...";
        }

        // 6. 외부 고유 ID 추출 키 후보군
        String rawId = findFirstValidField(item, List.of(
                "ykiho", "servId", "wantedId", "연번", "id", "seq", "key", "sn", "facltNo", "일련번호", "고유번호", "hpid"
        ));

        String externalId;
        if (rawId != null && !rawId.isBlank()) {
            externalId = sourceSpec.name() + "_" + rawId;
        } else {
            String safeShortName = finalName.length() > 20 ? finalName.substring(0, 20) : finalName;
            externalId = (sourceSpec.name() + "_" + safeShortName + "_" + Math.abs(address.hashCode())).replaceAll("\\s+", "");
        }

        if (externalId.length() > 100) {
            externalId = externalId.substring(0, 100);
        }

        return InfoItem.builder()
                .externalId(externalId)
                .infoCategory(category)
                .name(finalName)
                .introduction(introduction)
                .address(address)
                .sido(sido)
                .sigungu(sigungu)
                .phone(phone)
                .homepageUrl(homepageUrl)
                .syncedAt(LocalDateTime.now())
                .build();
    }

    /**
     * OpenApiSourceSpec의 urlType 규칙에 따라 JSON 루트 노드에서 데이터 배열/객체 노드를 찾습니다.
     */
    private JsonNode extractItemNode(JsonNode root, OpenApiSourceSpec sourceSpec) {
        String urlType = sourceSpec.getUrlType();

        if ("ODCLOUD".equals(urlType)) {
            return root.path("data");
        }

        if ("GG".equals(urlType)) {
            String key = sourceSpec.getApiKeyName();
            if (key != null && root.path(key).size() > 1) {
                return root.path(key).get(1).path("row");
            }
        }

        if ("DATAGO".equals(urlType)) {
            JsonNode wantedList = root.path("wantedList");
            if (!wantedList.isMissingNode()) {
                return wantedList.path("wantedList").isMissingNode() ? wantedList : wantedList.path("wantedList");
            }
            return root.path("response").path("body").path("items").path("item");
        }

        return root;
    }

    /**
     * 필드 후보군을 순회하며 JSON 노드에서 유효한 문자열 값을 반환합니다.
     */
    private String findFirstValidField(JsonNode item, List<String> fieldCandidates) {
        for (String candidate : fieldCandidates) {
            JsonNode node = item.path(candidate);
            if (!node.isMissingNode() && !node.isNull()) {
                String text = node.asText().trim();
                if (!text.isBlank() && !"null".equalsIgnoreCase(text) && !"undefined".equalsIgnoreCase(text)) {
                    return text;
                }
            }
        }
        return null;
    }
}