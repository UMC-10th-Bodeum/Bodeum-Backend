package com.bodeum.global.infrastructure.mapper;

import com.bodeum.global.infrastructure.constant.OpenApiSourceSpec;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class OpenApiMapperFactory {

    // 스프링이 빈으로 등록된 모든 OpenApiMapper 구현체를 자동으로 리스트에 주입.
    private final List<OpenApiMapper> mappers;

    // 현재 수집하려는 API 스펙을 지원하는 매퍼 구현체를 동적으로 찾아 반환
    public OpenApiMapper getMapper(OpenApiSourceSpec sourceSpec) {
        return mappers.stream()
                .filter(mapper -> mapper.supports(sourceSpec))
                .findFirst()
                .orElseThrow(() -> {
                    log.error("해당 오픈 API 스펙을 지원하는 매퍼가 존재하지 않습니다: {}", sourceSpec.name());
                    return new IllegalArgumentException("지원하지 않는 오픈 API 스펙입니다: " + sourceSpec.name());
                });
    }
}