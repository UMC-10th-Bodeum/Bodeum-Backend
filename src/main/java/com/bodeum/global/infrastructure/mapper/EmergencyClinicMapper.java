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

// 응급 의료 (EMERGENCY_CLINIC 계열 8개) ──► EmergencyClinicMapper
//       └─ 실시간 병상, 중증 수용 정보 등 특수 데이터 추출 필요

@Slf4j
@Component
public class EmergencyClinicMapper implements OpenApiMapper {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public boolean supports(OpenApiSourceSpec sourceSpec) {
        // EMERGENCY_CLINIC 소분류에 속하는 8개 API 처리를 전담
        return sourceSpec.getCategory() == OpenApiCategory.EMERGENCY_CLINIC;
    }

    @Override
    public InfoItem mapToEntity(String rawData, InfoCategory category, OpenApiSourceSpec sourceSpec) {
        try {
            JsonNode root = objectMapper.readTree(rawData);
            JsonNode itemNode = root.path("response").path("body").path("items").path("item");
            JsonNode item = itemNode.isArray() ? itemNode.get(0) : itemNode;

            return parseItemNode(item, category, sourceSpec);

        } catch (Exception e) {
            log.error("응급의료 데이터 -> InfoItem 정규화 실패: {}", sourceSpec.name(), e);
            throw new IllegalArgumentException("응급의료 API 데이터 파싱 에러");
        }
    }

    @Override
    public List<InfoItem> mapToEntityList(String rawData, InfoCategory category, OpenApiSourceSpec sourceSpec) {
        List<InfoItem> resultList = new ArrayList<>();
        try {
            JsonNode root = objectMapper.readTree(rawData);
            JsonNode itemNode = root.path("response").path("body").path("items").path("item");

            if (itemNode.isArray()) {
                for (JsonNode item : itemNode) {
                    try {
                        resultList.add(parseItemNode(item, category, sourceSpec));
                    } catch (Exception e) {
                        log.warn("응급의료 개별 항목 파싱 실패 패스: {}", sourceSpec.name());
                    }
                }
            } else if (!itemNode.isMissingNode()) {
                resultList.add(parseItemNode(itemNode, category, sourceSpec));
            }
        } catch (Exception e) {
            log.error("응급의료 리스트 정규화 실패: {}", sourceSpec.name(), e);
        }
        return resultList;
    }

    private InfoItem parseItemNode(JsonNode item, InfoCategory category, OpenApiSourceSpec sourceSpec) {
        // 오픈 API가 내려주는 정보 추출
        String hpid = item.path("hpid").asText(); // 기관 고유 ID
        String name = item.path("dutyName").asText();
        String address = item.path("dutyAddr").asText();
        String phone = item.path("dutyTel1").asText();

        // 시도, 시군구 텍스트 파싱 처리 (예: "서울특별시 강남구..." -> "서울특별시", "강남구")
        String[] addrTokens = address.split(" ");
        String sido = addrTokens.length > 0 ? addrTokens[0] : "미분류";
        String sigungu = addrTokens.length > 1 ? addrTokens[1] : "미분류";

        // 실시간 가용 병상 등 상세 텍스트 정보는 소개글(introduction) 공간에 정규화하여 보존
        String introduction = String.format("[실시간 가용 병상 정보]\n%s", item.toString());

        // externalId 부재 시 sourceSpec 이름과 기관명 조합으로 유니크 키 생성
        String externalId = hpid.isEmpty() ? (sourceSpec.name() + "_" + name).replaceAll(" ", "") : hpid;

        return InfoItem.builder()
                .externalId(externalId)
                .infoCategory(category)
                .name(name)
                .introduction(introduction)
                .address(address)
                .sido(sido)
                .sigungu(sigungu)
                .phone(phone)
                .homepageUrl(null)
                .syncedAt(LocalDateTime.now())
                .build();
    }
}