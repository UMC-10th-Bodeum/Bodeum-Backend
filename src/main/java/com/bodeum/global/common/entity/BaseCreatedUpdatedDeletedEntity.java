package com.bodeum.global.common.entity;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;

import java.time.Instant;
import lombok.Getter;

@Getter
@MappedSuperclass
public abstract class BaseCreatedUpdatedDeletedEntity extends BaseCreatedUpdatedEntity {

    @Column(name = "deleted_at")
    private Instant deletedAt;

    public void delete() {
        this.deletedAt = Instant.now();
    }

    public void restore() {
        this.deletedAt = null;
    }

    public boolean isDeleted() {
        return deletedAt != null;
    }
}