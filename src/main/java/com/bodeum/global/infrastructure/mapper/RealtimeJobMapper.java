package com.bodeum.global.infrastructure.mapper;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.bodeum.domain.info.entity.InfoCategory;
import com.bodeum.domain.info.entity.InfoItem;
import com.bodeum.global.infrastructure.constant.OpenApiCategory;
import com.bodeum.global.infrastructure.constant.OpenApiSourceSpec;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

// 구인 정보 (REALTIME_JOB 계열 3개) ──► RealtimeJobMapper
//     └─ 회사명, 모집 요강, 자격 요건 등 취업 특화 데이터 추출 필요

@Slf4j
@Component
public class RealtimeJobMapper implements OpenApiMapper {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public boolean supports(OpenApiSourceSpec sourceSpec) {
        // REALTIME_JOB 소분류에 속하는 3개 API 처리를 전담
        return sourceSpec.getCategory() == OpenApiCategory.REALTIME_JOB;
    }

    @Override
    public InfoItem mapToEntity(String rawData, InfoCategory category, OpenApiSourceSpec sourceSpec) {
        try {
            JsonNode root = objectMapper.readTree(rawData);

            // 플랫폼별 구조 분기 (ODCLOUD와 DATAGO의 JSON 구조 차이 대응)
            JsonNode itemsNode = sourceSpec.getUrlType().equals("ODCLOUD")
                    ? root.path("data")
                    : root.path("response").path("body").path("items").path("item");

            JsonNode item = itemsNode.isArray() ? itemsNode.get(0) : itemsNode;

            return parseItemNode(item, category, sourceSpec);

        } catch (Exception e) {
            log.error("구인 정보 데이터 -> InfoItem 정규화 실패: {}", sourceSpec.name(), e);
            throw new IllegalArgumentException("구인 API 데이터 파싱 에러");
        }
    }

    @Override
    public List<InfoItem> mapToEntityList(String rawData, InfoCategory category, OpenApiSourceSpec sourceSpec) {
        List<InfoItem> resultList = new ArrayList<>();
        try {
            JsonNode root = objectMapper.readTree(rawData);

            // 플랫폼별 구조 분기 (ODCLOUD와 DATAGO의 JSON 구조 차이 대응)
            JsonNode itemsNode = sourceSpec.getUrlType().equals("ODCLOUD")
                    ? root.path("data")
                    : root.path("response").path("body").path("items").path("item");

            if (itemsNode.isArray()) {
                for (JsonNode item : itemsNode) {
                    try {
                        resultList.add(parseItemNode(item, category, sourceSpec));
                    } catch (Exception e) {
                        log.warn("구인 정보 개별 항목 파싱 실패 패스: {}", sourceSpec.name());
                    }
                }
            } else if (!itemsNode.isMissingNode()) {
                resultList.add(parseItemNode(itemsNode, category, sourceSpec));
            }
        } catch (Exception e) {
            log.error("구인 정보 리스트 정규화 실패: {}", sourceSpec.name(), e);
        }
        return resultList;
    }

    private InfoItem parseItemNode(JsonNode item, InfoCategory category, OpenApiSourceSpec sourceSpec) {
        String jobId = item.path("jobId").asText(); // 공단 구인공고 ID
        String companyName = item.path("compNm").asText(); // 회사명
        String jobTitle = item.path("title").asText(); // 공고제목
        String address = item.path("workAddr").asText(); // 근무지 주소
        String phone = item.path("contTel").asText(); // 담당자 연락처

        // 회사명 길이 제한 (최대 80자)
        if (companyName != null && companyName.length() > 80) {
            companyName = companyName.substring(0, 77) + "...";
        }

        String[] addrTokens = address.split(" ");
        String sido = addrTokens.length > 0 ? addrTokens[0] : "미분류";
        String sigungu = addrTokens.length > 1 ? addrTokens[1] : "미분류";

        // 구인공고명과 상세 자격요건을 introduction 필드에 정합
        String introduction = String.format("공고명: %s\n상세 요강: %s", jobTitle, item.path("reqSpec").asText());
        if (introduction.length() > 250) {
            introduction = introduction.substring(0, 247) + "...";
        }

        String externalId = jobId.isEmpty() ? (sourceSpec.name() + "_" + companyName).replaceAll(" ", "") : jobId;
        if (externalId.length() > 100) {
            externalId = externalId.substring(0, 100);
        }

        String homepageUrl = item.path("homepage").asText();
        if (homepageUrl.isBlank()) {
            homepageUrl = null;
        }

        return InfoItem.builder()
                .externalId(externalId)
                .infoCategory(category)
                .name(companyName)
                .introduction(introduction)
                .address(address)
                .sido(sido)
                .sigungu(sigungu)
                .phone(phone)
                .homepageUrl(homepageUrl)
                .syncedAt(LocalDateTime.now())
                .build();
    }
}