-- Migration: Add soft delete support to trainees table
-- This allows deletion of trainees without losing their historical data (attendance, matches, ratings)

ALTER TABLE trainees ADD COLUMN deleted_at TIMESTAMP NULL DEFAULT NULL;

-- Add index on deleted_at for filtering active trainees
CREATE INDEX idx_trainee_deleted_at ON trainees(deleted_at);

-- Update existing queries to filter out soft-deleted trainees
-- This is handled at the JPA level with @Where clause and JPQL filters
