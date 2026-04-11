# Entity and Relationship (ER) and Normalization Documentation

## 1. Entities

### Strong Entities
- `coaches`
- `trainees`
- `matches`
- `notifications`

### Weak / Associative Entities
- `match_participants` (depends on `matches` and `trainees`)
- `attendance` (depends on `trainees`)
- `match_results` (depends on `matches` and `trainees`)
- `ratings_history` (depends on `trainees` and optionally `match_results`)
- `blitz_rating` (depends on `trainees`)
- `bullet_rating` (depends on `trainees`)
- `rapid_rating` (depends on `trainees`)

---

## 2. Attributes Classification

### Simple Attributes
- `age`, `department`, `type`, `is_read`, `round_number`, `status`, `message`, `scheduled_at`, `sent_at`

### Composite Attributes (conceptual)
- `full_name` (can be split into first name and last name)
- `address` (can be split into street, city, province, etc.)

### Multivalued Attributes (modeled as separate tables)
- A trainee can have many attendance records 
- A trainee can have many notifications 
- A trainee can have many rating history entries 
- A match can have many participants 
- A Coach can have multiple trainee 

### Derived Attributes
- `ranking` (derived from rating/performance logic)
- `rating_change` (derivable as `new_rating - old_rating`, though stored)

---

## 3. Primary Keys

All main relations use surrogate primary key `id`:

- `coaches(id)`
- `trainees(id)`
- `matches(id)`
- `attendance(id)`
- `match_participants(id)`
- `match_results(id)`
- `ratings_history(id)`
- `notifications(id)`
- `blitz_rating(id)`
- `bullet_rating(id)`
- `rapid_rating(id)`

Additional unique constraints (candidate/alternate keys):

- `attendance(trainee_id, attendance_date)` unique
- `match_participants(match_id, trainee_id)` unique
- `match_results(match_id, white_trainee_id, black_trainee_id)` unique
- `blitz_rating(trainee_id)` unique
- `bullet_rating(trainee_id)` unique
- `rapid_rating(trainee_id)` unique

---

## 4. Relationships and Cardinalities

1. `coaches (1) ---- (Many) trainees`
2. `trainees (1) ---- (1) attendance`
3. `trainees (1) ---- (Many) notifications` (nullable FK allows global notifications)
4. `trainees (1) ---- (Many) ratings_history`
5. `matches (1) ---- (Many) match_participants`
6. `trainees (1) ---- (Many) match_participants`
7. `matches (1) ---- (Many) match_results`
8. `trainees (1) ---- (Many) match_results` as white player
9. `trainees (1) ---- (Many) match_results` as black player
10. `match_results (1) ---- (Many) ratings_history` (optional via nullable `match_result_id`)
11. `trainees (1) ---- (1) blitz_rating`
12. `trainees (1) ---- (1) bullet_rating`
13. `trainees (1) ---- (1) rapid_rating`

---

## 5. Normalized Tables (Up to 3NF / near BCNF)

## Initial Unnormalized Form (UNF)

Single wide structure with repeating groups:

`COACHING_UNF( coach_data, trainee_data, {attendance_group}, {match_group}, {participants_group}, {result_group}, {ratings_history_group}, {notifications_group} )`

Issues in UNF:
- Repeating groups
- Data redundancy
- Insert, update, and delete anomalies

---

## First Normal Form (1NF) - Atomic Values

Action:
- Remove repeating groups into separate row-based relations.
- Ensure one value per cell.

Resulting 1NF relations (conceptual):
- `coaches_1nf`
- `trainees_1nf`
- `attendance_1nf`
- `matches_1nf`
- `match_participants_1nf`
- `match_results_1nf`
- `ratings_history_1nf`
- `notifications_1nf`

---

## Second Normal Form (2NF) - Remove Partial Dependency

Action:
- For composite-key contexts, ensure non-key attributes depend on the full key.

Examples:
- `attendance(trainee_id, attendance_date, is_present, remarks)` where attributes depend on full `(trainee_id, attendance_date)`
- `match_participants(match_id, trainee_id, piece_color, board_number, start_rating, points_earned, is_bye)` where participation attributes depend on full `(match_id, trainee_id)`

Result:
- Trainee descriptive attributes remain in `trainees`
- Match descriptive attributes remain in `matches`
- Participation and attendance details stay in their own relations

---

## 5 Third Normal Form (3NF) - Remove Transitive Dependency

Action:
- Remove attributes that depend on non-key attributes.

Examples:
- Coach details stored in `coaches`, referenced by `trainees.coach_id`
- Notification details stored in `notifications`, referenced by `notifications.trainee_id`
- Match schedule info in `matches`; outcome info in `match_results`

Result:
- Current schema is in 3NF
- Near BCNF, with practical stored derived fields like `ranking` and `rating_change`

---

## 6. Final Set of Normalized Relations with PK and FK

| Relation | Primary Key | Foreign Keys |
|---|---|---|
| `coaches` | `id` | — |
| `trainees` | `id` | `coach_id -> coaches.id` |
| `blitz_rating` | `id` | `trainee_id -> trainees.id` (unique) |
| `bullet_rating` | `id` | `trainee_id -> trainees.id` (unique) |
| `rapid_rating` | `id` | `trainee_id -> trainees.id` (unique) |
| `attendance` | `id` | `trainee_id -> trainees.id` |
| `matches` | `id` | — |
| `match_participants` | `id` | `match_id -> matches.id`, `trainee_id -> trainees.id` |
| `match_results` | `id` | `match_id -> matches.id`, `white_trainee_id -> trainees.id`, `black_trainee_id -> trainees.id` |
| `ratings_history` | `id` | `trainee_id -> trainees.id`, `match_result_id -> match_results.id` |
| `notifications` | `id` | `trainee_id -> trainees.id` (nullable) |

