-- Migration: Add Performance Indexes for Chess Coach System
-- Purpose: Improve query performance for frequently accessed columns

-- Index on MatchResult for trainee history queries
CREATE INDEX IF NOT EXISTS idx_match_result_trainee_played_at 
ON match_results(white_trainee_id, black_trainee_id, played_at);

-- Index on Attendance for date range queries
CREATE INDEX IF NOT EXISTS idx_attendance_trainee_date 
ON attendance(trainee_id, attendance_date);

-- Index on RatingsHistory for rating trend queries
CREATE INDEX IF NOT EXISTS idx_ratings_history_trainee_created_at 
ON ratings_history(trainee_id, created_at);

-- Index on Match for status and format queries
CREATE INDEX IF NOT EXISTS idx_match_status_format 
ON match(status, format);

-- Index on MatchParticipant for board queries
CREATE INDEX IF NOT EXISTS idx_match_participant_match_board 
ON match_participants(match_id, board_number);

-- Index on Trainee for coach and name queries
CREATE INDEX IF NOT EXISTS idx_trainee_coach_name 
ON trainees(coach_id, name);
