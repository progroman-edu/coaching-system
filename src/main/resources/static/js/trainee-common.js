// Shared trainee UI helpers used across browse, register, and leaderboard pages.
import { escapeHtml } from "./ui.js";

const NOTE_STORAGE_KEY = "traineeCoachNotes";
const EVALUATION_STORAGE_KEY = "traineeEvaluations";
export const DELETE_PLAYER_KEY = "allowPlayerDeletion";
export const DEFAULT_PROFILE_PHOTO = "/icons/default_pfp.jpg";
export const EVALUATION_CATEGORIES = [
    { key: "opening", label: "Opening", weight: 25 },
    { key: "middleGame", label: "Middle Game", weight: 25 },
    { key: "endGame", label: "End Game", weight: 25 },
    { key: "puzzle", label: "Puzzle", weight: 25 }
];

export function loadCoachNotes() {
    try {
        return JSON.parse(localStorage.getItem(NOTE_STORAGE_KEY) || "{}");
    } catch {
        return {};
    }
}

export function saveCoachNotes(notes) {
    localStorage.setItem(NOTE_STORAGE_KEY, JSON.stringify(notes));
}

export function loadEvaluations() {
    try {
        return JSON.parse(localStorage.getItem(EVALUATION_STORAGE_KEY) || "{}");
    } catch {
        return {};
    }
}

export function saveEvaluations(evaluations) {
    localStorage.setItem(EVALUATION_STORAGE_KEY, JSON.stringify(evaluations));
}

export function getTraineeEvaluation(traineeId) {
    const evaluations = loadEvaluations();
    return evaluations[String(traineeId)] || null;
}

export function saveTraineeEvaluation(traineeId, evaluation) {
    const evaluations = loadEvaluations();
    evaluations[String(traineeId)] = evaluation;
    saveEvaluations(evaluations);
}

export function getDefaultEvaluationScores() {
    return {
        opening: 5,
        middleGame: 5,
        endGame: 5,
        puzzle: 5
    };
}

export function calculateEvaluationSummary(scores = {}) {
    const normalized = {};
    let weightedTotal = 0;
    for (const category of EVALUATION_CATEGORIES) {
        const rawValue = Number(scores?.[category.key]);
        const safeValue = Number.isFinite(rawValue) ? Math.min(10, Math.max(1, rawValue)) : 5;
        normalized[category.key] = safeValue;
        weightedTotal += (safeValue / 10) * category.weight;
    }
    const average = weightedTotal / 10;
    return {
        scores: normalized,
        percent: Math.round(weightedTotal * 10) / 10,
        average: Math.round(average * 10) / 10
    };
}

export function getEvaluationBadgeText(traineeId) {
    const evaluation = getTraineeEvaluation(traineeId);
    if (!evaluation?.summary?.percent) {
        return "Not evaluated";
    }
    return `Eval ${evaluation.summary.percent}%`;
}

export function isDeletePlayerEnabled() {
    return localStorage.getItem(DELETE_PLAYER_KEY) === "1";
}

export function getTraineeDetailHref(traineeId) {
    return `/trainees/detail.html?id=${encodeURIComponent(traineeId)}`;
}

export function getTraineeEvaluationHref(traineeId) {
    return `/trainees/evaluation.html?id=${encodeURIComponent(traineeId)}`;
}

export function modeLabel(mode) {
    const normalizedMode = String(mode || "").toLowerCase();
    if (normalizedMode === "blitz") return "Blitz";
    if (normalizedMode === "bullet") return "Bullet";
    return "Rapid";
}

export function getModeCurrentRating(trainee, mode = "rapid") {
    const normalizedMode = String(mode || "").toLowerCase();
    if (normalizedMode === "blitz") return trainee?.blitzCurrentRating ?? 1200;
    if (normalizedMode === "bullet") return trainee?.bulletCurrentRating ?? 1200;
    return trainee?.rapidCurrentRating ?? 1200;
}

export function getModeHighestRating(trainee, mode = "rapid") {
    const normalizedMode = String(mode || "").toLowerCase();
    if (normalizedMode === "blitz") return trainee?.blitzHighestRating ?? getModeCurrentRating(trainee, "blitz");
    if (normalizedMode === "bullet") return trainee?.bulletHighestRating ?? getModeCurrentRating(trainee, "bullet");
    return trainee?.rapidHighestRating ?? getModeCurrentRating(trainee, "rapid");
}

export function getRankBadgeClass(rank) {
    if (rank === 1) return "rank-badge rank-1";
    if (rank === 2) return "rank-badge rank-2";
    if (rank === 3) return "rank-badge rank-3";
    return "rank-badge";
}

export function formatDelta(delta) {
    const value = Number(delta ?? 0);
    if (value > 0) return `<span class="delta-up">+${escapeHtml(value)}</span>`;
    if (value < 0) return `<span class="delta-down">${escapeHtml(value)}</span>`;
    return `<span class="delta-flat">0</span>`;
}

export function formatAttendance(value) {
    if (value == null) return "0.0%";
    return `${Number(value).toFixed(1)}%`;
}

export function formatLastActivity(value) {
    if (!value) return "N/A";
    const date = new Date(value);
    if (Number.isNaN(date.getTime())) return "N/A";
    return date.toLocaleDateString();
}

export function resolvePhotoSrc(photoPath) {
    const value = String(photoPath || "").trim();
    return value || DEFAULT_PROFILE_PHOTO;
}

export function renderAvatarImage(trainee, className = "trainee-avatar", altText) {
    const label = altText || `${String(trainee?.name || "Trainee").trim() || "Trainee"} photo`;
    return `<img class="${escapeHtml(className)}" src="${escapeHtml(resolvePhotoSrc(trainee?.photoPath))}" alt="${escapeHtml(label)}" loading="lazy" onerror="this.onerror=null;this.src='${DEFAULT_PROFILE_PHOTO}';">`;
}
