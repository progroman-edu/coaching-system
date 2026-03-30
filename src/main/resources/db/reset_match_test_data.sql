-- Reset only match-related test data while preserving trainee master data.
-- Safe for MySQL Workbench SQL_SAFE_UPDATES mode.
-- Tables affected: ratings_history (linked rows only), match_results, match_participants, matches.

START TRANSACTION;

-- Remove rating history entries created from match results.
DELETE rh
FROM ratings_history rh
JOIN match_results mr ON rh.match_result_id = mr.id;

-- Remove match result rows.
DELETE FROM match_results WHERE id IS NOT NULL;

-- Remove match participant rows.
DELETE FROM match_participants WHERE id IS NOT NULL;

-- Remove match schedule rows.
DELETE FROM matches WHERE id IS NOT NULL;

-- Optional: reset identifiers for cleaner future test inserts.
ALTER TABLE match_results AUTO_INCREMENT = 1;
ALTER TABLE match_participants AUTO_INCREMENT = 1;
ALTER TABLE matches AUTO_INCREMENT = 1;

COMMIT;

-- Verification
SELECT COUNT(*) AS matches_count FROM matches;
SELECT COUNT(*) AS participants_count FROM match_participants;
SELECT COUNT(*) AS results_count FROM match_results;
SELECT COUNT(*) AS linked_rating_rows
FROM ratings_history
WHERE match_result_id IS NOT NULL;
