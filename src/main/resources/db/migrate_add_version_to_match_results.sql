-- Migration: Add optimistic locking version column to match_results
-- This enables optimistic locking to prevent race conditions when recording match results

ALTER TABLE match_results ADD COLUMN version BIGINT NOT NULL DEFAULT 0;

-- Add indexes for performance on key lookups
CREATE INDEX idx_match_result_version ON match_results(version);

-- Update initial versions for existing records (set to 0 for all)
UPDATE match_results SET version = 0 WHERE version IS NULL;
