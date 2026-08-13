-- GuardianPoint는 프로필 없이는 의미가 없으므로 기존 고아 이력과 포인트를 먼저 정리한다.
DELETE history
FROM guardian_point_history history
JOIN guardian_point point ON point.guardian_point_id = history.guardian_point_id
LEFT JOIN guardian_profiles profile ON profile.id = point.guardian_profile_id
WHERE profile.id IS NULL;

DELETE point
FROM guardian_point point
LEFT JOIN guardian_profiles profile ON profile.id = point.guardian_profile_id
WHERE profile.id IS NULL;

-- News의 지역은 선택 값이므로 잘못된 참조만 NULL로 정리하고 소식 자체는 보존한다.
UPDATE news news_item
LEFT JOIN regions region ON region.id = news_item.region_id
SET news_item.region_id = NULL
WHERE news_item.region_id IS NOT NULL
  AND region.id IS NULL;

SET @guardian_point_fk_exists = (
    SELECT COUNT(*)
    FROM information_schema.KEY_COLUMN_USAGE
    WHERE CONSTRAINT_SCHEMA = DATABASE()
      AND TABLE_NAME = 'guardian_point'
      AND COLUMN_NAME = 'guardian_profile_id'
      AND REFERENCED_TABLE_NAME = 'guardian_profiles'
      AND REFERENCED_COLUMN_NAME = 'id'
);

SET @guardian_point_fk_sql = IF(
    @guardian_point_fk_exists = 0,
    'ALTER TABLE guardian_point
        ADD CONSTRAINT fk_guardian_point_guardian_profile
        FOREIGN KEY (guardian_profile_id) REFERENCES guardian_profiles (id)
        ON UPDATE RESTRICT ON DELETE RESTRICT',
    'SELECT 1'
);

PREPARE guardian_point_fk_statement FROM @guardian_point_fk_sql;
EXECUTE guardian_point_fk_statement;
DEALLOCATE PREPARE guardian_point_fk_statement;

SET @news_region_fk_exists = (
    SELECT COUNT(*)
    FROM information_schema.KEY_COLUMN_USAGE
    WHERE CONSTRAINT_SCHEMA = DATABASE()
      AND TABLE_NAME = 'news'
      AND COLUMN_NAME = 'region_id'
      AND REFERENCED_TABLE_NAME = 'regions'
      AND REFERENCED_COLUMN_NAME = 'id'
);

SET @news_region_fk_sql = IF(
    @news_region_fk_exists = 0,
    'ALTER TABLE news
        ADD CONSTRAINT fk_news_region
        FOREIGN KEY (region_id) REFERENCES regions (id)
        ON UPDATE RESTRICT ON DELETE RESTRICT',
    'SELECT 1'
);

PREPARE news_region_fk_statement FROM @news_region_fk_sql;
EXECUTE news_region_fk_statement;
DEALLOCATE PREPARE news_region_fk_statement;
