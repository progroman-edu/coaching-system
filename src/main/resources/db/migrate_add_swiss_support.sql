-- Migration: Add Swiss Pairing System Support
-- Purpose: Enable proper Swiss tournament implementation with round tracking and tiebreaker rematches

-- Add swiss_round_number to match_participants to track which round each pairing belongs to
ALTER TABLE match_participants ADD COLUMN swiss_round_number INT NULL DEFAULT NULL;

-- Add index for efficient Swiss round queries
CREATE INDEX IF NOT EXISTS idx_match_participant_swiss_round 
ON match_participants(match_id, swiss_round_number);

-- Create rematch_round table to track tiebreaker rematches (Buchholz tiebreaker tertiary condition)
CREATE TABLE IF NOT EXISTS rematch_round (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    trainee_id_1 BIGINT NOT NULL,
    trainee_id_2 BIGINT NOT NULL,
    original_match_id BIGINT NOT NULL,
    rematch_match_id BIGINT NULL,
    reason VARCHAR(255) NOT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'pending',
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_rematch_trainee_1 FOREIGN KEY (trainee_id_1) REFERENCES trainees(id),
    CONSTRAINT fk_rematch_trainee_2 FOREIGN KEY (trainee_id_2) REFERENCES trainees(id),
    CONSTRAINT fk_rematch_original_match FOREIGN KEY (original_match_id) REFERENCES matches(id),
    CONSTRAINT fk_rematch_rematch_match FOREIGN KEY (rematch_match_id) REFERENCES matches(id),
    CONSTRAINT uk_rematch_trainee_pair UNIQUE (trainee_id_1, trainee_id_2, original_match_id)
);

-- Add indexes for rematch queries
CREATE INDEX IF NOT EXISTS idx_rematch_status ON rematch_round(status);
CREATE INDEX IF NOT EXISTS idx_rematch_original_match ON rematch_round(original_match_id);
CREATE INDEX IF NOT EXISTS idx_rematch_rematch_match ON rematch_round(rematch_match_id);

-- Documentation:
-- swiss_round_number: Used to identify which Swiss round a match belongs to (1, 2, 3, etc.)
--                     Allows querying opponent history per round for pairing generation
-- rematch_round: Stores tiebreaker rematches created when two players have:
--                - Same final score
--                - Previously played each other
--                - Result was a draw
--                Status: 'pending' (not yet scheduled), 'scheduled', 'completed'
