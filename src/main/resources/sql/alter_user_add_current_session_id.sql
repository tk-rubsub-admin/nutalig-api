ALTER TABLE user
    ADD COLUMN current_session_id VARCHAR(255) NULL AFTER verified_date;
