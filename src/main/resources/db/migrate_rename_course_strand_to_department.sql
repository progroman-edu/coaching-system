-- Run this on existing databases where trainees.course_strand must be renamed to department.
-- Safe to re-run.
SET @db_name = DATABASE();

SET @has_department = (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = @db_name
      AND TABLE_NAME = 'trainees'
      AND COLUMN_NAME = 'department'
);

SET @has_course_strand = (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = @db_name
      AND TABLE_NAME = 'trainees'
      AND COLUMN_NAME = 'course_strand'
);

SET @sql = (
    SELECT CASE
        WHEN @has_department = 0 AND @has_course_strand = 1 THEN
            'ALTER TABLE trainees CHANGE COLUMN course_strand department VARCHAR(100) NOT NULL'
        WHEN @has_department = 1 AND @has_course_strand = 1 THEN
            'UPDATE trainees SET department = COALESCE(NULLIF(department, ''''), course_strand) WHERE department IS NULL OR department = '''''
        WHEN @has_department = 1 THEN
            'SELECT ''department already exists'''
        ELSE
            'SELECT ''Neither department nor course_strand exists'''
    END
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @has_department = (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = @db_name
      AND TABLE_NAME = 'trainees'
      AND COLUMN_NAME = 'department'
);

SET @has_course_strand = (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = @db_name
      AND TABLE_NAME = 'trainees'
      AND COLUMN_NAME = 'course_strand'
);

SET @sql = (
    SELECT CASE
        WHEN @has_department = 1 AND @has_course_strand = 1 THEN
            'ALTER TABLE trainees DROP COLUMN course_strand'
        ELSE
            'SELECT ''No legacy course_strand column to drop'''
    END
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @has_department_index = (
    SELECT COUNT(*)
    FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = @db_name
      AND TABLE_NAME = 'trainees'
      AND INDEX_NAME = 'idx_trainee_department'
);

SET @has_course_strand_index = (
    SELECT COUNT(*)
    FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = @db_name
      AND TABLE_NAME = 'trainees'
      AND INDEX_NAME = 'idx_trainee_course_strand'
);

SET @sql = (
    SELECT CASE
        WHEN @has_department_index = 0 AND @has_course_strand_index > 0 THEN
            'ALTER TABLE trainees RENAME INDEX idx_trainee_course_strand TO idx_trainee_department'
        WHEN @has_department_index = 0 THEN
            'CREATE INDEX idx_trainee_department ON trainees(department)'
        ELSE
            'SELECT ''department index already exists'''
    END
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
