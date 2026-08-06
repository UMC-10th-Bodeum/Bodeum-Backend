UPDATE `post`
SET is_question = CASE
    WHEN board_type = 'INFORMATION_QUESTION' THEN TRUE
    ELSE FALSE
END
WHERE is_question <> (board_type = 'INFORMATION_QUESTION');
