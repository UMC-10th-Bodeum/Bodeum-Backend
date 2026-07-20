package com.bodeum.domain.info.service;

import com.bodeum.domain.info.entity.InfoItem;
import com.bodeum.global.apiPayload.code.OpenApiErrorCode;
import com.bodeum.global.apiPayload.exception.ProjectException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.PreparedStatement;
import java.sql.Timestamp;
import java.util.List;

// 많은 양의 데이터를 하나씩 저장하면 서버가 죽음. db에 한 번에 넣는 역할.

@Slf4j
@Service
@RequiredArgsConstructor
public class InfoItemBulkUpsertService {

    private final JdbcTemplate jdbcTemplate;

    @Transactional
    public void bulkUpsert(List<InfoItem> items) {
        if (items == null || items.isEmpty()) {
            throw new ProjectException(OpenApiErrorCode.EMPTY_RESPONSE_DATA);
        }

        // MySQL 기준: external_id가 중복되면 지정한 필드들을 자동으로 UPDATE
        String sql = """
            INSERT INTO info_item (
                external_id, info_category_id, name, introduction, 
                address, sido, sigungu, phone, homepage_url, 
                view_count, scrap_count, review_count, synced_at, created_at, updated_at
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, 0, 0, 0, ?, NOW(), NOW())
            
            -- 매퍼가 생성한 고유 키(external_id)가 DB에 이미 있으면 핵심 정보만 Update하고, 없으면 신규 Insert를 단 한 번의 쿼리 묶음(Bulk)으로 끝.
            ON DUPLICATE KEY UPDATE
                
                name = VALUES(name),
                info_category_id = VALUES(info_category_id),
                introduction = VALUES(introduction),
                address = VALUES(address),
                sido = VALUES(sido),
                sigungu = VALUES(sigungu),
                phone = VALUES(phone),
                homepage_url = VALUES(homepage_url),
                synced_at = VALUES(synced_at),
                updated_at = NOW()
        """;

        try {
            jdbcTemplate.batchUpdate(sql, items, items.size(), (PreparedStatement ps, InfoItem item) -> {
                ps.setString(1, item.getExternalId());
                ps.setLong(2, item.getInfoCategory().getId());
                ps.setString(3, item.getName());
                ps.setString(4, item.getIntroduction());
                ps.setString(5, item.getAddress());
                ps.setString(6, item.getSido());
                ps.setString(7, item.getSigungu());
                ps.setString(8, item.getPhone());
                ps.setString(9, item.getHomepageUrl());
                ps.setTimestamp(10, Timestamp.valueOf(item.getSyncedAt()));
            });
            log.info("성공적으로 {}건의 InfoItem 데이터를 벌크 동기화(Upsert) 했습니다.", items.size());
        } catch (Exception e) {
            log.error("Bulk Upsert 중 DB 에러 발생: {}", e.getMessage());
            throw new ProjectException(OpenApiErrorCode.BULK_INSERT_FAILED);
        }
    }
}