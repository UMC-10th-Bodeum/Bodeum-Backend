package com.bodeum.global.infrastructure.scheduler;

import com.bodeum.domain.info.entity.InfoCategory;
import com.bodeum.domain.info.entity.InfoItem;
import com.bodeum.domain.info.service.InfoItemBulkUpsertService;
import com.bodeum.domain.info.repository.InfoCategoryRepository;
import com.bodeum.global.config.OpenApiUrlBuilder;
import com.bodeum.global.infrastructure.constant.OpenApiSourceSpec;
import com.bodeum.global.infrastructure.mapper.OpenApiMapper;
import com.bodeum.global.infrastructure.mapper.OpenApiMapperFactory;
import com.bodeum.global.apiPayload.exception.ProjectException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

// 33개의 OpenApiSourceSpec을 돌면서 카테고리 매핑, WebClient로 비동기 호출해서 매퍼로 정규화된 리스트를 벌크 서비스로 넘겨 최종 저장!

@Slf4j
@Component
@RequiredArgsConstructor
public class OpenApiFetchScheduler {

    // WebClient 메모리 버퍼 용량을 10MB로 확장 (기본값 256KB -> DataBufferLimitException 방지)
    private final WebClient webClient = WebClient.builder()
            .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(10 * 1024 * 1024))
            .build();

    private final OpenApiUrlBuilder urlBuilder;
    private final OpenApiMapperFactory mapperFactory;
    private final InfoItemBulkUpsertService bulkUpsertService;
    private final InfoCategoryRepository infoCategoryRepository;

    @Scheduled(cron = "0 0 3 * * *") // 매일 새벽 3시 가동
    public void fetchAllOpenApiData() {
        log.info("=== 오픈 API 대량 동기화 배치 프로세스 시작 ===");

        for (OpenApiSourceSpec spec : OpenApiSourceSpec.values()) {
            List<InfoItem> chunkList = new ArrayList<>();
            try {
                InfoCategory category = infoCategoryRepository.findById(spec.getCategory().getId())
                        .orElseThrow(() -> new IllegalArgumentException("카테고리 누락 ID: " + spec.getCategory().getId()));

                // 1. 요청 건수를 100건에서 1000건으로 증량 (1페이지, 1000건)
                URI targetUri = urlBuilder.buildUri(spec, 1, 1000);

                // 2. WebClient 호출
                String rawData = webClient.get()
                        .uri(targetUri)
                        .retrieve()
                        .bodyToMono(String.class)
                        .block();

                if (rawData == null || rawData.isBlank()) continue;

                // 3. 단건 매핑이 아닌, 응답 내 전체 목록(List)을 정규화하도록 수정
                OpenApiMapper mapper = mapperFactory.getMapper(spec);
                List<InfoItem> infoItems = mapper.mapToEntityList(rawData, category, spec);

                if (infoItems != null && !infoItems.isEmpty()) {
                    chunkList.addAll(infoItems);
                }

                // 4. 리스트에 모인 대량 데이터 한방에 DB 벌크 적재
                if (!chunkList.isEmpty()) {
                    bulkUpsertService.bulkUpsert(chunkList);
                    log.info("[API 수집 성공] API명: {}, 수집된 건수: {}건", spec.name(), chunkList.size());
                }

            } catch (ProjectException e) {
                log.error("오픈 API 수집 중 비즈니스 에러 발생 - API명: {}, 에러코드: {}", spec.name(), e.getErrorCode().getMessage());
            } catch (Exception e) {
                log.error("오픈 API 수집 실패 - API명: {}, 원인: {}", spec.name(), e.getMessage());
            }
        }

        log.info("=== 오픈 API 대량 동기화 배치 프로세스 종료 ===");
    }
}