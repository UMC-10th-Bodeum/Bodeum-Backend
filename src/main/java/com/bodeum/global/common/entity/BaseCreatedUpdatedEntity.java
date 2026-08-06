package com.bodeum.global.common.entity;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;

import java.time.Instant;
import lombok.Getter;
import org.springframework.data.annotation.LastModifiedDate;

@Getter
@MappedSuperclass
public abstract class BaseCreatedUpdatedEntity extends BaseCreatedEntity {

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    /**
     * 애그리거트 자식 엔티티만 바뀐 경우 updatedAt을 갱신하기 위해 호출한다.
     * @LastModifiedDate는 이 엔티티의 행이 dirty로 판정될 때만 동작하는데,
     * 자식 테이블만 변경되면 부모 행에는 UPDATE가 나가지 않아 갱신이 누락된다.
     * 여기서 필드를 직접 건드려 부모 행을 dirty로 만들면 감사 리스너가 정상 동작한다.
     */
    protected void markUpdated() {
        this.updatedAt = Instant.now();
    }
}