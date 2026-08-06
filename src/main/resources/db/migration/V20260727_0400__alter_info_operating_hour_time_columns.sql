-- info_operating_hour 테이블의 시간 컬럼 타입을 VARCHAR에서 TIME으로 변경
ALTER TABLE info_operating_hour
    MODIFY COLUMN open_time TIME,
    MODIFY COLUMN close_time TIME;