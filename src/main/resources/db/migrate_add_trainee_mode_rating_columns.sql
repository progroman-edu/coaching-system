-- Run this on existing databases created before mode-specific rating columns were added.
-- Safe to re-run: each statement executes only when the target column is missing.
SET @db_name = DATABASE();

SET @sql = (
    SELECT IF(
        EXISTS (
            SELECT 1
            FROM information_schema.COLUMNS
            WHERE TABLE_SCHEMA = @db_name
              AND TABLE_NAME = 'trainees'
              AND COLUMN_NAME = 'highest_rapid_rating'
        ),
        'SELECT ''highest_rapid_rating already exists''',
        'ALTER TABLE trainees ADD COLUMN highest_rapid_rating INT NULL'
    )
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = (
    SELECT IF(
        EXISTS (
            SELECT 1
            FROM information_schema.COLUMNS
            WHERE TABLE_SCHEMA = @db_name
              AND TABLE_NAME = 'trainees'
              AND COLUMN_NAME = 'highest_blitz_rating'
        ),
        'SELECT ''highest_blitz_rating already exists''',
        'ALTER TABLE trainees ADD COLUMN highest_blitz_rating INT NULL'
    )
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = (
    SELECT IF(
        EXISTS (
            SELECT 1
            FROM information_schema.COLUMNS
            WHERE TABLE_SCHEMA = @db_name
              AND TABLE_NAME = 'trainees'
              AND COLUMN_NAME = 'highest_bullet_rating'
        ),
        'SELECT ''highest_bullet_rating already exists''',
        'ALTER TABLE trainees ADD COLUMN highest_bullet_rating INT NULL'
    )
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
