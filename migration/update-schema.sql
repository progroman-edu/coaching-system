ALTER TABLE trainees
    ADD current_rating_mode VARCHAR(10) NULL;

ALTER TABLE trainees
    ADD highest_blitz_rating INT NULL;

ALTER TABLE trainees
    ADD highest_bullet_rating INT NULL;

ALTER TABLE trainees
    ADD highest_rapid_rating INT NULL;

ALTER TABLE trainees
DROP
COLUMN highest_rating;

ALTER TABLE matches
DROP
COLUMN format;

ALTER TABLE matches
DROP
COLUMN status;

ALTER TABLE matches
    ADD format VARCHAR(30) NOT NULL;

ALTER TABLE match_participants
DROP
COLUMN piece_color;

ALTER TABLE match_participants
    ADD piece_color VARCHAR(20) NOT NULL;

ALTER TABLE match_results
DROP
COLUMN result_type;

ALTER TABLE match_results
    ADD result_type VARCHAR(20) NOT NULL;

ALTER TABLE matches
    ADD status VARCHAR(30) NOT NULL;

ALTER TABLE notifications
DROP
COLUMN type;

ALTER TABLE notifications
    ADD type VARCHAR(40) NOT NULL;