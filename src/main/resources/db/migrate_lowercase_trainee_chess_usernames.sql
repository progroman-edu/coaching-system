-- Run this on existing databases to lowercase all stored trainee Chess.com usernames.
-- Safe to re-run.
UPDATE trainees
SET chess_username = LOWER(chess_username)
WHERE chess_username IS NOT NULL;
