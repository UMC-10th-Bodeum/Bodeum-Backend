UPDATE news
SET original_url = NULL
WHERE original_url IS NOT NULL
  AND (
      LOWER(original_url) REGEXP '^https?://([^.]+[.])?data[.]go[.]kr(/|$)'
      OR LOWER(original_url) REGEXP '^https?://([^.]+[.])?odcloud[.]kr(/|$)'
      OR LOWER(original_url) REGEXP '^https?://([^.]+[.])?gg[.]go[.]kr(/|$)'
  );
