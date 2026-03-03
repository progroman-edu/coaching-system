-- This file documents the database schema and indexes for reference.
CREATE TABLE IF NOT EXISTS coaches (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    full_name VARCHAR(120) NOT NULL,
    email VARCHAR(120) NOT NULL UNIQUE,
    phone VARCHAR(30),
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);

CREATE TABLE IF NOT EXISTS trainees (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    coach_id BIGINT NOT NULL,
    name VARCHAR(120) NOT NULL,
    age INT NOT NULL,
    address VARCHAR(255) NOT NULL,
    grade_level VARCHAR(50) NOT NULL,
    course_strand VARCHAR(100) NOT NULL,
    current_rating INT NOT NULL,
    highest_rating INT,
    ranking INT,
    photo_path VARCHAR(255),
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_trainee_coach FOREIGN KEY (coach_id) REFERENCES coaches (id)
);

CREATE INDEX idx_trainee_name ON trainees(name);
CREATE INDEX idx_trainee_current_rating ON trainees(current_rating);
CREATE INDEX idx_trainee_age ON trainees(age);
CREATE INDEX idx_trainee_course_strand ON trainees(course_strand);

CREATE TABLE IF NOT EXISTS attendance (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    trainee_id BIGINT NOT NULL,
    attendance_date DATE NOT NULL,
    is_present BOOLEAN NOT NULL,
    remarks VARCHAR(255),
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_attendance_trainee FOREIGN KEY (trainee_id) REFERENCES trainees (id),
    CONSTRAINT uk_attendance_trainee_date UNIQUE (trainee_id, attendance_date)
);

CREATE INDEX idx_attendance_date ON attendance(attendance_date);
CREATE INDEX idx_attendance_trainee ON attendance(trainee_id);

CREATE TABLE IF NOT EXISTS matches (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    format VARCHAR(30) NOT NULL,
    round_number INT NOT NULL,
    scheduled_date DATE NOT NULL,
    status VARCHAR(30) NOT NULL,
    notes VARCHAR(255),
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);

CREATE INDEX idx_match_scheduled_date ON matches(scheduled_date);
CREATE INDEX idx_match_format_round ON matches(format, round_number);
CREATE INDEX idx_match_status ON matches(status);

CREATE TABLE IF NOT EXISTS match_participants (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    match_id BIGINT NOT NULL,
    trainee_id BIGINT NOT NULL,
    piece_color VARCHAR(20) NOT NULL,
    board_number INT,
    start_rating INT,
    points_earned DOUBLE,
    is_bye BOOLEAN NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_match_participants_match FOREIGN KEY (match_id) REFERENCES matches (id),
    CONSTRAINT fk_match_participants_trainee FOREIGN KEY (trainee_id) REFERENCES trainees (id),
    CONSTRAINT uk_match_participant_match_trainee UNIQUE (match_id, trainee_id)
);

CREATE INDEX idx_match_participants_match ON match_participants(match_id);
CREATE INDEX idx_match_participants_trainee ON match_participants(trainee_id);

CREATE TABLE IF NOT EXISTS match_results (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    match_id BIGINT NOT NULL,
    white_trainee_id BIGINT NOT NULL,
    black_trainee_id BIGINT NOT NULL,
    white_score DOUBLE NOT NULL,
    black_score DOUBLE NOT NULL,
    result_type VARCHAR(20) NOT NULL,
    played_at TIMESTAMP NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_match_results_match FOREIGN KEY (match_id) REFERENCES matches (id),
    CONSTRAINT fk_match_results_white FOREIGN KEY (white_trainee_id) REFERENCES trainees (id),
    CONSTRAINT fk_match_results_black FOREIGN KEY (black_trainee_id) REFERENCES trainees (id),
    CONSTRAINT uk_match_result_match_players UNIQUE (match_id, white_trainee_id, black_trainee_id)
);

CREATE INDEX idx_match_results_match ON match_results(match_id);
CREATE INDEX idx_match_results_white ON match_results(white_trainee_id);
CREATE INDEX idx_match_results_black ON match_results(black_trainee_id);
CREATE INDEX idx_match_results_played_at ON match_results(played_at);

CREATE TABLE IF NOT EXISTS ratings_history (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    trainee_id BIGINT NOT NULL,
    match_result_id BIGINT,
    old_rating INT NOT NULL,
    new_rating INT NOT NULL,
    rating_change INT NOT NULL,
    notes VARCHAR(255),
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_ratings_history_trainee FOREIGN KEY (trainee_id) REFERENCES trainees (id),
    CONSTRAINT fk_ratings_history_match_result FOREIGN KEY (match_result_id) REFERENCES match_results (id)
);

CREATE INDEX idx_ratings_history_trainee ON ratings_history(trainee_id);
CREATE INDEX idx_ratings_history_match_result ON ratings_history(match_result_id);

CREATE TABLE IF NOT EXISTS notifications (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    trainee_id BIGINT,
    type VARCHAR(40) NOT NULL,
    message VARCHAR(255) NOT NULL,
    scheduled_at TIMESTAMP,
    sent_at TIMESTAMP,
    is_read BOOLEAN NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_notifications_trainee FOREIGN KEY (trainee_id) REFERENCES trainees (id)
);

CREATE INDEX idx_notifications_trainee ON notifications(trainee_id);
CREATE INDEX idx_notifications_type ON notifications(type);
CREATE INDEX idx_notifications_is_read ON notifications(is_read);

