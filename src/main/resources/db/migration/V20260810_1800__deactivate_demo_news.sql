-- Remove user scraps first so saved-item counts cannot retain hidden demo news.
DELETE ns
FROM news_scrap ns
JOIN news n ON n.news_id = ns.news_id
WHERE n.news_source_id IS NULL
  AND n.news_id IN (1, 2, 3, 4, 5, 6, 7, 8, 9, 10);

-- These ten records predate public-data ingestion and are demo news only.
UPDATE news
SET is_active = FALSE,
    deleted_at = NOW()
WHERE news_source_id IS NULL
  AND news_id IN (1, 2, 3, 4, 5, 6, 7, 8, 9, 10)
  AND deleted_at IS NULL;
