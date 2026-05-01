// This file powers trainee evaluation scoring.
import { api } from "./api.js";
import { showMessage } from "./ui.js";
import {
    calculateEvaluationSummary,
    EVALUATION_CATEGORIES,
    getDefaultEvaluationScores,
    getTraineeDetailHref,
    getTraineeEvaluation,
    renderAvatarImage,
    saveTraineeEvaluation
} from "./trainee-common.js";

const msg = document.getElementById("msg");
const traineeNameEl = document.getElementById("traineeName");
const traineeMetaEl = document.getElementById("traineeMeta");
const traineeAvatarEl = document.getElementById("traineeAvatar");
const overallPercentEl = document.getElementById("overallPercent");
const overallAverageEl = document.getElementById("overallAverage");
const overallBandEl = document.getElementById("overallBand");
const lastSavedEl = document.getElementById("lastSaved");
const evaluationForm = document.getElementById("evaluationForm");
const backBtn = document.getElementById("backBtn");
const detailBtn = document.getElementById("detailBtn");
const resetBtn = document.getElementById("resetBtn");

let trainee = null;

function getTraineeIdFromUrl() {
    const params = new URLSearchParams(window.location.search);
    const traineeId = Number(params.get("id"));
    return Number.isFinite(traineeId) && traineeId > 0 ? traineeId : null;
}

function getCurrentScores() {
    const fallback = getDefaultEvaluationScores();
    const scores = {};
    for (const category of EVALUATION_CATEGORIES) {
        const input = evaluationForm?.elements?.[category.key];
        const rawValue = Number(input?.value);
        scores[category.key] = Number.isFinite(rawValue) ? rawValue : fallback[category.key];
    }
    return scores;
}

function getPerformanceBand(percent) {
    if (percent >= 85) return "Tournament Ready";
    if (percent >= 70) return "Strong";
    if (percent >= 55) return "Developing";
    return "Needs Work";
}

function updateSummary() {
    const summary = calculateEvaluationSummary(getCurrentScores());
    if (overallPercentEl) overallPercentEl.textContent = `${summary.percent}%`;
    if (overallAverageEl) overallAverageEl.textContent = `${summary.average}/10`;
    if (overallBandEl) overallBandEl.textContent = getPerformanceBand(summary.percent);
    if (evaluationForm) {
        for (const category of EVALUATION_CATEGORIES) {
            const input = evaluationForm.elements[category.key];
            const valueEl = document.getElementById(`${category.key}Value`);
            if (valueEl && input) {
                valueEl.textContent = `${input.value}/10`;
            }
        }
    }
    return summary;
}

function fillFormScores(scores) {
    if (!evaluationForm) {
        return;
    }
    const source = { ...getDefaultEvaluationScores(), ...(scores || {}) };
    for (const category of EVALUATION_CATEGORIES) {
        const input = evaluationForm.elements[category.key];
        if (input) {
            input.value = String(source[category.key]);
        }
    }
    updateSummary();
}

function renderSavedMeta(evaluation) {
    if (!lastSavedEl) {
        return;
    }
    if (!evaluation?.updatedAt) {
        lastSavedEl.textContent = "No saved evaluation yet.";
        return;
    }
    const date = new Date(evaluation.updatedAt);
    lastSavedEl.textContent = Number.isNaN(date.getTime())
        ? "Saved evaluation available."
        : `Last saved ${date.toLocaleString()}.`;
}

async function loadTrainee() {
    const traineeId = getTraineeIdFromUrl();
    if (!traineeId) {
        showMessage(msg, "Invalid trainee evaluation request.", false);
        return;
    }

    try {
        trainee = await api.getTrainee(traineeId);
        if (traineeNameEl) traineeNameEl.textContent = trainee.name || "Trainee";
        if (traineeMetaEl) {
            traineeMetaEl.textContent = `${trainee.department || "Department not set"} • Rapid ${trainee.rapidCurrentRating ?? 1200} • Blitz ${trainee.blitzCurrentRating ?? 1200} • Bullet ${trainee.bulletCurrentRating ?? 1200}`;
        }
        if (traineeAvatarEl) {
            traineeAvatarEl.innerHTML = renderAvatarImage(trainee, "evaluation-hero-photo");
        }

        const saved = getTraineeEvaluation(traineeId);
        fillFormScores(saved?.scores);
        renderSavedMeta(saved);
    } catch (err) {
        showMessage(msg, `Unable to load trainee evaluation: ${err.message}`, false);
    }
}

evaluationForm?.addEventListener("input", () => {
    updateSummary();
});

evaluationForm?.addEventListener("submit", async (e) => {
    e.preventDefault();
    const traineeId = getTraineeIdFromUrl();
    if (!traineeId) {
        showMessage(msg, "Invalid trainee evaluation request.", false);
        return;
    }
    const summary = updateSummary();
    const evaluation = {
        scores: summary.scores,
        summary: {
            percent: summary.percent,
            average: summary.average
        },
        updatedAt: new Date().toISOString()
    };
    saveTraineeEvaluation(traineeId, evaluation);
    renderSavedMeta(evaluation);
    showMessage(msg, `Evaluation saved for ${trainee?.name || "trainee"}.`);
});

resetBtn?.addEventListener("click", () => {
    fillFormScores(getDefaultEvaluationScores());
});

backBtn?.addEventListener("click", () => {
    window.location.href = "/trainees.html";
});

detailBtn?.addEventListener("click", () => {
    const traineeId = getTraineeIdFromUrl();
    if (traineeId) {
        window.location.href = getTraineeDetailHref(traineeId);
    }
});

loadTrainee();
